package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/pion/rtcp"
	"github.com/pion/sdp/v3"
	"github.com/pion/webrtc/v4"
)

// ---------- utilidades de logging ----------
func logStream(streamID string, format string, a ...interface{}) {
	msg := fmt.Sprintf(format, a...)
	fmt.Fprintf(os.Stderr, "[%s] %s\n", streamID, strings.TrimSuffix(msg, "\n"))
}

// ---------- estructuras ----------
type OfferRequest struct {
	StreamID      string   `json:"streamId"`
	SDP           string   `json:"sdp"`
	VideoSettings []string `json:"videoSettings"`
}

type StreamSession struct {
	StreamID  string
	PC        *webrtc.PeerConnection
	VideoPort int
	AudioPort int
	Cancel    chan struct{}
}

var (
	streams   = make(map[string]*StreamSession)
	streamsMu sync.RWMutex

	pendingCandidates   = make(map[string][]webrtc.ICECandidateInit)
	pendingCandidatesMu sync.Mutex
)

// ---------- gestión de streams ----------
func addStream(s *StreamSession) {
	streamsMu.Lock()
	defer streamsMu.Unlock()
	streams[s.StreamID] = s
}

func getStream(streamID string) (*StreamSession, bool) {
	streamsMu.RLock()
	defer streamsMu.RUnlock()
	s, ok := streams[streamID]
	return s, ok
}

func handleRemoteCandidate(streamID string, ice webrtc.ICECandidateInit) {
	if sess, ok := getStream(streamID); ok && sess.PC != nil {
		_ = sess.PC.AddICECandidate(ice)
		return
	}

	pendingCandidatesMu.Lock()
	defer pendingCandidatesMu.Unlock()
	pendingCandidates[streamID] = append(pendingCandidates[streamID], ice)
}

// ---------- main ----------
func main() {
	baseAPI := webrtc.NewAPI()
	reader := bufio.NewReader(os.Stdin)

	for {
		line, err := reader.ReadBytes('\n')
		if err != nil {
			return
		}
		lineStr := strings.TrimSpace(string(line))
		if lineStr == "" {
			continue
		}

		var generic map[string]json.RawMessage
		if err := json.Unmarshal(line, &generic); err != nil {
			continue
		}

		var msgType string
		if err := json.Unmarshal(generic["type"], &msgType); err != nil {
			continue
		}

		switch msgType {
		case "candidate":
			var c struct {
				StreamID  string                  `json:"streamId"`
				Candidate webrtc.ICECandidateInit `json:"candidate"`
			}
			if err := json.Unmarshal(line, &c); err == nil {
				handleRemoteCandidate(c.StreamID, c.Candidate)
			}
		case "offer":
			var req OfferRequest
			if err := json.Unmarshal(line, &req); err == nil {
				go handleOffer(baseAPI, req)
			}
		case "stopStreamByID": // admite dos nombres si el front usa otro
			// Intentamos primero parsear el formato esperado: {"type":"stopStreamByID","streamId":"..."}
			var payload struct {
				StreamID string `json:"streamId"`
			}
			if err := json.Unmarshal(line, &payload); err == nil && payload.StreamID != "" {
				logStream(payload.StreamID, "Mensaje stop recibido -> deteniendo sesión")
				go stopStreamByID(payload.StreamID)
			}
		}
	}
}

// ---------- offer handler ----------
func handleOffer(_api *webrtc.API, req OfferRequest) {
	mediaEngine := &webrtc.MediaEngine{}

	if err := mediaEngine.RegisterCodec(webrtc.RTPCodecParameters{
		RTPCodecCapability: webrtc.RTPCodecCapability{
			MimeType:    webrtc.MimeTypeH264,
			ClockRate:   90000,
			SDPFmtpLine: "level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f",
		},
		PayloadType: 102,
	}, webrtc.RTPCodecTypeVideo); err != nil {
		logStream(req.StreamID, "Error registering H264: %v", err)
		return
	}

	if err := mediaEngine.RegisterCodec(webrtc.RTPCodecParameters{
		RTPCodecCapability: webrtc.RTPCodecCapability{
			MimeType:  webrtc.MimeTypeOpus,
			ClockRate: 48000,
			Channels:  2,
		},
		PayloadType: 111,
	}, webrtc.RTPCodecTypeAudio); err != nil {
		logStream(req.StreamID, "Error registering Opus: %v", err)
		return
	}

	settingEngine := webrtc.SettingEngine{}
	settingEngine.DetachDataChannels()
	api := webrtc.NewAPI(webrtc.WithMediaEngine(mediaEngine), webrtc.WithSettingEngine(settingEngine))

	pc, err := api.NewPeerConnection(webrtc.Configuration{
		ICEServers: []webrtc.ICEServer{{URLs: []string{"stun:stun.l.google.com:19302"}}},
	})
	if err != nil {
		logStream(req.StreamID, "Error creating peer connection: %v", err)
		return
	}

	logStream(req.StreamID, "Recibido SDP Offer")

	pc.OnICECandidate(func(c *webrtc.ICECandidate) {
		if c == nil {
			return
		}
		j := map[string]string{
			"type":      "candidate",
			"streamId":  req.StreamID,
			"candidate": c.ToJSON().Candidate,
		}
		b, _ := json.Marshal(j)
		fmt.Println(string(b))
	})

	cancel := make(chan struct{})
	videoPort, audioPort, err := getTwoFreeUDPPorts()
	if err != nil {
		logStream(req.StreamID, "Error getting free UDP ports: %v", err)
		return
	}

	session := &StreamSession{StreamID: req.StreamID, PC: pc, VideoPort: videoPort, AudioPort: audioPort, Cancel: cancel}
	addStream(session)

	var keyframeOnce sync.Once
	pliCancel := make(chan struct{})

	// cleanup para esta sesión (seguro, idempotente)
	var cleanupOnce sync.Once
	cleanup := func() {
		cleanupOnce.Do(func() {
			// Remove from streams map
			streamsMu.Lock()
			delete(streams, req.StreamID)
			streamsMu.Unlock()

			// Close cancel channel
			select {
			case <-cancel:
				// already closed
			default:
				func() {
					defer func() { _ = recover() }()
					close(cancel)
				}()
			}

			// Close pliCancel channel
			select {
			case <-pliCancel:
				// already closed
			default:
				func() {
					defer func() { _ = recover() }()
					close(pliCancel)
				}()
			}

			// Delete pending candidates for this stream (avoid acumulación)
			pendingCandidatesMu.Lock()
			delete(pendingCandidates, req.StreamID)
			pendingCandidatesMu.Unlock()

			// Close PeerConnection
			if pc != nil {
				_ = pc.Close()
			}

			logStream(req.StreamID, "Session cleaned up")
		})
	}

	pc.OnConnectionStateChange(func(state webrtc.PeerConnectionState) {
		logStream(req.StreamID, "PeerConnection state: %s", state.String())
		if state == webrtc.PeerConnectionStateFailed ||
			state == webrtc.PeerConnectionStateDisconnected ||
			state == webrtc.PeerConnectionStateClosed {
			cleanup()
		}
	})

	sdpForFFmpeg, chosenVideoPT, chosenAudioPT := prelimSDPText(req.SDP, req.VideoSettings, videoPort, audioPort)

	pc.OnTrack(func(track *webrtc.TrackRemote, _ *webrtc.RTPReceiver) {
		codec := strings.ToLower(track.Codec().MimeType)
		if strings.Contains(codec, "h264") {
			logStream(req.StreamID, "Video track recibido (%s), forward directo a puerto %d (PT->%d)", codec, videoPort, chosenVideoPT)
			go sendPLIPeriodically(pc, track, pliCancel)
			go streamVideoForwardAndDetectKeyframe(req.StreamID, track, videoPort, cancel, &keyframeOnce, chosenVideoPT, pliCancel)
			go func() {
				for i := 0; i < 3; i++ {
					_ = pc.WriteRTCP([]rtcp.Packet{&rtcp.PictureLossIndication{MediaSSRC: uint32(track.SSRC())}})
					time.Sleep(100 * time.Millisecond)
				}
			}()
		} else if strings.Contains(codec, "opus") {
			logStream(req.StreamID, "Audio track recibido, forward directo a puerto %d (PT->%d)", audioPort, chosenAudioPT)
			go streamAudio(req.StreamID, track, audioPort, cancel, chosenAudioPT)
		} else {
			logStream(req.StreamID, "Track con codec desconocido (%s) - forward sin rewriting", codec)
			go func() {
				addr, _ := net.ResolveUDPAddr("udp", fmt.Sprintf("127.0.0.1:%d", videoPort))
				conn, err := net.DialUDP("udp", nil, addr)
				if err != nil {
					return
				}
				defer conn.Close()
				buf := make([]byte, 1500)
				for {
					select {
					case <-cancel:
						return
					default:
					}
					pkt, _, err := track.ReadRTP()
					if err != nil {
						continue
					}
					if len(pkt.Payload) == 0 {
						continue
					}
					raw, err := pkt.MarshalTo(buf)
					if err != nil {
						continue
					}
					_, _ = conn.Write(buf[:raw])
				}
			}()
		}
	})

	if err := pc.SetRemoteDescription(webrtc.SessionDescription{Type: webrtc.SDPTypeOffer, SDP: req.SDP}); err != nil {
		logStream(req.StreamID, "Error setting remote description: %v", err)
		cleanup()
		return
	}

	// Aplicar candidatos pendientes (si los hay) y luego borrarlos
	pendingCandidatesMu.Lock()
	if list, ok := pendingCandidates[req.StreamID]; ok {
		for _, ice := range list {
			_ = pc.AddICECandidate(ice)
		}
		delete(pendingCandidates, req.StreamID)
	}
	pendingCandidatesMu.Unlock()

	answer, err := pc.CreateAnswer(nil)
	if err != nil {
		logStream(req.StreamID, "Error creating answer: %v", err)
		cleanup()
		return
	}
	if err := pc.SetLocalDescription(answer); err != nil {
		logStream(req.StreamID, "Error setting local description: %v", err)
		cleanup()
		return
	}

	logStream(req.StreamID, "Generando y enviando SDP para FFmpeg (video=%d audio=%d)", videoPort, audioPort)
	sendSDPResponse(req.StreamID, sdpForFFmpeg, req.VideoSettings)

	ffmpegBindTimeout := 20 * time.Second
	okV := waitForUDPListenerWithTimeout(videoPort, ffmpegBindTimeout)
	okA := waitForUDPListenerWithTimeout(audioPort, ffmpegBindTimeout)
	if okV && okA {
		logStream(req.StreamID, "Detected bind on both FFmpeg ports (video=%d audio=%d)", videoPort, audioPort)
	} else {
		logStream(req.StreamID, "WARNING: timeout waiting for external FFmpeg bind (video ok=%v audio ok=%v)", okV, okA)
	}

	<-webrtc.GatheringCompletePromise(pc)
	resp := map[string]string{"type": "webrtc-answer", "streamId": req.StreamID, "sdp": pc.LocalDescription().SDP}
	b, _ := json.Marshal(resp)
	logStream(req.StreamID, "Enviando webrtc-answer al frontend")
	fmt.Println(string(b))

	// Espera a que la sesión sea cancelada; cuando ocurra, cleanup.
	<-cancel
	cleanup()
}

// ---------- forwarders ----------
func streamVideoForwardAndDetectKeyframe(streamID string, track *webrtc.TrackRemote, port int, cancel <-chan struct{}, once *sync.Once, targetPT int, pliCancel chan struct{}) {
	addr, _ := net.ResolveUDPAddr("udp", fmt.Sprintf("127.0.0.1:%d", port))
	conn, err := net.DialUDP("udp", nil, addr)
	if err != nil {
		logStream(streamID, "Error conectando a video UDP %d: %v", port, err)
		return
	}
	defer conn.Close()

	buf := make([]byte, 4096) // buffer ampliado
	var forwarded uint64
	var skippedEmpty uint64

	for {
		select {
		case <-cancel:
			return
		default:
		}
		pkt, _, err := track.ReadRTP()
		if err != nil {
			continue
		}
		if len(pkt.Payload) == 0 {
			atomic.AddUint64(&skippedEmpty, 1)
			if atomic.LoadUint64(&skippedEmpty)%200 == 0 {
				logStream(streamID, "Video: paquetes vacíos detectados %d (puerto %d)", atomic.LoadUint64(&skippedEmpty), port)
			}
			continue
		}
		payload := pkt.Payload
		isKey := false
		if len(payload) > 0 {
			nalType := payload[0] & 0x1F
			switch nalType {
			case 7, 8, 5:
				isKey = true
			case 28:
				if len(payload) > 1 && (payload[1]&0x80) != 0 {
					if t := payload[1] & 0x1F; t == 5 || t == 7 || t == 8 {
						isKey = true
					}
				}
			case 24:
				offset := 1
				for offset < len(payload)-2 {
					nalSize := int(payload[offset])<<8 | int(payload[offset+1])
					offset += 2
					if offset+nalSize > len(payload) {
						break
					}
					if nalSize > 0 {
						agg := payload[offset] & 0x1F
						if agg == 5 || agg == 7 || agg == 8 {
							isKey = true
							break
						}
					}
					offset += nalSize
				}
			}
		}
		if isKey {
			once.Do(func() { logStream(streamID, "Primer keyframe detectado en video (puerto %d)", port) })
		}
		if targetPT >= 0 && targetPT <= 255 {
			pkt.Header.PayloadType = uint8(targetPT)
		}
		raw, err := pkt.MarshalTo(buf)
		if err == nil {
			if _, err := conn.Write(buf[:raw]); err != nil {
				logStream(streamID, "Error escribiendo UDP video a %d: %v", port, err)
			}
			atomic.AddUint64(&forwarded, 1)
		}
	}
}

func streamAudio(streamID string, track *webrtc.TrackRemote, port int, cancel <-chan struct{}, targetPT int) {
	addr, _ := net.ResolveUDPAddr("udp", fmt.Sprintf("127.0.0.1:%d", port))
	conn, err := net.DialUDP("udp", nil, addr)
	if err != nil {
		logStream(streamID, "Error conectando a audio UDP %d: %v", port, err)
		return
	}
	defer conn.Close()

	buf := make([]byte, 2048) // buffer ampliado ligeramente
	var forwarded uint64
	var skippedEmpty uint64

	for {
		select {
		case <-cancel:
			return
		default:
		}
		pkt, _, err := track.ReadRTP()
		if err != nil {
			continue
		}
		if len(pkt.Payload) == 0 {
			atomic.AddUint64(&skippedEmpty, 1)
			if atomic.LoadUint64(&skippedEmpty)%300 == 0 {
				logStream(streamID, "Audio: paquetes vacíos detectados %d (puerto %d)", atomic.LoadUint64(&skippedEmpty), port)
			}
			continue
		}
		if targetPT >= 0 && targetPT <= 255 {
			pkt.Header.PayloadType = uint8(targetPT)
		}
		raw, err := pkt.MarshalTo(buf)
		if err == nil {
			if _, err := conn.Write(buf[:raw]); err != nil {
				logStream(streamID, "Error escribiendo UDP audio a %d: %v", port, err)
			}
			atomic.AddUint64(&forwarded, 1)
		}
	}
}

// ---------- helpers ----------
func waitForUDPListenerWithTimeout(port int, timeout time.Duration) bool {
	addr := fmt.Sprintf("127.0.0.1:%d", port)
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		c, err := net.ListenPacket("udp", addr)
		if err != nil {
			if strings.Contains(err.Error(), "address already in use") || strings.Contains(err.Error(), "bind: permission denied") {
				return true
			}
		} else {
			_ = c.Close()
		}
		time.Sleep(150 * time.Millisecond)
	}
	return false
}

func getTwoFreeUDPPorts() (int, int, error) {
	conn1, err := net.ListenPacket("udp", "127.0.0.1:0")
	if err != nil {
		return 0, 0, err
	}
	defer conn1.Close()
	conn2, err := net.ListenPacket("udp", "127.0.0.1:0")
	if err != nil {
		return 0, 0, err
	}
	defer conn2.Close()
	return conn1.LocalAddr().(*net.UDPAddr).Port, conn2.LocalAddr().(*net.UDPAddr).Port, nil
}

// ---------- SDP ----------
func prelimSDPText(offerSDP string, videoSettings []string, videoPort, audioPort int) (string, int, int) {
	width, height, fps := 1280, 720, 30
	if len(videoSettings) >= 3 {
		if w, err := strconv.Atoi(videoSettings[0]); err == nil && w > 0 {
			width = w
		}
		if h, err := strconv.Atoi(videoSettings[1]); err == nil && h > 0 {
			height = h
		}
		if f, err := strconv.Atoi(videoSettings[2]); err == nil && f > 0 {
			fps = f
		}
	}

	parsed := sdp.SessionDescription{}
	_ = parsed.Unmarshal([]byte(offerSDP))

	audioPT, audioCodec, audioFmtp := 111, "opus/48000/2", "minptime=10;useinbandfec=1"
	videoPT, videoCodec, videoFmtp := 102, "H264/90000", "packetization-mode=1;profile-level-id=42e01f;level-asymmetry-allowed=1"
	pixelFormat := "yuv420p"

	for _, md := range parsed.MediaDescriptions {
		if len(md.MediaName.Formats) == 0 {
			continue
		}
		kind := strings.ToLower(md.MediaName.Media)
		ptStr := md.MediaName.Formats[0]

		if kind == "audio" {
			if v, err := strconv.Atoi(ptStr); err == nil {
				audioPT = v
			}
			for _, a := range md.Attributes {
				if a.Key == "rtpmap" {
					parts := strings.Fields(a.Value)
					if len(parts) >= 2 && parts[0] == ptStr {
						audioCodec = parts[1]
					}
				} else if a.Key == "fmtp" {
					parts := strings.SplitN(a.Value, " ", 2)
					if len(parts) == 2 && parts[0] == ptStr {
						audioFmtp = parts[1]
					}
				}
			}
		} else if kind == "video" {
			if v, err := strconv.Atoi(ptStr); err == nil {
				videoPT = v
			}
			for _, a := range md.Attributes {
				if a.Key == "rtpmap" {
					parts := strings.Fields(a.Value)
					if len(parts) >= 2 && parts[0] == ptStr {
						videoCodec = parts[1]
					}
				} else if a.Key == "fmtp" {
					parts := strings.SplitN(a.Value, " ", 2)
					if len(parts) == 2 && parts[0] == ptStr {
						videoFmtp = parts[1]
						for _, attr := range strings.Split(videoFmtp, ";") {
							attr = strings.TrimSpace(attr)
							if strings.HasPrefix(attr, "pixel_format=") {
								pixelFormat = strings.TrimPrefix(attr, "pixel_format=")
							}
						}
					}
				} else if a.Key == "framerate" {
					if f, err := strconv.Atoi(a.Value); err == nil && f > 0 {
						fps = f
					}
				}
			}
		}
	}

	if !strings.Contains(strings.ToUpper(videoCodec), "H264") {
		videoPT = 102
		videoCodec = "H264/90000"
		videoFmtp = "packetization-mode=1;profile-level-id=42e01f;level-asymmetry-allowed=1;pixel_format=" + pixelFormat
	}

	if !strings.Contains(strings.ToLower(videoFmtp), "pixel_format") {
		videoFmtp += ";pixel_format=" + pixelFormat
	}

	// Construcción clara del SDP RTP para FFmpeg
	// Usamos "recvonly" porque FFmpeg solo recibe
	// Nota: a=framesize usa '<pt> <width>-<height>'
	sdpText := fmt.Sprintf(`v=0
o=- 0 0 IN IP4 127.0.0.1
s=WebRTC Stream
c=IN IP4 127.0.0.1
t=0 0
m=audio %d RTP/AVP %d
a=rtpmap:%d %s
a=fmtp:%d %s
a=recvonly
m=video %d RTP/AVP %d
a=rtpmap:%d %s
a=fmtp:%d %s
a=framerate:%d
a=framesize:%d %d-%d
a=recvonly
`, audioPort, audioPT, audioPT, audioCodec, audioPT, audioFmtp,
		videoPort, videoPT, videoPT, videoCodec, videoPT, videoFmtp,
		fps, videoPT, width, height)

	return sdpText, videoPT, audioPT
}

func sendSDPResponse(streamID, sdpText string, videoSettings []string) {
	resp := struct {
		Type          string   `json:"type"`
		StreamID      string   `json:"streamId"`
		SDP           string   `json:"sdp"`
		VideoSettings []string `json:"videoSettings"`
	}{
		Type:          "rtp-sdp",
		StreamID:      streamID,
		SDP:           sdpText,
		VideoSettings: videoSettings,
	}
	b, _ := json.Marshal(resp)
	fmt.Println(string(b))
}

func sendPLIPeriodically(pc *webrtc.PeerConnection, track *webrtc.TrackRemote, cancel <-chan struct{}) {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-cancel:
			return
		case <-ticker.C:
			_ = pc.WriteRTCP([]rtcp.Packet{&rtcp.PictureLossIndication{MediaSSRC: uint32(track.SSRC())}})
		}
	}
}

// stopStreamByID detiene y limpia la sesión asociada al streamID.
// Es idempotente.
func stopStreamByID(streamID string) {
	streamsMu.Lock()
	sess, ok := streams[streamID]
	if ok {
		delete(streams, streamID)
	}
	streamsMu.Unlock()

	if !ok {
		return
	}

	// Borrar pending candidates
	pendingCandidatesMu.Lock()
	delete(pendingCandidates, streamID)
	pendingCandidatesMu.Unlock()

	// Cerrar cancel si no cerrado
	select {
	case <-sess.Cancel:
		// ya cerrado
	default:
		func() {
			defer func() { _ = recover() }()
			close(sess.Cancel)
		}()
	}

	// Cerrar PeerConnection
	if sess.PC != nil {
		_ = sess.PC.Close()
	}

	logStream(streamID, "stopStreamByID: sesión parada por petición/errores")
}
