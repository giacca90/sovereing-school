import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, catchError, map, Observable, of } from 'rxjs';
import { VideoElement } from 'web-obs';
import { LoginService } from './login.service';

@Injectable({
	providedIn: 'root',
})
export class StreamingService {
	private ws: WebSocket | null = null;

	private rtmpUrlSubject = new BehaviorSubject<string | null>(null);
	rtmpUrl$ = this.rtmpUrlSubject.asObservable(); // lo que expones

	// Mensaje de estado para los componentes
	private statusSubject = new BehaviorSubject<string>('');
	status$ = this.statusSubject.asObservable(); // lo que expones

	private readySubject = new BehaviorSubject<boolean>(false);
	ready$ = this.readySubject.asObservable(); // lo que expones
	get isReady(): boolean {
		return this.readySubject.getValue();
	}

	public UrlPreview: string = '';
	public streamId: string | null = null;
	private pc!: RTCPeerConnection;
	// TODO: revisar si hace falta almacenar los ICE candidates pendientes
	private pendingCandidates: RTCIceCandidateInit[] = [];

	constructor(
		private http: HttpClient,
		private loginService: LoginService,
	) {}

	get URL(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			return (window as any).__env.BACK_STREAM ?? '';
		}
		return '';
	}

	get backURL(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			return (window as any).__env.BACK_BASE ?? '';
		}
		return '';
	}

	get webSocketUrlWebcam(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			return ((window as any).__env.BACK_STREAM_WSS ?? '') + '/live-webcam';
		}
		return '';
	}

	get webSocketUrlOBS(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			const ruta = ((window as any).__env.BACK_STREAM_WSS ?? '') + '/live-obs';
			return ruta;
		}
		return '';
	}

	/**
	 * Función para obtener el video de un curso y clase
	 * @param id_curso Number: ID del curso
	 * @param id_clase Number: ID de la clase
	 * @returns Observable<Blob>: Video obtenido
	 */
	getVideo(id_curso: number, id_clase: number): Observable<Blob> {
		return this.http.get(`${this.URL}/${id_curso}/${id_clase}`, { responseType: 'blob', withCredentials: true });
	}

	startWebOBS(): Promise<string> {
		return new Promise((resolve, reject) => {
			if (!window.WebSocket) {
				const msg = '<span style="color: red; font-weight: bold;">WebSocket no es compatible con este navegador.</span>';
				this.statusSubject.next(msg);
				return reject(new Error(msg));
			}

			const token = localStorage.getItem('Token');
			if (!token) {
				return reject(new Error('Token JWT no encontrado en localStorage'));
			}

			this.ws = new WebSocket(`${this.webSocketUrlWebcam}?token=${token}`);

			this.ws.onopen = () => {
				console.log('Conexión WebSocket establecida.');
				this.statusSubject.next('Conexión WebSocket establecida. Enviando ID del usuario...');
				const message = { type: 'userId' };
				this.ws?.send(JSON.stringify(message));
			};

			this.ws.onmessage = async (event) => {
				const data = JSON.parse(event.data);
				console.log('Recibido:', data);

				// 🔹 Error de autenticación
				if (data.type === 'auth') {
					console.error('Error recibido del servidor:', data.message);
					this.loginService.refreshToken().subscribe({
						next: (newToken: string | null) => {
							if (newToken) {
								localStorage.setItem('Token', newToken);
								this.startWebOBS().then(resolve).catch(reject);
							}
						},
						error: (error: HttpErrorResponse) => {
							this.ws?.close();
							this.loginService.logout();
							reject(new Error('Error al refrescar el token: ' + error.message));
						},
					});
				}

				// 🔹 Backend confirma streamId
				if (data.type === 'streamId') {
					console.log('ID del stream recibido:', data.streamId);
					this.streamId = data.streamId;
					this.statusSubject.next('Todo listo!!');
					resolve(data.streamId as string);
				}

				// 🔹 Backend devuelve SDP answer
				if (data.type === 'webrtc-answer') {
					try {
						if (!this.pc) {
							console.error('RTCPeerConnection no inicializada');
							return;
						}
						await this.pc.setRemoteDescription({
							type: 'answer',
							sdp: data.sdp,
						});
						console.log('Conexión WebRTC completada con éxito');
						this.statusSubject.next('Conexión WebRTC completada con éxito');
						this.readySubject.next(true);
					} catch (err) {
						console.error('Error al establecer la descripción remota:', err);
						this.ws?.close();
						this.readySubject.next(false);
					}
				}

				// 🔹 Backend envía ICE candidate
				if (data.type === 'candidate') {
					try {
						if (this.pc) {
							await this.pc.addIceCandidate(new RTCIceCandidate(data.candidate));
							console.log('Candidate remoto añadido:', data.candidate);
						}
					} catch (err) {
						console.error('Error al añadir candidate remoto:', err);
					}
				}
			};

			this.ws.onerror = (err) => {
				reject(new Error('Error en WebSocket: ' + JSON.stringify(err)));
			};

			this.ws.onclose = () => {
				this.statusSubject.next('Conexión WebSocket cerrada.');
				this.readySubject.next(false);
			};
		});
	}

	/**
	 * Función para emitir un video a través de WebRTC
	 * @param stream MediaStream
	 * @param clase Objeto Clase
	 */
	async emitirWebOBS(stream: MediaStream) {
		try {
			// 🧩 Validación del MediaStream
			if (!stream) throw new Error('No se ha proporcionado ningún MediaStream válido.');

			const videoTrack = stream.getVideoTracks()[0];
			if (!videoTrack) throw new Error('El MediaStream no contiene pista de video.');

			const { width, height, frameRate } = videoTrack.getSettings();
			if (!width || !height || !frameRate) throw new Error('No se puede emitir sin datos de resolución o FPS.');

			// 🧩 Verificación WebSocket
			if (!this.ws || this.ws.readyState !== WebSocket.OPEN) throw new Error('El WebSocket no está abierto o no existe.');
			if (!this.streamId) throw new Error('No hay streamId asignado.');

			// 📡 Avisar al backend que inicia emisión
			this.ws.send(
				JSON.stringify({
					type: 'emitir',
					videoSettings: { width, height, fps: frameRate },
					streamId: this.streamId,
				}),
			);

			// 🧠 Crear PeerConnection con ICE servers
			const iceServers: RTCIceServer[] = [
				{ urls: 'stun:stun.l.google.com:19302' }, // STUN público
				// { urls: 'turn:turn.myserver.com', username: 'user', credential: 'pass' } // TURN opcional
			];
			this.pc = new RTCPeerConnection({ iceServers });

			// 🔹 Buffer para candidates que lleguen antes de la SDP remota
			const pendingCandidates: RTCIceCandidateInit[] = [];

			// 🔹 Enviar ICE candidates al backend
			this.pc.onicecandidate = (event) => {
				if (event.candidate && this.ws?.readyState === WebSocket.OPEN) {
					this.ws.send(
						JSON.stringify({
							type: 'candidate',
							streamId: this.streamId,
							candidate: event.candidate,
						}),
					);
				}
			};

			// 📊 Logs de estado
			this.pc.oniceconnectionstatechange = () => console.log('ICE state:', this.pc?.iceConnectionState);
			this.pc.onconnectionstatechange = () => console.log('PeerConnection state:', this.pc?.connectionState);

			// 🎥 Añadir tracks de audio y video
			for (const track of stream.getTracks()) {
				const sender = this.pc.addTrack(track, stream);

				if (track.kind === 'video') {
					const params = sender.getParameters();
					if (!params.encodings || params.encodings.length === 0) params.encodings = [{}];
					params.encodings[0].maxBitrate = 5_000_000; // 5 Mbps
					params.encodings[0].scaleResolutionDownBy = 1.0;
					params.encodings[0].maxFramerate = frameRate;
					params.degradationPreference = 'maintain-resolution';

					try {
						await sender.setParameters(params);
					} catch (err) {
						console.warn('Error aplicando parámetros de video:', err);
					}
				}
			}

			// 🧾 Crear y enviar la oferta SDP
			const offer = await this.pc.createOffer();
			await this.pc.setLocalDescription(offer);
			this.ws.send(JSON.stringify({ type: 'offer', streamId: this.streamId, sdp: offer.sdp }));

			// 🔹 Manejar mensajes entrantes del backend
			this.ws.onmessage = async (event) => {
				const data = JSON.parse(event.data);

				if (data.type === 'webrtc-answer') {
					await this.pc.setRemoteDescription({ type: 'answer', sdp: data.sdp });

					// Vaciar buffer de candidates pendientes
					for (const candidate of pendingCandidates) {
						await this.pc.addIceCandidate(new RTCIceCandidate(candidate));
					}
					pendingCandidates.length = 0;

					console.log('✅ Remote description aplicada y candidates pendientes agregados');
				}

				if (data.type === 'candidate') {
					if (this.pc.remoteDescription && this.pc.remoteDescription.type) {
						await this.pc.addIceCandidate(new RTCIceCandidate(data.candidate));
					} else {
						pendingCandidates.push(data.candidate);
					}
				}
			};

			// ⚠️ Eventos de error / cierre de WebSocket
			this.ws.onerror = (event: Event) => {
				const error = event as ErrorEvent;
				console.error('⚠️ Error en WebSocket:', error.message);
				this.statusSubject.next(`❌ Error en WebSocket: ${error.message}`);
				this.readySubject.next(false);
			};

			this.ws.onclose = () => {
				console.log('🔌 WebSocket cerrado');
				this.statusSubject.next('Conexión finalizada.');
				this.readySubject.next(false);
			};
		} catch (err) {
			console.error('🚨 Error en emitirWebOBS:', err);
			this.statusSubject.next(`❌ Error: ${(err as Error).message}`);
			this.readySubject.next(false);
			throw err;
		}
	}

	/**
	 * Método para detener la grabación y la conexión
	 */
	stopMediaStreaming() {
		this.detenerWebOBS();
		this.detenerOBS();

		if (this.ws) {
			this.ws.close(); // Cerrar WebSocket
			console.log('WebSocket cerrado.');
		}

		this.statusSubject.next('Transmisión detenida.');
		this.readySubject.next(false);
	}

	/**
	 * Función para iniciar la previsualización de OBS
	 * @param userId Number: ID del usuario
	 */
	async startOBS() {
		if (!window.WebSocket) {
			console.error('WebSocket no es compatible con este navegador.');
			this.statusSubject.next('WebSocket no es compatible con este navegador.');
			return;
		}

		// Abrir conexión WebSocket
		const token = localStorage.getItem('Token');
		if (token) {
			const url = `${this.webSocketUrlOBS}?token=${token}`;
			console.log('URL:', url);
			this.ws = new WebSocket(url);
		} else {
			console.error('Token JWT no encontrado en localStorage');
			return;
		}

		this.ws.onopen = () => {
			console.log('Conexión WebSocket establecida.');
			this.statusSubject.next('Conexión WebSocket establecida. Enviando ID del usuario...');

			// Enviar el ID del usuario al servidor
			const message = { type: 'request_rtmp_url' };
			this.ws?.send(JSON.stringify(message));
		};

		this.ws.onmessage = (event) => {
			const data = JSON.parse(event.data);

			if (data.type === 'auth') {
				console.error('Error recibido del servidor:', data.message);
				this.loginService.refreshToken().subscribe({
					next: (token: string | null) => {
						if (token) {
							localStorage.setItem('Token', token);
							this.startOBS();
							return;
						}
					},
					error: (error: HttpErrorResponse) => {
						console.error('Error al refrescar el token: ' + error.message);
						this.loginService.logout();
						return;
					},
				});
			}

			if (data.type === 'rtmp_url') {
				console.log('URL RTMP recibida:', data.rtmpUrl);
				this.rtmpUrlSubject.next(data.rtmpUrl);
				return data.rtmpUrl;
			} else if (data.type === 'emitiendoOBS') {
				console.log('Emisión de OBS iniciada.');
				this.readySubject.next(true);
				this.statusSubject.next('Emisión de OBS iniciada.');
			} else if (data.type === 'error') {
				console.error('Error recibido del servidor:', data.message);
				this.readySubject.next(false);
				this.statusSubject.next(`<span style="color: red; font-weight: bold;">Error: ${data.message}</span>`);
			}
		};

		this.ws.onerror = (error) => {
			console.error('Error en la conexión WebSocket:', error);
			this.readySubject.next(false);
			this.statusSubject.next('<span style="color: red; font-weight: bold;">Error en la conexión WebSocket. ' + error + '</span>');
		};

		this.ws.onclose = () => {
			console.log('Conexión WebSocket cerrada.');
			this.readySubject.next(false);
			this.statusSubject.next('Conexión WebSocket cerrada.');
		};
	}

	/**
	 * Función para emitir una clase en OBS
	 * @param curso Curso: Curso en el que se va a emitir la clase
	 * @param clase Clase: Clase que se va a emitir
	 */
	async emitirOBS() {
		if (!this.ws) {
			throw new Error('No se puede conectar con Websocket');
		}
		//this.ws.send(JSON.stringify({ 'event': 'emitirOBS', 'rtmpUrl': this.rtmpUrl }));
		if (!this.rtmpUrl$) throw new Error('No se puede emitir OBS sin ruta RTMP');
		this.readySubject.next(true);

		// Enviar el evento de emitir OBS
		this.ws.send(JSON.stringify({ 'event': 'emitirOBS', 'rtmpUrl': this.rtmpUrl$ }));
		this.ws.onmessage = (event) => {
			const data = JSON.parse(event.data);
			if (data.type === 'start') {
				console.log('Emisión de OBS iniciada.');
				this.readySubject.next(true);
				this.statusSubject.next('Emisión de OBS iniciada.');
			} else if (data.type === 'info') {
				console.log('Info recibida del servidor:', data.message);
				this.statusSubject.next(`Info: ${data.message}`);
			} else if (data.type === 'error') {
				console.error('Error recibido del servidor:', data.message);
				this.readySubject.next(false);
				this.statusSubject.next(`<span style="color: red; font-weight: bold;">Error: ${data.message}</span>`);
				this.ws?.close();
			}
		};
	}

	/**
	 * Función para detener la grabación de OBS
	 */
	detenerOBS() {
		if (this.ws) {
			this.ws.send(JSON.stringify({ 'event': 'detenerStreamOBS', 'rtmpUrl': this.rtmpUrl$ }));
			this.statusSubject.next('Deteniendo la emisión...');
			this.readySubject.next(false);
		} else {
			console.error('No se pudo detener OBS');
			this.statusSubject.next('<span style="color: red; font-weight: bold;">No se pudo detener OBS</span>');
		}
	}

	/**
	 * Función para detener la grabación de WebOBS
	 */
	detenerWebOBS() {
		if (this.ws) {
			this.ws.send(JSON.stringify({ 'type': 'detenerStreamWebRTC', 'streamId': this.streamId }));
			this.statusSubject.next('Deteniendo la emisión...');
			this.readySubject.next(false);
		} else {
			console.error('No se pudo detener WebRTC');
			this.statusSubject.next('<span style="color: red; font-weight: bold;">No se pudo detener WebRTC</span>');
		}
	}

	/**
	 * Función para cerrar las conexiones WebSocket
	 */
	closeConnections() {
		if (this.ws?.OPEN) {
			this.ws.close();
			this.ws = null;
		}
	}

	/**
	 * Función para obtener los presets de WebOBS
	 * @returns Observable<Map<string, { elements: VideoElement[]; shortcut: string }>>: Presets obtenidos
	 */
	getPresets() {
		return this.http.get(`${this.URL}/presets/get/${this.loginService.usuario?.id_usuario}`, { responseType: 'json', withCredentials: true });
	}

	/**
	 * Función para guardar los presets de WebOBS
	 * @param presets Map<string, { elements: VideoElement[]; shortcut: string }>: Presets a guardar
	 */
	savePresets(presets: Map<string, { elements: VideoElement[]; shortcut: string }>) {
		const presetsObj = Object.fromEntries(presets);
		console.log('Presets:', presets);
		console.log('Presets JSON:', JSON.stringify(presetsObj));
		console.log('URL:', `${this.URL}/presets/save/${this.loginService.usuario?.id_usuario}`);
		this.http.put(`${this.URL}/presets/save/${this.loginService.usuario?.id_usuario}`, JSON.stringify(presetsObj)).subscribe((response) => {
			console.log('Respuesta:', response);
		});
	}

	/**
	 * Función para cargar un video estatico
	 * @param file File: Archivo que se va a cargar
	 * @param idCurso Number: ID del curso
	 * @param idClase Number
	 * @returns String | null: Dirección del video o null si hubo un error
	 */
	subeVideo(file: File, idCurso: number, idClase: number): Observable<string | null> {
		const formData = new FormData();
		formData.append('video', file, file.name);

		return this.http.post<string>(this.backURL + '/cursos/subeVideo/' + idCurso + '/' + idClase, formData, { observe: 'response', responseType: 'text' as 'json' }).pipe(
			map((response: HttpResponse<string>) => {
				if (response.ok) {
					return response.body;
				}
				return null;
			}),
			catchError((e: Error) => {
				console.error('Error en subir el video: ' + e.message);
				return of(null);
			}),
		);
	}
}
