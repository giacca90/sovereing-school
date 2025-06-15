package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"os"
	"os/exec"
	"strings"
	"sync"
	"time"

	"github.com/pion/rtcp"
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

			// Enviar RTCP PLI para pedir keyframe ANTES de arrancar ffmpeg
			go func() {
				fmt.Fprintf(os.Stderr, "[%s] Enviando RTCP PLI para forzar keyframe...\n", req.SessionID)

				for {
					// Envía PLI solo si la conexión está abierta
					if pc.ConnectionState() == webrtc.PeerConnectionStateClosed {
						return
					}

					// Envía RTCP PLI con el SSRC del track de video
					err := pc.WriteRTCP([]rtcp.Packet{
						&rtcp.PictureLossIndication{
							MediaSSRC: uint32(videoTrack.SSRC()),
						},
					})
					if err != nil {
						fmt.Fprintf(os.Stderr, "[%s] Error enviando RTCP PLI: %v\n", req.SessionID, err)
					} else {
						fmt.Fprintf(os.Stderr, "[%s] RTCP PLI enviado\n", req.SessionID)
					}

					// Espera 1 segundo antes de intentar otra vez si fuera necesario
					time.Sleep(1 * time.Second)
				}
			}()

			// Dale 3 segundos para que llegue el keyframe y luego arranca ffmpeg
			time.AfterFunc(3*time.Second, func() {
				startFFmpeg(req.SessionID, videoTrack, audioTrack, req.Width, req.Height, req.FPS)
			})
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

func startFFmpeg(sessionID string, videoTrack, audioTrack *webrtc.TrackRemote, width, height, fps int) {
	fmt.Fprintf(os.Stderr, "[%s] Arrancando ffmpeg tras esperar keyframe...\n", sessionID)

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

	// Generar SDP
	sdp := fmt.Sprintf(`v=0
o=- 0 0 IN IP4 127.0.0.1
s=WebRTC Stream
c=IN IP4 127.0.0.1
t=0 0
m=audio %d RTP/AVP %d
a=rtpmap:%d %s
m=video %d RTP/AVP %d
a=rtpmap:%d %s
a=framerate:%d
a=framesize:%d %d-%d
`,
		audioPort, audioPT, audioPT, audioCodec,
		videoPort, videoPT, videoPT, videoCodec,
		fps, videoPT, width, height)

	fmt.Fprintf(os.Stderr, "[%s] SDP generado:\n%s\n", sessionID, sdp)

	sdpFile := fmt.Sprintf("/tmp/%s.sdp", sessionID)
	if err := os.WriteFile(sdpFile, []byte(sdp), 0644); err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error escribiendo SDP: %v\n", sessionID, err)
		return
	}
	fmt.Fprintf(os.Stderr, "[%s] Archivo SDP: %s\n", sessionID, sdpFile)

	// Llamar ffmpeg
	cmd := exec.Command("ffmpeg",
		"-protocol_whitelist", "file,udp,rtp",
		"-use_wallclock_as_timestamps", "1",
		"-fflags", "+genpts",
		"-i", sdpFile,
		"-r", fmt.Sprintf("%d", fps),
		"-c:v", "libx264",
		//		"-preset", "ultrafast",
		//		"-tune", "zerolatency",
		//"-profile:v", "high",
		//"-x264-params", "level=6.0:vbv-maxrate=50000:vbv-bufsize=100000",
		"-filter:a", "aresample=async=1",
		"-c:a", "aac",
		"-b:a", "128k",
		"-f", "mpegts",
		"pipe:1",
	)

	cmd.Stderr = os.Stderr
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error creando stdout pipe ffmpeg: %v\n", sessionID, err)
		return
	}
	if err := cmd.Start(); err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error lanzando ffmpeg: %v\n", sessionID, err)
		return
	}

	// Conexiones UDP
	videoConn, err := net.DialUDP("udp", nil, &net.UDPAddr{IP: net.ParseIP("127.0.0.1"), Port: videoPort})
	if err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error UDP video: %v\n", sessionID, err)
		return
	}
	defer videoConn.Close()

	audioConn, err := net.DialUDP("udp", nil, &net.UDPAddr{IP: net.ParseIP("127.0.0.1"), Port: audioPort})
	if err != nil {
		fmt.Fprintf(os.Stderr, "[%s] Error UDP audio: %v\n", sessionID, err)
		return
	}
	defer audioConn.Close()

	// Enviar RTP video
	go func() {
		for {
			pkt, _, err := videoTrack.ReadRTP()
			if err != nil {
				break
			}
			raw, _ := pkt.Marshal()
			videoConn.Write(raw)
		}
	}()

	// Enviar RTP audio
	go func() {
		for {
			pkt, _, err := audioTrack.ReadRTP()
			if err != nil {
				break
			}
			raw, _ := pkt.Marshal()
			audioConn.Write(raw)
		}
	}()

	fmt.Fprintf(os.Stderr, "[%s] Transmitiendo stdout de ffmpeg...\n", sessionID)
	io.Copy(os.Stdout, stdout)
	cmd.Wait()
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
