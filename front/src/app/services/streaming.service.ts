import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { VideoElement } from '../components/editor-curso/editor-clase/editor-webcam/editor-webcam.component';
import { Clase } from '../models/Clase';
import { Curso } from '../models/Curso';
import { CursosService } from './cursos.service';
import { LoginService } from './login.service';

@Injectable({
	providedIn: 'root',
})
export class StreamingService {
	public enGrabacion: boolean = false;
	private ws: WebSocket | null = null;
	private rtmpUrl: string | null = null;
	public UrlPreview: string = '';
	private streamId: string | null = null;
	private pc!: RTCPeerConnection;

	constructor(
		private http: HttpClient,
		private cursoService: CursosService,
		private loginService: LoginService,
	) {}

	get URL(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			return (window as any).__env.BACK_STREAM ?? '';
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

	getVideo(id_curso: number, id_clase: number): Observable<Blob> {
		return this.http.get(`${this.URL}/${id_curso}/${id_clase}`, { responseType: 'blob', withCredentials: true });
	}

	// TODO: Revisar
	/**
	 * Función para emitir un video a través de WebRTC
	 * @param stream MediaStream
	 * @param clase Objeto Clase
	 */
	async emitirWebcam(stream: MediaStream, clase: Clase | null) {
		if (!window.WebSocket) {
			console.error('WebSocket no es compatible con este navegador.');
			return;
		}

		// Obtener los datos del mediaStream
		const width = stream.getVideoTracks()[0].getSettings().width;
		const height = stream.getVideoTracks()[0].getSettings().height;
		const fps = stream.getVideoTracks()[0].getSettings().frameRate;
		if (!width || !height || !fps) return;

		// Abrir conexión WebSocket
		const token = localStorage.getItem('Token');
		if (token) {
			this.ws = new WebSocket(`${this.webSocketUrlWebcam}?token=${token}`);
		} else {
			console.error('Token JWT no encontrado en localStorage');
			return;
		}

		this.ws.onopen = () => {
			console.log('Conexión WebSocket establecida.');
			// Enviar el ID del usuario al servidor
			const message = { type: 'userId', userId: this.loginService.usuario?.id_usuario, videoSettings: { width: width, height: height, fps: fps } };
			this.ws?.send(JSON.stringify(message));
		};

		// Manejamos los mensajes del WebSocket
		this.ws.onmessage = async (event) => {
			const data = JSON.parse(event.data);
			// Error de autenticación, refrescamos el token
			if (data.type === 'auth') {
				console.error('Error recibido del servidor:', data.message);
				this.loginService.refreshToken().subscribe({
					next: (token: string | null) => {
						if (token) {
							localStorage.setItem('Token', token);
							this.emitirWebcam(stream, clase);
							return;
						}
					},
					error: (error: HttpErrorResponse) => {
						console.error('Error al refrescar el token: ' + error.message);
						this.ws?.close();
						this.loginService.logout();
						return;
					},
				});
			}
			// Recibimos el ID del stream, actualizamos el curso
			if (data.type === 'streamId') {
				console.log('ID del stream recibido:', data.streamId);
				this.streamId = data.streamId;

				// Actualizar el curso
				try {
					if (!clase || !clase.curso_clase) return;

					const curso = await this.cursoService.getCurso(clase.curso_clase); // Asume que devuelve una Promesa
					if (!curso) return;

					clase.direccion_clase = data.streamId;
					curso.clases_curso?.push(clase);

					const success = await firstValueFrom(this.cursoService.updateCurso(curso)); // Esperar Observable
					if (!success) {
						console.error('Falló la actualización del curso');
						return;
					}
					console.log('Curso actualizado');
				} catch (error) {
					console.error('Error al actualizar el curso: ' + error);
					this.ws?.close();
					return;
				}

				// Preparamos WebRTC
				this.pc = new RTCPeerConnection();

				stream.getTracks().forEach((track) => {
					const sender = this.pc.addTrack(track, stream);

					if (track.kind === 'video') {
						const parameters = sender.getParameters();

						if (!parameters.encodings || parameters.encodings.length === 0) {
							parameters.encodings = [{}];
						}

						parameters.encodings[0].maxBitrate = 5_000_000; // 5 Mbps
						parameters.encodings[0].scaleResolutionDownBy = 1.0; // Sin reducción de resolución
						// parameters.encodings[0].minBitrate = 2_000_000; // Opcional, para Chrome

						parameters.degradationPreference = 'maintain-resolution';

						sender.setParameters(parameters).catch((e) => {
							console.warn('Error aplicando parámetros:', e);
						});
					}
				});

				// Crea la oferta WebRTC
				try {
					const offer = await this.pc.createOffer();
					await this.pc.setLocalDescription(offer);

					// Envía oferta al backend con sessionId
					const message = {
						streamId: data.streamId,
						sdp: offer.sdp,
					};
					this.ws?.send(JSON.stringify(message));
				} catch (error) {
					console.error('Error al crear oferta:', error);
					this.ws?.close();
					return;
				}
			}

			// Recibimos la respuesta WebRTC (SDP answer)
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
					this.enGrabacion = true;
				} catch (error) {
					console.error('Error al establecer la descripción remota:', error);
					this.ws?.close();
					return;
				}
			}
		};

		this.ws.onerror = (event: Event) => {
			const error = event as ErrorEvent;
			console.error('Error en WebSocket:', error.message);
			this.enGrabacion = false;
		};

		this.ws.onclose = () => {
			console.log('Cerrando MediaRecorder y WebSocket.');
			this.enGrabacion = false;
		};
	}

	// Método para detener la grabación y la conexión
	stopMediaStreaming() {
		const status = document.getElementById('status') as HTMLParagraphElement;
		this.detenerWebcam();
		this.detenerOBS();

		if (this.ws) {
			this.ws.close(); // Cerrar WebSocket
			console.log('WebSocket cerrado.');
		}

		if (status) {
			status.textContent = 'Transmisión detenida.';
		}

		this.enGrabacion = false;
	}

	async startOBS(userId: number) {
		let status: HTMLParagraphElement | null = null;

		if (!window.WebSocket) {
			console.error('WebSocket no es compatible con este navegador.');
			status = document.getElementById('statusOBS') as HTMLParagraphElement;
			if (status) {
				status.textContent = 'WebSocket no es compatible con este navegador.';
			}
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
			status = document.getElementById('statusOBS') as HTMLParagraphElement;
			if (status) {
				status.textContent = 'Conexión WebSocket establecida. Enviando ID del usuario...';

				// Enviar el ID del usuario al servidor
				const message = { type: 'request_rtmp_url' };
				this.ws?.send(JSON.stringify(message));
			} else {
				console.error('status no encontrado');
			}
		};

		this.ws.onmessage = (event) => {
			const data = JSON.parse(event.data);

			if (data.type === 'auth') {
				console.error('Error recibido del servidor:', data.message);
				this.loginService.refreshToken().subscribe({
					next: (token: string | null) => {
						if (token) {
							localStorage.setItem('Token', token);
							this.startOBS(userId);
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
				this.rtmpUrl = data.rtmpUrl;
				if (status) {
					status.textContent = `URL para OBS recibida:`;
				}

				// Mostrar la URL RTMP al usuario
				const enlaces: HTMLDivElement = document.getElementById('enlaces') as HTMLDivElement;
				if (enlaces) {
					const server: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
					server.textContent = 'URL del servidor';
					const lServer: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
					lServer.classList.add('p-2', 'cursor-pointer', 'rounded-lg', 'border', 'border-black', 'text-blue-700');
					lServer.textContent = (data.rtmpUrl as string).substring(0, (data.rtmpUrl as string).lastIndexOf('/'));
					lServer.onclick = () =>
						navigator.clipboard.writeText(lServer.textContent || '').then(() => {
							const tooltip = document.getElementById('tooltip') as HTMLDivElement;
							if (tooltip) {
								tooltip.textContent = 'Copiado al portapapeles';
								setTimeout(() => {
									tooltip.textContent = 'Haz click para copiar';
								}, 3000);
							}
						});
					// Crear tooltip dinámico para lServer
					lServer.addEventListener('mouseover', (event: MouseEvent) => {
						const tooltip = document.createElement('div');
						tooltip.id = 'tooltip';
						tooltip.textContent = 'Haz click para copiar';
						tooltip.classList.add('absolute', 'bg-black', 'text-white', 'text-xs', 'p-1', 'rounded-sm', 'tooltip');
						// Posicionar el tooltip basado en la posición del ratón
						tooltip.style.position = 'fixed'; // Usamos 'fixed' para que funcione con las coordenadas del mouse
						tooltip.style.top = `${event.clientY - 30}px`; // Ajustar posición encima del ratón
						tooltip.style.left = `${event.clientX}px`; // Alinear con el puntero del ratón
						enlaces.appendChild(tooltip);
					});

					lServer.addEventListener('mousemove', (event: MouseEvent) => moveTooltip(event));

					lServer.addEventListener('mouseleave', () => {
						const tooltip = document.getElementById('tooltip') as HTMLDivElement;
						if (tooltip) {
							tooltip.remove();
						}
					});

					const key: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
					key.textContent = 'Clave del stream';
					const lKey: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
					lKey.classList.add('p-2', 'cursor-pointer', 'rounded-lg', 'border', 'border-black', 'text-blue-700');
					lKey.textContent = (data.rtmpUrl as string).substring((data.rtmpUrl as string).lastIndexOf('/') + 1);
					lKey.onclick = () =>
						navigator.clipboard.writeText(lKey.textContent || '').then(() => {
							const tooltip = document.getElementById('tooltip') as HTMLDivElement;
							if (tooltip) {
								tooltip.textContent = 'Copiado al portapapeles';
								setTimeout(() => {
									tooltip.textContent = 'Haz click para copiar';
								}, 3000);
							}
						});
					lKey.addEventListener('mouseover', (event: MouseEvent) => {
						const tooltip = document.createElement('div');
						tooltip.id = 'tooltip';
						tooltip.textContent = 'Haz click para copiar';
						tooltip.classList.add('absolute', 'bg-black', 'text-white', 'text-xs', 'p-1', 'rounded-sm', 'tooltip');
						// Posicionar el tooltip basado en la posición del ratón
						tooltip.style.position = 'fixed'; // Usamos 'fixed' para que funcione con las coordenadas del mouse
						tooltip.style.top = `${event.clientY - 40}px`; // Ajustar posición encima del ratón
						tooltip.style.left = `${event.clientX}px`; // Alinear con el puntero del ratón
						enlaces.appendChild(tooltip);
					});

					lKey.addEventListener('mousemove', (event: MouseEvent) => moveTooltip(event));

					lKey.addEventListener('mouseleave', () => {
						const tooltip = document.getElementById('tooltip') as HTMLDivElement;
						if (tooltip) {
							tooltip.remove();
						}
					});

					// Mover el tooltip con el ratón mientras esté en el elemento
					const moveTooltip = (moveEvent: MouseEvent) => {
						const tooltip = document.getElementById('tooltip') as HTMLDivElement;
						tooltip.style.top = `${moveEvent.clientY - 30}px`;
						tooltip.style.left = `${moveEvent.clientX}px`;
					};
					enlaces.appendChild(server);
					enlaces.appendChild(lServer);
					enlaces.appendChild(key);
					enlaces.appendChild(lKey);
				}

				// Devuelve la URL para la preview
				this.UrlPreview = this.URL + '/getPreview/' + data.rtmpUrl.split('/').pop();
			} else if (data.type === 'emitiendoOBS') {
				console.log('Emisión de OBS iniciada.');
				this.enGrabacion = true;
				if (status) {
					status.textContent = 'Emisión de OBS iniciada.';
				}
			} else if (data.type === 'error') {
				console.error('Error recibido del servidor:', data.message);
				this.enGrabacion = false;
				if (status) {
					status.textContent = `Error: ${data.message}`;
				}
			}
		};

		this.ws.onerror = (error) => {
			console.error('Error en la conexión WebSocket:', error);
			this.enGrabacion = false;
			if (status) {
				status.textContent = 'Error en la conexión WebSocket.';
			}
		};

		this.ws.onclose = () => {
			console.log('Conexión WebSocket cerrada.');
			this.enGrabacion = false;
			if (status) {
				status.textContent = 'Conexión WebSocket cerrada.';
			}
		};
	}

	emitirOBS(clase: Clase | null) {
		const status = document.getElementById('statusOBS');
		if (!this.ws) {
			throw new Error('No se puede conectar con Websocket');
		}
		//this.ws.send(JSON.stringify({ 'event': 'emitirOBS', 'rtmpUrl': this.rtmpUrl }));
		if (status) {
			status.textContent = 'Creando la clase...';
		}
		this.enGrabacion = true;
		if (!clase || !this.rtmpUrl) throw new Error('No se puede emitir OBS sin ruta RTMP');
		clase.direccion_clase = this.rtmpUrl.substring(this.rtmpUrl.lastIndexOf('/') + 1);
		if (!clase.curso_clase) {
			this.enGrabacion = false;
			this.ws?.close();
			console.error('No se puede emitir OBS sin curso');
			throw new Error('No se puede emitir OBS sin curso');
		}
		this.cursoService.getCurso(clase.curso_clase).then((curso) => {
			if (!curso || !curso.clases_curso) {
				this.enGrabacion = false;
				this.ws?.close();
				console.error('Falló la actualización del curso');
				throw new Error('Falló la actualización del curso');
			}
			clase.posicion_clase = curso.clases_curso.length + 1;
			curso.clases_curso?.push(clase);
			this.cursoService.updateCurso(curso).subscribe({
				next: (success: Curso) => {
					if (!success || !status || !this.ws) throw new Error('Falló la actualización del curso');
					console.log('Curso actualizado con éxito');
					status.textContent = 'Clase creada, iniciando la emisión...';
					this.ws.send(JSON.stringify({ 'event': 'emitirOBS', 'rtmpUrl': this.rtmpUrl }));
					this.ws.onmessage = (event) => {
						const data = JSON.parse(event.data);
						if (data.type === 'start') {
							console.log('Emisión de OBS iniciada.');
							this.enGrabacion = true;
							status.textContent = 'Emisión de OBS iniciada.';
						} else if (data.type === 'info') {
							console.log('Info recibida del servidor:', data.message);
							status.textContent = `Info: ${data.message}`;
						} else if (data.type === 'error') {
							console.error('Error recibido del servidor:', data.message);
							this.enGrabacion = false;
							status.textContent = `Error: ${data.message}`;
							this.ws?.close();
						}
					};
				},
				error: (error) => {
					this.enGrabacion = false;
					this.ws?.close();
					console.error('Error al actualizar el curso: ' + error);
					throw new Error('Error al actualizar el curso: ' + error);
				},
			});
		});
	}

	detenerOBS() {
		const status = document.getElementById('statusOBS');
		if (this.ws) {
			this.ws.send(JSON.stringify({ 'event': 'detenerStreamOBS', 'rtmpUrl': this.rtmpUrl }));
			if (status) {
				status.textContent = 'Deteniendo la emisión...';
			}
			this.enGrabacion = true;
		} else {
			console.error('No se pudo detener OBS');
			if (status) {
				status.textContent = 'No se pudo detener OBS';
			}
		}
	}

	detenerWebcam() {
		const status = document.getElementById('status') as HTMLParagraphElement;
		if (this.ws) {
			this.ws.send(JSON.stringify({ 'event': 'detenerStreamWebcam', 'streamId': this.streamId }));
			if (status) {
				status.textContent = 'Deteniendo la emisión...';
			}
			this.enGrabacion = true;
		} else {
			console.error('No se pudo detener WebRTC');
			if (status) {
				status.textContent = 'No se pudo detener WebRTC';
			}
		}
	}

	closeConnections() {
		if (this.ws?.OPEN) {
			this.ws.close();
			this.ws = null;
		}
	}

	getPresets() {
		return this.http.get(`${this.URL}/presets/get/${this.loginService.usuario?.id_usuario}`, { responseType: 'json', withCredentials: true });
	}

	savePresets(presets: Map<string, { elements: VideoElement[]; shortcut: string }>) {
		const presetsObj = Object.fromEntries(presets);
		console.log('Presets:', presets);
		console.log('Presets JSON:', JSON.stringify(presetsObj));
		console.log('URL:', `${this.URL}/presets/save/${this.loginService.usuario?.id_usuario}`);
		this.http.put(`${this.URL}/presets/save/${this.loginService.usuario?.id_usuario}`, JSON.stringify(presetsObj)).subscribe((response) => {
			console.log('Respuesta:', response);
		});
	}
}
