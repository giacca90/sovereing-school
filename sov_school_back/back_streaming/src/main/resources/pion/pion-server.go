package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"time"

	//"io"
	"net"
	"os"

	//"os/exec"
	"strings"
	"sync"

	"github.com/pion/rtcp"
	"github.com/pion/rtp"
	"github.com/pion/rtp/codecs"
	"github.com/pion/webrtc/v4"
)

type OfferRequest struct {
	SessionID string `json:"sessionId"`
	SDP       string `json:"sdp"`
	Width     int    `json:"width"`
	Height    int    `json:"height"`
	FPS       int    `json:"fps"`
}

type AnswerResponse struct {
	SessionID string `json:"sessionId"`
	SDP       string `json:"sdp"`
}

func main() {
	api := webrtc.NewAPI()
	reader := bufio.NewReader(os.Stdin)

	for {
		line, err := reader.ReadBytes('\n')
		if err != nil {
			fmt.Fprintf(os.Stderr, "[ERROR] No se pudo leer la entrada: %v\n", err)
			return
		}
		lineStr := strings.TrimSpace(string(line))
		if lineStr == "" {
			continue
		}

		var req OfferRequest
		if err := json.Unmarshal([]byte(lineStr), &req); err != nil {
			fmt.Fprintf(os.Stderr, "[ERROR] JSON inválido: %v\n", err)
			continue
		}

		go handleOffer(api, req)
	}
}

func handleOffer(api *webrtc.API, req OfferRequest) {
	pc, err := api.NewPeerConnection(webrtc.Configuration{})
	if err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error creando PeerConnection: %v\n", req.SessionID, err)
		return
	}

	var videoTrack, audioTrack *webrtc.TrackRemote
	var tracksMutex sync.Mutex
	muxStarted := false

	ready := make(chan struct{})

	pc.OnTrack(func(track *webrtc.TrackRemote, _ *webrtc.RTPReceiver) {
		<-ready

		mime := strings.ToLower(track.Codec().MimeType)
		tracksMutex.Lock()
		defer tracksMutex.Unlock()

		if strings.Contains(mime, "video") {
			fmt.Fprintf(os.Stderr, "[%s] Video recibido: %s\n", req.SessionID, mime)
			videoTrack = track
		} else if strings.Contains(mime, "audio") {
			fmt.Fprintf(os.Stderr, "[%s] Audio recibido: %s\n", req.SessionID, mime)
			audioTrack = track
		} else {
			fmt.Fprintf(os.Stderr, "[%s] Codec no soportado: %s\n", req.SessionID, mime)
			return
		}

		if !muxStarted && videoTrack != nil && audioTrack != nil {
			muxStarted = true
			startFFmpeg(req.SessionID, videoTrack, audioTrack, req.Width, req.Height, req.FPS, pc)
		}
	})

	offer := webrtc.SessionDescription{Type: webrtc.SDPTypeOffer, SDP: req.SDP}
	if err := pc.SetRemoteDescription(offer); err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error en SetRemoteDescription: %v\n", req.SessionID, err)
		return
	}

	answer, err := pc.CreateAnswer(nil)
	if err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error en CreateAnswer: %v\n", req.SessionID, err)
		return
	}
	if err := pc.SetLocalDescription(answer); err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error en SetLocalDescription: %v\n", req.SessionID, err)
		return
	}
	<-webrtc.GatheringCompletePromise(pc)

	resp := AnswerResponse{SessionID: req.SessionID, SDP: pc.LocalDescription().SDP}
	respBytes, _ := json.Marshal(resp)

	fmt.Println(string(respBytes))
	os.Stdout.Sync()

	close(ready)
}

func startFFmpeg(sessionID string, videoTrack, audioTrack *webrtc.TrackRemote, width, height, fps int, pc *webrtc.PeerConnection) {
	fmt.Fprintf(os.Stderr, "[%s] Arrancando ffmpeg\n", sessionID)

	videoPort, audioPort, err := getTwoFreeUDPPorts()
	if err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error obteniendo puertos: %v\n", sessionID, err)
		return
	}

	fmt.Fprintf(os.Stderr, "[%s] Puertos RTP → video:%d audio:%d\n", sessionID, videoPort, audioPort)

	audioPT := audioTrack.PayloadType()
	videoPT := videoTrack.PayloadType()

	audioCodec := "opus/48000/2"
	videoCodec := "VP8/90000"
	videoMime := strings.ToLower(videoTrack.Codec().MimeType)
	audioMime := strings.ToLower(audioTrack.Codec().MimeType)

	if strings.Contains(audioMime, "pcma") {
		audioCodec = "PCMA/8000"
	} else if strings.Contains(audioMime, "pcmu") {
		audioCodec = "PCMU/8000"
	}
	if strings.Contains(videoMime, "vp9") {
		videoCodec = "VP9/90000"
	} else if strings.Contains(videoMime, "h264") {
		videoCodec = "H264/90000"
	}

	sdp := fmt.Sprintf(`v=0
o=- 0 0 IN IP4 127.0.0.1
s=WebRTC Stream
c=IN IP4 127.0.0.1
t=0 0
a=tool:libavformat
m=audio %d RTP/AVP %d
a=rtpmap:%d %s
a=recvonly
b=AS:12
m=video %d RTP/AVP %d
a=rtpmap:%d %s
a=recvonly
a=framerate:%d
a=fmtp:%d max-fr=%d;max-fs=%d
a=imageattr:%d send * recv [x=%d,y=%d]
a=rtcp-fb:%d nack pli
b=AS:3000
`,
		audioPort, audioPT, audioPT, audioCodec,
		videoPort, videoPT, videoPT, videoCodec,
		fps, videoPT, fps, (width * height / 256),
		videoPT, width, height,
		videoPT)

	fmt.Fprintf(os.Stderr, "[%s] SDP generado:\n%s\n", sessionID, sdp)

	sdpFile := fmt.Sprintf("/tmp/%s.sdp", sessionID)
	if err := os.WriteFile(sdpFile, []byte(sdp), 0644); err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error escribiendo SDP: %v\n", sessionID, err)
		return
	}
	fmt.Fprintf(os.Stderr, "[%s] Archivo SDP: %s\n", sessionID, sdpFile)

	time.Sleep(3 * time.Second)

	// Enviar RTCP PLI para pedir keyframe
	go func() {
		fmt.Fprintf(os.Stderr, "[%s] Enviando RTCP PLI para forzar keyframe...\n", sessionID)

		for {
			if pc.ConnectionState() == webrtc.PeerConnectionStateClosed {
				return
			}

			err := pc.WriteRTCP([]rtcp.Packet{
				&rtcp.PictureLossIndication{
					MediaSSRC: uint32(videoTrack.SSRC()),
				},
			})
			if err != nil {
				fmt.Fprintf(os.Stderr, "[%s] Error enviando RTCP PLI: %v\n", sessionID, err)
			} else {
				fmt.Fprintf(os.Stderr, "[%s] RTCP PLI enviado\n", sessionID)
			}

			time.Sleep(1 * time.Second)
		}
	}()

	videoConn, err := net.DialUDP("udp", nil, &net.UDPAddr{IP: net.ParseIP("127.0.0.1"), Port: videoPort})
	if err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error UDP video: %v\n", sessionID, err)
		return
	}

	audioConn, err := net.DialUDP("udp", nil, &net.UDPAddr{IP: net.ParseIP("127.0.0.1"), Port: audioPort})
	if err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error UDP audio: %v\n", sessionID, err)
		return
	}

	videoBuffer := make(chan []byte, 500) // ajusta capacidad según necesidades
	audioBuffer := make(chan []byte, 500)

	// Consumidor de video: toma UN paquete, reintenta hasta éxito, luego pasa al siguiente
	go func() {
		for pkt := range videoBuffer {
			for {
				if _, err := videoConn.Write(pkt); err != nil {
					fmt.Fprintf(os.Stderr, "[%s] Reintentando paquete de video: %v\n", sessionID, err)
					time.Sleep(100 * time.Millisecond)
					continue
				}
				break
			}
		}
	}()

	// Consumidor de audio: idéntico
	go func() {
		for pkt := range audioBuffer {
			for {
				if _, err := audioConn.Write(pkt); err != nil {
					fmt.Fprintf(os.Stderr, "[%s] Reintentando paquete de audio: %v\n", sessionID, err)
					time.Sleep(100 * time.Millisecond)
					continue
				}
				break
			}
		}
	}()

	// Productor de video: encola **todo** (bloqueando si el buffer está lleno)
	go func() {
		for {
			p, _, err := videoTrack.ReadRTP()
			if err != nil {
				return
			}
			raw, _ := p.Marshal()
			videoBuffer <- raw
		}
	}()

	// Productor de audio: idem
	go func() {
		for {
			p, _, err := audioTrack.ReadRTP()
			if err != nil {
				return
			}
			raw, _ := p.Marshal()
			audioBuffer <- raw
		}
	}()

	/* fmt.Fprintf(os.Stderr, "[%s] Transmitiendo stdout de ffmpeg...\n", sessionID)
	io.Copy(os.Stdout, stdout)
	cmd.Wait() */
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

	addr1 := conn1.LocalAddr().(*net.UDPAddr)
	addr2 := conn2.LocalAddr().(*net.UDPAddr)

	return addr1.Port, addr2.Port, nil
}

func isKeyframe(pkt *rtp.Packet, mime string) bool {
	switch mime {
	case "video/vp8":
		var vp8 codecs.VP8Packet
		if _, err := vp8.Unmarshal(pkt.Payload); err != nil {
			return false
		}
		if len(vp8.Payload) < 1 {
			return false
		}
		return (vp8.Payload[0] & 0x01) == 0
	case "video/h264":
		if len(pkt.Payload) < 1 {
			return false
		}
		nalType := pkt.Payload[0] & 0x1F
		return nalType == 5
	case "video/vp9":
		if len(pkt.Payload) < 1 {
			return false
		}
		return (pkt.Payload[0] & 0x01) == 0
	default:
		return false
	}
}
