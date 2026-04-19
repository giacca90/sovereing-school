import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { Preset } from 'web-obs';
import { LoginService } from './login.service';

@Injectable({
	providedIn: 'root',
})
export class StreamingService {
	private ws: WebSocket | null = null;

	/* // URL del flujo RTMP
	private readonly rtmpUrlSubject = new BehaviorSubject<string | null>(null);
	rtmpUrl$ = this.rtmpUrlSubject.asObservable(); // lo que expones
 */
	/* // Mensaje de estado para los componentes
	private readonly statusSubject = new BehaviorSubject<string>('');
	status$ = this.statusSubject.asObservable(); // lo que expones
 */
	/* // Avisa si está listo para emitir a los componentes
	private readonly readySubject = new BehaviorSubject<boolean>(false);
	ready$ = this.readySubject.asObservable(); // lo que expones
 */
	/* // Señal que indica si se está emitiendoOBS
	private readonly emisionSubject = new BehaviorSubject<boolean>(false);
	emision$ = this.emisionSubject.asObservable(); // lo que expones */
	public emitiendo: boolean = false;

	// URL del flujo RTMP
	public rtmpUrl: string = '';
	// URL de la previsualización de OBS
	public status: string = '';
	public UrlPreview: string = '';
	public streamId: string | null = null;
	private pc!: RTCPeerConnection;
	private pendingCandidates: RTCIceCandidateInit[] = [];

	constructor(
		private readonly http: HttpClient,
		private readonly loginService: LoginService,
	) {}

	get URL(): string {
		const win = globalThis?.window as any;
		return win?.__env?.BACK_STREAM ?? '';
	}

	get backURL(): string {
		const win = globalThis?.window as any;
		return win?.__env?.BACK_BASE ?? '';
	}

	get backStreamURL(): string {
		const win = globalThis?.window as any;
		return win?.__env?.BACK_STREAM ?? '';
	}

	get webSocketUrlWebcam(): string {
		const win = globalThis?.window as any;
		return win?.__env ? `${win.__env.BACK_STREAM_WSS ?? ''}/live-webcam` : '';
	}

	get webSocketUrlOBS(): string {
		const win = globalThis?.window as any;
		return win?.__env ? `${win.__env.BACK_STREAM_WSS ?? ''}/live-obs` : '';
	}

	/**
	 * Función para obtener el video de un curso y clase
	 * @param idCurso Number: ID del curso
	 * @param idClase Number: ID de la clase
	 * @returns Observable<Blob>: Video obtenido
	 */
	getVideo(idCurso: number, idClase: number): Observable<Blob> {
		return this.http.get(`${this.URL}/${idCurso}/${idClase}`, { responseType: 'blob', withCredentials: true });
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

	/**
	 * Función para iniciar la trasmisión con WebOBS
	 * @returns Promise<string> streamId
	 */
	startWebOBS(): Promise<string> {
		return new Promise((resolve, reject) => {
			const win = globalThis?.window as any;

			if (!win?.WebSocket) {
				const msg = '❌ WebSocket no soportado en este navegador.';
				this.status = msg;
				return reject(new Error(msg));
			}

			const token = localStorage.getItem('Token');
			if (!token) return reject(new Error('Token JWT no encontrado'));

			this.ws = new WebSocket(`${this.webSocketUrlWebcam}?token=${token}`);

			this.ws.onopen = () => {
				console.log('🔗 Conexión WebSocket abierta');
				this.status = 'Conexión establecida. Enviando ID de usuario...';
				this.ws?.send(JSON.stringify({ type: 'userId' }));
			};

			// 👉 Configuramos manejadores una sola vez
			this.setupWebSocketHandlersWebOBS(resolve, reject);
		});
	}

	/**
	 * Función para emitir un video a través de WebOBS
	 * @param stream MediaStream
	 */
	async emitirWebOBS(stream: MediaStream) {
		try {
			if (!stream) throw new Error('No se proporcionó un MediaStream válido.');
			if (!this.ws || this.ws.readyState !== WebSocket.OPEN) throw new Error('WebSocket no está abierto.');
			if (!this.streamId) throw new Error('No hay streamId disponible.');

			const videoTrack = stream.getVideoTracks()[0];
			if (!videoTrack) throw new Error('El MediaStream no contiene pista de video.');

			const { width, height, frameRate } = videoTrack.getSettings();
			if (!width || !height || !frameRate) throw new Error('No se puede emitir sin resolución o FPS.');

			// 📡 Avisar inicio de emisión
			this.ws.send(
				JSON.stringify({
					type: 'emitir',
					videoSettings: { width, height, fps: frameRate },
					streamId: this.streamId,
				}),
			);
			this.emitiendo = true;

			this.pc = new RTCPeerConnection({
				iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
			});
			this.pendingCandidates = [];

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

			// Añadir tracks
			for (const track of stream.getTracks()) {
				const sender = this.pc.addTrack(track, stream);
				if (track.kind === 'video') {
					const params = sender.getParameters();
					if (!params.encodings?.length) params.encodings = [{}];
					params.encodings[0].maxBitrate = 5_000_000;
					params.encodings[0].maxFramerate = frameRate;
					params.degradationPreference = 'maintain-resolution';
					await sender.setParameters(params).catch((err) => console.warn('Error aplicando parámetros de video:', err));
				}
			}

			// Crear oferta
			const offer = await this.pc.createOffer();
			await this.pc.setLocalDescription(offer);
			this.ws.send(JSON.stringify({ type: 'offer', streamId: this.streamId, sdp: offer.sdp }));
		} catch (err) {
			console.error('🚨 Error en emitirWebOBS:', err);
			this.status = `❌ Error: ${(err as Error).message}`;
			this.emitiendo = false;
			throw err;
		}
	}

	/**
	 * Función para detener la grabación de WebOBS
	 */
	detenerWebOBS() {
		if (this.ws) {
			this.ws.send(JSON.stringify({ 'type': 'detenerStreamWebRTC', 'streamId': this.streamId }));
			this.status = 'Deteniendo la emisión...';
		} else {
			console.error('No se pudo detener WebRTC');
			this.status = '<span style="color: red; font-weight: bold;">No se pudo detener WebRTC</span>';
		}
		this.emitiendo = false;
	}

	/**
	 * Función para iniciar la previsualización de OBS
	 */
	startOBS() {
		const win = globalThis?.window as any;

		if (!win?.WebSocket) {
			const msg = '❌ WebSocket no soportado en este navegador.';
			this.status = msg;
			return;
		}

		// Abrir conexión WebSocket
		const token = localStorage.getItem('Token');
		if (!token) {
			console.error('Token JWT no encontrado en localStorage');
			return;
		}

		const url = `${this.webSocketUrlOBS}?token=${token}`;
		console.log('🌐 URL WebSocket OBS:', url);

		this.ws = new WebSocket(url);

		this.ws.onopen = () => {
			console.log('✅ Conexión WebSocket establecida con OBS');
			this.status = 'Conexión WebSocket establecida. Pidiendo URL del servidor...';

			// Pedir la URL RTMP al backend
			const message = { type: 'request_rtmp_url' };
			this.ws?.send(JSON.stringify(message));
		};

		// Configura todos los manejadores
		this.setupWebSocketHandlersOBS();
	}

	/**
	 * Registra el progreso de visualización de un fragmento
	 * @param idCurso ID del curso
	 * @param idClase ID de la clase
	 * @param segment Índice del segmento visto
	 */
	registrarProgreso(idCurso: number, idClase: number, segment: number): Observable<any> {
		return this.http.post(`${this.backStreamURL}/registrar-fragmento`, null, {
			params: {
				idCurso: idCurso.toString(),
				idClase: idClase.toString(),
				segment: segment.toString(),
			},
			withCredentials: true,
		});
	}

	/**
	 * Función para emitir una clase en OBS
	 */
	async emitirOBS() {
		if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
			this.emitiendo = false;
			throw new Error('No se puede conectar con WebSocket');
		}

		if (!this.rtmpUrl) {
			this.emitiendo = false;
			throw new Error('No se puede emitir OBS sin una ruta RTMP válida');
		}

		console.log('📡 Iniciando emisión OBS con RTMP:', this.rtmpUrl);

		this.ws.send(JSON.stringify({ type: 'emitirOBS', rtmpUrl: this.rtmpUrl }));
	}

	/**
	 * Función para detener la grabación de OBS
	 */
	detenerOBS() {
		if (this.ws) {
			this.ws.send(JSON.stringify({ 'type': 'detenerStreamOBS', 'rtmpUrl': this.rtmpUrl }));
			this.status = 'Deteniendo la emisión...';
		} else {
			console.error('No se pudo detener OBS');
			this.status = '<span style="color: red; font-weight: bold;">No se pudo detener OBS</span>';
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

		this.status = 'Transmisión detenida.';
		this.emitiendo = false;
	}

	/**
	 * 🔧 Función auxiliar: configura los manejadores del WebSocket de OBS
	 */
	private setupWebSocketHandlersOBS() {
		if (!this.ws) return;

		this.ws.onmessage = (event) => {
			const data = JSON.parse(event.data);
			console.log('📩 Mensaje recibido (OBS):', data);

			try {
				switch (data.type) {
					// 🔹 Error de autenticación
					case 'auth':
						console.error('Error recibido del servidor:', data.message);
						this.loginService.refreshToken().subscribe({
							next: (token: string | null) => {
								if (token) {
									localStorage.setItem('Token', token);
									this.startOBS(); // reinicia la conexión
								}
							},
							error: (error: HttpErrorResponse) => {
								console.error('Error al refrescar el token:', error.message);
								this.loginService.logout();
							},
						});
						break;

					// 🔹 Backend envía RTMP URL
					case 'rtmp_url':
						console.log('✅ URL RTMP recibida:', data.message);
						this.rtmpUrl = data.message;
						this.status = 'Esperando conexión con OBS...';

						// Prepara la URL para preview
						this.UrlPreview = `${this.URL}/getPreview/${data.message.split('/').pop()}`;
						console.log('🔗 URL Preview:', this.UrlPreview);
						break;

					// 🔹 Error genérico del servidor
					case 'error':
						console.error('Error recibido del servidor:', data.message);
						this.emitiendo = false;
						this.status = `<span style="color: red; font-weight: bold;">Error: ${data.message}</span>`;
						break;

					// 🔹 Inicio de emisión OBS
					case 'start':
						console.log('🎥 Emisión de OBS iniciada.');
						this.emitiendo = true;
						this.status = 'Emisión de OBS iniciada.';
						break;

					// 🔹 Información general
					case 'info':
						console.log('ℹ️ Info recibida del servidor:', data.message);
						this.status = `Info: ${data.message}`;
						break;

					// 🔹 Mensaje desconocido
					default:
						console.warn('⚠️ Mensaje desconocido del servidor OBS:', data);
						break;
				}
			} catch (err) {
				console.error('❌ Error procesando mensaje OBS:', err);
				this.ws?.close();
				this.emitiendo = false;
			}
		};

		this.ws.onerror = (event: Event) => {
			console.error('⚠️ Error en la conexión WebSocket OBS:', event);

			this.emitiendo = false;

			let message = 'Error en la conexión WebSocket';
			if (event instanceof ErrorEvent) message += ': ' + event.message;

			this.status = `<span style="color: red; font-weight: bold;">${message}</span>`;
		};

		this.ws.onclose = () => {
			console.log('🔌 Conexión WebSocket OBS cerrada.');
			this.emitiendo = false;
			this.status = 'Conexión WebSocket cerrada.';
		};
	}

	private setupWebSocketHandlersWebOBS(resolve: (v: string) => void, reject: (e: Error) => void) {
		if (!this.ws) return;
		this.ws.onmessage = async (event) => {
			const data = JSON.parse(event.data);
			console.log('📩 Mensaje recibido:', data);

			try {
				switch (data.type) {
					// 🔹 Autenticación fallida
					case 'auth':
						console.error('Error de autenticación:', data.message);
						this.loginService.refreshToken().subscribe({
							next: (newToken) => {
								if (newToken) {
									localStorage.setItem('Token', newToken);
									this.startWebOBS().then(resolve).catch(reject);
								}
							},
							error: (err: HttpErrorResponse) => {
								console.error('Error al refrescar token:', err.message);
								this.ws?.close();
								this.loginService.logout();
								reject(new Error('Error al refrescar token: ' + err.message));
							},
						});
						break;

					// 🔹 Stream ID confirmado
					case 'streamId':
						console.log('✅ StreamId recibido:', data.streamId);
						this.streamId = data.streamId;
						this.status = 'Todo listo!!';
						resolve(data.streamId as string);
						break;

					// 🔹 Respuesta WebRTC (SDP answer)
					case 'webrtc-answer':
						if (!this.pc) {
							console.error('RTCPeerConnection no inicializada');
							return;
						}
						await this.pc.setRemoteDescription({ type: 'answer', sdp: data.sdp });
						for (const candidate of this.pendingCandidates) {
							await this.pc.addIceCandidate(new RTCIceCandidate(candidate));
						}
						this.pendingCandidates.length = 0;
						console.log('✅ WebRTC completado con éxito');
						this.status = 'Conexión WebRTC completada con éxito';
						break;

					// 🔹 ICE candidate
					case 'candidate':
						if (!this.pc) {
							console.warn('RTCPeerConnection no inicializada');
							return;
						}
						if (this.pc.remoteDescription?.type) {
							await this.pc.addIceCandidate(new RTCIceCandidate(data.candidate));
							console.log('🧊 Candidate remoto añadido:', data.candidate);
						} else {
							this.pendingCandidates.push(data.candidate);
							console.log('🕓 Candidate en espera');
						}
						break;
					case 'info':
						console.log('ℹ️ Info recibida del servidor:', data.message);
						break;
					default:
						console.warn('⚠️ Tipo de mensaje desconocido:', data);
				}
			} catch (err) {
				console.error('❌ Error procesando mensaje:', err);
				this.ws?.close();
				this.emitiendo = false;
			}
		};

		this.ws.onerror = (event: Event) => {
			const error = event as ErrorEvent;
			console.error('⚠️ Error WebSocket:', error.message);
			this.status = `❌ Error en WebSocket: ${error.message}`;
			this.emitiendo = false;
			reject(new Error(error.message));
		};

		this.ws.onclose = () => {
			console.log('🔌 WebSocket cerrado');
			this.status = 'Conexión cerrada.';
			this.emitiendo = false;
		};
	}

	/**
	 * Función para obtener los presets de WebOBS
	 * @returns Observable<Map<string, Preset>>: Presets obtenidos
	 */
	getPresets() {
		return this.http.get(`${this.URL}/presets/get/${this.loginService.usuario?.idUsuario}`, { responseType: 'json', withCredentials: true });
	}

	/**
	 * Función para guardar los presets de WebOBS
	 * @param presets Map<string, Preset>: Presets a guardar
	 */
	savePresets(presets: Map<string, Preset>) {
		const presetsObj = Object.fromEntries(presets);
		console.log('Presets:', presets);
		console.log('Presets JSON:', JSON.stringify(presetsObj));
		console.log('URL:', `${this.URL}/presets/save/${this.loginService.usuario?.idUsuario}`);
		this.http.put(`${this.URL}/presets/save/${this.loginService.usuario?.idUsuario}`, JSON.stringify(presetsObj)).subscribe((response) => {
			console.log('Respuesta:', response);
		});
	}
}
