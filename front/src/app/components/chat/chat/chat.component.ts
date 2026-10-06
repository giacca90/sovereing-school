import { afterNextRender, ChangeDetectorRef, Component, ElementRef, Input, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { CursoChat } from '../../../models/CursoChat';
import { MensajeChat } from '../../../models/MensajeChat';
import { ChatService } from '../../../services/chat.service';
import { LoginService } from '../../../services/login.service';

@Component({
	selector: 'app-chat',
	standalone: true,
	imports: [RouterModule],
	templateUrl: './chat.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './chat.component.css',
})
export class ChatComponent implements OnInit, OnDestroy {
	@Input() idCurso: number | null = null;
	@ViewChildren('mexElement') mexElements!: QueryList<ElementRef>;
	@ViewChildren('mexcInput') mexcInputs!: QueryList<ElementRef<HTMLInputElement>>;
	@ViewChildren('claseElement') claseElements!: QueryList<ElementRef>;
	@ViewChildren('arrowElement') arrowElements!: QueryList<ElementRef>;
	@ViewChild('mexInput') mexInput!: ElementRef<HTMLInputElement>;

	chat: CursoChat | null = null;
	claseAbiertaId: number | null = null;
	respuesta: MensajeChat | null = null;
	respuestaClase: MensajeChat | null = null;
	subscription: Subscription | null = null;
	idMensaje: string | null = null;
	pregunta: { minute: number; second: number } | null = null;
	public Math = Math;

	/**
	 * Constructor del componente.
	 * @param {ChatService} chatService - Servicio de chat.
	 * @param {LoginService} loginService - Servicio de autenticación.
	 * @param {ActivatedRoute} route - Ruta activada.
	 * @param {Router} router - Router de Angular.
	 * @param {ChangeDetectorRef} cdr - Detección de cambios.
	 */
	constructor(
		public chatService: ChatService,
		public loginService: LoginService,
		private readonly route: ActivatedRoute,
		private readonly router: Router,
		public cdr: ChangeDetectorRef,
	) {
		if (!this.idCurso) {
			this.route.paramMap.subscribe((params) => {
				this.idCurso = params.get('idCurso') as number | null;
				this.idMensaje = params.get('idMensaje');
			});
		}
		afterNextRender(() => {
			if (this.idCurso && this.idMensaje) {
				this.chatService.mensajeLeido(this.idMensaje);
			}
			if (this.idCurso) {
				this.subscription = this.chatService.getChat(this.idCurso).subscribe({
					next: (data: CursoChat | null) => {
						if (!data) return;
						this.chat = data;
						this.cdr.detectChanges();
						if (this.idMensaje) {
							if (data.mensajes.some((mensaje) => mensaje.idMensaje === this.idMensaje)) {
								const target = this.mexElements.find((el) => el.nativeElement.id === 'mex-' + this.idMensaje);
								if (target) {
									target.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
									target.nativeElement.focus();
								}
							} else {
								for (const clase of data.clases) {
									if (clase.mensajes.some((mex) => mex.idMensaje === this.idMensaje)) {
										this.abreChatClase(clase.idClase);
										this.cdr.detectChanges();
										const target = this.mexElements.find((el) => el.nativeElement.id === 'mex-' + this.idMensaje);

										if (target) {
											target.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
											target.nativeElement.focus();
											return;
										}
									}
								}
							}
						}
						this.cdr.detectChanges();
					},
					error: (e) => {
						console.error('Error en recibir el chat: ' + e.message);
					},
				});
			} else {
				console.error('El curso es nulo');
			}
		});
	}

	/**
	 * Inicialización del componente.
	 */
	ngOnInit(): void {
		if (!this.idCurso) {
			this.route.paramMap.subscribe((params) => {
				this.idCurso = params.get('idCurso') as number | null;
			});
		}
	}

	/**
	 * Limpieza de recursos al destruir el componente.
	 */
	ngOnDestroy(): void {
		this.idCurso = null;
		this.chat = null;
		this.respuesta = null;
		this.subscription?.unsubscribe();
	}

	/**
	 * Desplaza la vista a un mensaje específico.
	 * @param {string} idMensaje - ID del mensaje.
	 */
	scrollToMessage(idMensaje: string) {
		const target = this.mexElements.find((el) => el.nativeElement.id === 'mex-' + idMensaje);
		if (target) {
			target.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
			target.nativeElement.focus();
		} else {
			// Si no se encuentra, intentamos abrir el chat de la clase
			for (const clase of this.chat?.clases || []) {
				if (clase.mensajes.some((mex) => mex.idMensaje === idMensaje)) {
					this.abreChatClase(clase.idClase);
					this.cdr.detectChanges();
					setTimeout(() => {
						const innerTarget = this.mexElements.find((el) => el.nativeElement.id === 'mex-' + idMensaje);
						if (innerTarget) {
							innerTarget.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
							innerTarget.nativeElement.focus();
						}
					}, 100);
					return;
				}
			}
		}
	}

	/**
	 * Envía un mensaje al chat.
	 * @param {number} [clase] - ID de la clase.
	 */
	enviarMensaje(clase?: number) {
		if (this.idCurso === null) {
			console.error('El curso es null');
			return;
		}
		if (clase) {
			let resp: string | null = null;
			if (this.respuestaClase) {
				resp = this.respuestaClase.idMensaje;
			}
			const target = this.mexcInputs.find((el) => el.nativeElement.id === 'mexc-' + clase);
			if (target && target.nativeElement.value) {
				this.chatService.enviarMensaje(this.idCurso, clase, target.nativeElement.value, resp, this.pregunta);
				target.nativeElement.value = '';
				target.nativeElement.placeholder = 'Escribe tu mensaje en la clase...';
				this.respuesta = null;
				this.respuestaClase = null;
				this.pregunta = null;
				this.cdr.detectChanges();
			}
		} else {
			let resp: string | null = null;
			if (this.respuesta) {
				resp = this.respuesta.idMensaje;
			}
			const input = this.mexInput.nativeElement;
			if (input.value) {
				this.chatService.enviarMensaje(this.idCurso, 0, input.value, resp, this.pregunta);
				input.value = '';
				this.respuesta = null;
				this.respuestaClase = null;
				this.pregunta = null;
				this.cdr.detectChanges();
			}
		}
	}

	abreChatClase(idClase: number) {
		if (this.claseAbiertaId === idClase) {
			this.claseAbiertaId = null;
		} else {
			this.claseAbiertaId = idClase;
		}
		this.cdr.detectChanges();
	}

	creaPregunta(idClase: number, momento: number) {
		this.abreChatClase(idClase);
		const minutes = Math.floor(momento / 60);
		const seconds = Math.floor(momento % 60);
		this.pregunta = { minute: minutes, second: seconds };
		const target = this.mexcInputs.find((el) => el.nativeElement.id === 'mexc-' + idClase);
		if (target) {
			target.nativeElement.placeholder = `Haz una pregunta en ${minutes}:${seconds}`;
			target.nativeElement.focus();
		}
	}

	cierraPregunta(idClase: number) {
		this.respuesta = null;
		this.respuestaClase = null;
		this.pregunta = null;
		const target = this.mexcInputs.find((el) => el.nativeElement.id === 'mexc-' + idClase);
		if (target) {
			target.nativeElement.placeholder = 'Escribe tu mensaje en la clase...';
		}
		this.cdr.detectChanges();
	}

	navegaAlVideo(idCurso: number, idClase: number, momento?: number) {
		if (momento) {
			this.router.navigate(['/repro', this.loginService.usuario?.idUsuario, idCurso, idClase], { queryParams: { momento } });
		} else {
			this.router.navigate(['/repro', this.loginService.usuario?.idUsuario, idCurso, idClase]);
		}
	}
}
