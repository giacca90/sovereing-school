import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, catchError, map, Observable, of, Subject, switchMap, takeUntil, timer } from 'rxjs';
import { CursoChat } from '../models/CursoChat';
import { InitChatUsuario } from '../models/InitChatUsuario';
import { MensajeChat } from '../models/MensajeChat';
import { LoginService } from './login.service';

@Injectable({
	providedIn: 'root',
})
export class ChatService {
	private jwtToken: string | null = localStorage.getItem('Token');

	public initSubject = new BehaviorSubject<InitChatUsuario | null>(null);
	private readonly cursoSubject = new BehaviorSubject<CursoChat | null>(null);
	private readonly unsubscribe$ = new Subject<void>();

	private client: Client = new Client({
		brokerURL: this.urlWss + '?token=' + this.jwtToken,
		reconnectDelay: 1000,
	});

	private currentSubscription: StompSubscription | null = null; // Asegúrate de tener esta referencia

	constructor(
		private readonly loginService: LoginService,
		@Inject(PLATFORM_ID) private readonly platformId: object,
		private readonly http: HttpClient,
	) {
		if (isPlatformBrowser(this.platformId)) {
			this.client.onWebSocketError = (error) => {
				console.error('Error con WebSocket', error.message);
			};

			this.client.onStompError = (frame) => {
				const message = frame.headers['message'];
				if (message?.includes('Token inválido')) {
					this.loginService.refreshToken().subscribe({
						next: (token: string | null) => {
							if (token) {
								// Actualiza el localStorage y jwtToken
								localStorage.setItem('Token', token);
								this.jwtToken = token;

								// Desactiva el cliente STOMP anterior antes de crear uno nuevo
								if (this.client) {
									this.client.deactivate(); // Desactiva el cliente STOMP actual
								}

								// Crea un nuevo cliente STOMP con el nuevo token
								this.client = new Client({
									brokerURL: this.urlWss + '?token=' + this.jwtToken,
									reconnectDelay: 1000, // Esperar entre intentos de reconexión
									onConnect: (frame) => {
										this.initUsuario();
									},
									onDisconnect: () => {
										console.log('Cliente STOMP desconectado');
									},
								});

								// Activar el nuevo cliente STOMP
								this.client.activate();
							} else {
								console.error('No se pudo refrescar el token');
							}
						},
						error: (error: HttpErrorResponse) => {
							console.error('Error al refrescar el token: ' + error.message);
							this.loginService.logout();
						},
					});
				}
			};

			this.client.onConnect = (frame) => {
				this.initUsuario();
			};

			// Activa el WebSocket
			this.client.activate();
		}
	}

	get urlWss(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			const url = (globalThis.window as any).__env.BACK_CHAT_WSS ?? '';
			return url + '/chat-socket';
		}
		return '';
	}

	get url(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			const url = (globalThis.window as any).__env.BACK_CHAT ?? '';
			return url;
		}
		return '';
	}

	private initUsuario(): Observable<InitChatUsuario | null> {
		// Suscríbete a las respuestas del backend
		this.client.subscribe('/user/init_chat/result', (response) => {
			if (response.body.startsWith('Token inválido')) {
				this.loginService.refreshToken().subscribe({
					next: (token: string | null) => {
						if (token) {
							localStorage.setItem('Token', token);
							this.jwtToken = token;
							// Enviar el nuevo token al backend
							this.client.publish({
								destination: '/app/refresh-token',
								body: token,
							});
							// Publicar el mensaje al backend
							this.client.publish({
								destination: '/app/init',
								body: '',
							});
						} else {
							console.error('No se pudo refrescar el token');
							this.client.deactivate();
							this.loginService.logout();
						}
					},
					error: (error: HttpErrorResponse) => {
						console.error('Error al refrescar el token: ' + error.message);
						this.client.deactivate();
						this.loginService.logout();
					},
				});
			}
			const init: InitChatUsuario = JSON.parse(response.body) as InitChatUsuario;

			// Emitir el valor recibido a través del subject
			this.initSubject.next(init);
		});

		// Publicar el mensaje al backend
		this.client.publish({
			destination: '/app/init',
			body: '',
		});

		// Devolver el observable que los componentes pueden suscribirse
		return this.initSubject.asObservable();
	}

	getChat(idCurso: number): Observable<CursoChat | null> {
		// Si no está conectado, esperar 500ms antes de continuar
		if (!this.client.connected) {
			return timer(500).pipe(
				// Retrasa la ejecución 500ms
				switchMap(() => this.getChat(idCurso)), // Llama a getChat nuevamente después del retraso
			);
		}

		// Si ya hay una suscripción activa, desuscríbete primero
		if (this.currentSubscription) {
			this.currentSubscription.unsubscribe();
			this.cursoSubject.next(null); // Limpiar el estado actual del curso
		}

		// Suscríbete a las respuestas del backend
		this.currentSubscription = this.client.subscribe('/init_chat/' + idCurso, (response) => {
			const curso: CursoChat = JSON.parse(response.body) as CursoChat;
			// Emitir el valor recibido
			this.cursoSubject.next(curso);
		});

		// Publicar el mensaje al backend
		this.client.publish({
			destination: '/app/curso',
			body: idCurso.toString(),
		});

		// Devolver el observable que los componentes pueden suscribirse
		return this.cursoSubject.asObservable().pipe(
			takeUntil(this.unsubscribe$), // Desuscribirse cuando sea necesario
		);
	}

	enviarMensaje(idCurso: number | null, clase: number, value: string, respuesta: string | null, pregunta: { minute: number; second: number } | null) {
		let resp: MensajeChat | null = null;
		let preg: number | null = null;
		if (pregunta) {
			preg = pregunta.minute * 60 + pregunta.second;
		}
		if (respuesta) {
			resp = new MensajeChat(
				respuesta, // id mensaje
				idCurso, // id curso
				clase, // id clase
				this.loginService.usuario?.idUsuario, // id usuario
				null, // nombre curso
				null, // nombre clase
				null, // nombre usuario
				null, // foto curso
				null, // foto usuario
				null, // respuesta
				preg, // pregunta
				null, // mensaje
				null, // fecha
			);
		}

		const mensaje: MensajeChat = new MensajeChat(
			null, // id mensaje
			idCurso, // id curso
			clase, // id clase
			this.loginService.usuario?.idUsuario, // id usuario
			null, // nombre curso
			null, // nombre clase
			null, // nombre usuario
			null, // foto curso
			null, // foto usuario
			resp, // respuesta
			preg, // pregunta
			value, // mensaje
			new Date(), // fecha
		);

		// Publicar el mensaje al backend
		this.client.publish({
			destination: '/app/chat',
			body: JSON.stringify(mensaje),
		});
	}

	mensajeLeido(idMensaje: string) {
		this.client.publish({
			destination: '/app/leido',
			body: JSON.stringify(this.loginService.usuario?.idUsuario + ',' + idMensaje),
		});
	}

	getAllChats() {
		return this.http.get<CursoChat[]>(this.url + '/getAll', { observe: 'response' }).pipe(
			map((response: HttpResponse<CursoChat[]>) => {
				if (response.status === 200) {
					return response.body;
				}
				return [];
			}),
			catchError((e: Error) => {
				console.error('Error en obtener todos los usuarios: ' + e.message);
				return of([]);
			}),
		);
	}
}
