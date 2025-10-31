import { afterNextRender, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
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
	styleUrl: './chat.component.css',
})
export class ChatComponent implements OnInit, OnDestroy {
	@Input() idCurso: number | null = null;
	chat: CursoChat | null = null;
	respuesta: MensajeChat | null = null;
	respuestaClase: MensajeChat | null = null;
	subscription: Subscription | null = null;
	idMensaje: string | null = null;
	pregunta: { minute: number; second: number } | null = null;
	public Math = Math;

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
						if (data) {
							this.chat = data;
							this.cdr.detectChanges();
							if (this.idMensaje) {
								if (data.mensajes.filter((mensaje) => mensaje.idMensaje === this.idMensaje) && data.mensajes.filter((mensaje) => mensaje.idMensaje === this.idMensaje).length > 0) {
									const mexc: HTMLElement | null = document.getElementById('mex-' + this.idMensaje);
									if (mexc) {
										mexc.scrollIntoView({ behavior: 'smooth', block: 'center' });
										mexc.focus();
									}
								} else {
									data.clases.forEach((clase) => {
										if (clase.mensajes.filter((mex) => mex.idMensaje === this.idMensaje) && clase.mensajes.filter((mex) => mex.idMensaje === this.idMensaje).length > 0) {
											this.abreChatClase(clase.idClase);
											this.cdr.detectChanges();
											const mexc = document.getElementById('mex-' + this.idMensaje);

											if (mexc) {
												mexc.scrollIntoView({ behavior: 'smooth', block: 'center' });
												mexc.focus();
												return;
											}
										}
									});
								}
							}
							this.cdr.detectChanges();
						}
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

	ngOnInit(): void {
		if (!this.idCurso) {
			this.route.paramMap.subscribe((params) => {
				this.idCurso = params.get('idCurso') as number | null;
			});
		}
	}

	ngOnDestroy(): void {
		this.idCurso = null;
		this.chat = null;
		this.respuesta = null;
		this.subscription?.unsubscribe();
	}

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
			const input: HTMLInputElement = document.getElementById('mexc-' + clase) as HTMLInputElement;
			if (input.value) {
				this.chatService.enviarMensaje(this.idCurso, clase, input.value, resp, this.pregunta);
				input.value = '';
				input.placeholder = 'Escribe tu mensaje en la clase...';
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
			const input: HTMLInputElement = document.getElementById('mex') as HTMLInputElement;
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
		const claseElement = document.getElementById('clase-' + idClase);
		const flechaElement = document.getElementById('arrow-' + idClase);

		// Si ya está visible, se oculta
		if (!claseElement?.classList.contains('hidden')) {
			claseElement?.classList.add('hidden');
			flechaElement?.classList.remove('rotate-180');
		} else {
			// Oculta todas las cortinas y resetea las flechas
			const clases: NodeListOf<Element> = document.querySelectorAll('.clases');
			const flechas: NodeListOf<Element> = document.querySelectorAll('.arrow');

			clases.forEach((clase) => {
				clase.classList.add('hidden');
			});

			flechas.forEach((flecha) => {
				flecha.classList.remove('rotate-180');
			});

			// Muestra la cortina actual y rota su flecha
			claseElement?.classList.remove('hidden');
			flechaElement?.classList.add('rotate-180');
		}
	}

	creaPregunta(idClase: number, momento: number) {
		this.abreChatClase(idClase);
		const minutes = Math.floor(momento / 60);
		const seconds = Math.floor(momento % 60);
		this.pregunta = { minute: minutes, second: seconds };
		const input: HTMLInputElement = document.getElementById('mexc-' + idClase) as HTMLInputElement;
		input.placeholder = `Haz una pregunta en ${minutes}:${seconds}`;
		input.focus();
	}

	cierraPregunta(idClase: number) {
		this.respuesta = null;
		this.respuestaClase = null;
		this.pregunta = null;
		const input: HTMLInputElement = document.getElementById('mexc-' + idClase) as HTMLInputElement;
		input.placeholder = 'Escribe tu mensaje en la clase...';
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
