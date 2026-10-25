import { afterNextRender, Component, ElementRef, input, OnDestroy, signal, viewChild, viewChildren } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { CursoChat } from '../../../models/CursoChat';
import { MensajeChat } from '../../../models/MensajeChat';
import { ChatService } from '../../../services/chat.service';
import { LoginService } from '../../../services/login.service';

@Component({
	selector: 'app-chat',
	imports: [RouterModule],
	templateUrl: './chat.component.html',
	styleUrl: './chat.component.css',
})
export class ChatComponent implements OnDestroy {
	/**
	 * Inputs inyectados directamente por Angular (ya sea por binding del padre
	 * o mediante conComponentInputBinding() desde las rutas).
	 */
	idCurso = input<number | string | null>(null);
	idMensaje = input<string | null>(null);

	readonly mexElements = viewChildren<ElementRef>('mexElement');
	readonly mexcInputs = viewChildren<ElementRef<HTMLInputElement>>('mexcInput');
	readonly claseElements = viewChildren<ElementRef>('claseElement');
	readonly arrowElements = viewChildren<ElementRef>('arrowElement');
	readonly mexInput = viewChild.required<ElementRef<HTMLInputElement>>('mexInput');

	chat = signal<CursoChat | null>(null);
	claseAbiertaId = signal<number | null>(null);
	respuesta = signal<MensajeChat | null>(null);
	respuestaClase = signal<MensajeChat | null>(null);
	subscription: Subscription | null = null;
	pregunta = signal<{ minute: number; second: number } | null>(null);
	public Math = Math;

	constructor(
		public chatService: ChatService,
		public loginService: LoginService,
		private readonly router: Router,
	) {
		afterNextRender(() => {
			this.cargarDatosChat();
		});
	}

	/**
	 * Carga el chat y realiza el desplazamiento al mensaje si corresponde.
	 */
	private cargarDatosChat(): void {
		const cursoId = Number(this.idCurso());

		if (!cursoId) {
			console.error('El curso es nulo o inválido');
			return;
		}

		if (this.idMensaje()) {
			this.chatService.mensajeLeido(this.idMensaje()!);
		}

		this.subscription = this.chatService.getChat(cursoId).subscribe({
			next: (data: CursoChat | null) => {
				if (!data) return;
				this.chat.set(data);

				requestAnimationFrame(() => {
					if (this.idMensaje()) {
						const msgId = this.idMensaje();

						if (data.mensajes.some((mensaje) => mensaje.idMensaje === msgId)) {
							const target = this.mexElements().find((el) => el.nativeElement.id === 'mex-' + msgId);
							if (target) {
								target.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
								target.nativeElement.focus();
							}
						} else {
							for (const clase of data.clases) {
								if (clase.mensajes.some((mex) => mex.idMensaje === msgId)) {
									this.abreChatClase(clase.idClase);
									const target = this.mexElements().find((el) => el.nativeElement.id === 'mex-' + msgId);

									if (target) {
										target.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
										target.nativeElement.focus();
										return;
									}
								}
							}
						}
					}
				});
			},
			error: (e) => {
				console.error('Error en recibir el chat: ' + e.message);
			},
		});
	}

	/**
	 * Limpieza de recursos al destruir el componente.
	 */
	ngOnDestroy(): void {
		this.chat.set(null);
		this.respuesta.set(null);
		this.subscription?.unsubscribe();
	}

	/**
	 * Desplaza la vista a un mensaje específico.
	 *
	 * @param {string} idMensaje ID del mensaje.
	 */
	scrollToMessage(idMensaje: string): void {
		const target = this.mexElements().find((el) => el.nativeElement.id === 'mex-' + idMensaje);
		if (target) {
			target.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
			target.nativeElement.focus();
		} else {
			for (const clase of this.chat()?.clases || []) {
				if (clase.mensajes.some((mex) => mex.idMensaje === idMensaje)) {
					this.abreChatClase(clase.idClase);
					requestAnimationFrame(() => {
						const innerTarget = this.mexElements().find((el) => el.nativeElement.id === 'mex-' + idMensaje);
						if (innerTarget) {
							innerTarget.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
							innerTarget.nativeElement.focus();
						}
					});
					return;
				}
			}
		}
	}

	/**
	 * Envía un mensaje al chat.
	 *
	 * @param {number} [clase] ID de la clase.
	 */
	enviarMensaje(clase?: number): void {
		const cursoId = Number(this.idCurso());
		if (!cursoId) {
			console.error('El curso es null o inválido');
			return;
		}

		if (clase) {
			let resp: string | null = null;
			if (this.respuestaClase()) {
				resp = this.respuestaClase()!.idMensaje;
			}
			const target = this.mexcInputs().find((el) => el.nativeElement.id === 'mexc-' + clase);
			if (target && target.nativeElement.value) {
				this.chatService.enviarMensaje(cursoId, clase, target.nativeElement.value, resp, this.pregunta());
				target.nativeElement.value = '';
				target.nativeElement.placeholder = 'Escribe tu mensaje en la clase...';
				this.respuesta.set(null);
				this.respuestaClase.set(null);
				this.pregunta.set(null);
			}
		} else {
			let resp: string | null = null;
			if (this.respuesta()) {
				resp = this.respuesta()!.idMensaje;
			}
			const input = this.mexInput().nativeElement;
			if (input.value) {
				this.chatService.enviarMensaje(cursoId, 0, input.value, resp, this.pregunta());
				input.value = '';
				this.respuesta.set(null);
				this.respuestaClase.set(null);
				this.pregunta.set(null);
			}
		}
	}

	abreChatClase(idClase: number): void {
		if (this.claseAbiertaId() === idClase) {
			this.claseAbiertaId.set(null);
		} else {
			this.claseAbiertaId.set(idClase);
		}
	}

	creaPregunta(idClase: number, momento: number): void {
		this.abreChatClase(idClase);
		const minutes = Math.floor(momento / 60);
		const seconds = Math.floor(momento % 60);
		this.pregunta.set({ minute: minutes, second: seconds });
		const target = this.mexcInputs().find((el) => el.nativeElement.id === 'mexc-' + idClase);
		if (target) {
			target.nativeElement.placeholder = `Haz una pregunta en ${minutes}:${seconds}`;
			target.nativeElement.focus();
		}
	}

	cierraPregunta(idClase: number): void {
		this.respuesta.set(null);
		this.respuestaClase.set(null);
		this.pregunta.set(null);
		const target = this.mexcInputs().find((el) => el.nativeElement.id === 'mexc-' + idClase);
		if (target) {
			target.nativeElement.placeholder = 'Escribe tu mensaje en la clase...';
		}
	}

	navegaAlVideo(idCurso: number, idClase: number, momento?: number): void {
		if (momento) {
			this.router.navigate(['/repro', this.loginService.usuario?.idUsuario, idCurso, idClase], { queryParams: { momento } });
		} else {
			this.router.navigate(['/repro', this.loginService.usuario?.idUsuario, idCurso, idClase]);
		}
	}
}
