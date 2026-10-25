import { afterNextRender, Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CursoChat } from '../../../models/CursoChat';
import { InitChatUsuario } from '../../../models/InitChatUsuario';
import { MensajeChat } from '../../../models/MensajeChat';
import { ChatService } from '../../../services/chat.service';
import { LoginService } from '../../../services/login.service';

@Component({
	selector: 'app-home-chat',
	imports: [],
	templateUrl: './home-chat.component.html',
	styleUrl: './home-chat.component.css',
})
export class HomeChatComponent {
	chats = signal<MensajeChat[]>([]);
	cursos = signal<CursoChat[]>([]);
	cargando = signal<boolean>(true);
	public Math = Math;

	/**
	 * Constructor del componente.
	 * @param {Router} router - Router de Angular.
	 * @param {LoginService} loginService - Servicio de autenticación.
	 * @param {ChatService} chatService - Servicio de chat.
	 */
	constructor(
		public router: Router,
		public loginService: LoginService,
		private readonly chatService: ChatService,
	) {
		afterNextRender(() => {
			if (this.loginService.usuario) {
				this.chatService.initSubject.subscribe({
					next: (init: InitChatUsuario | null) => {
						if (init?.mensajes && init.cursos && init.idUsuario === this.loginService.usuario?.idUsuario) {
							this.chats.set(init.mensajes);
							this.cursos.set(init.cursos);
						}
						this.cargando.set(false);
					},
					error: (e: Error) => {
						console.error('Error en recibir la respuesta: ' + e.message);
					},
				});
			} else {
				console.error('El usuario no esta logueado');
				this.router.navigate(['/']);
			}
		});
	}

	/**
	 * Navega a la vista de un mensaje específico.
	 *
	 * @param {MensajeChat} mensaje Mensaje seleccionado.
	 */
	navegaMensaje(mensaje: MensajeChat): void {
		this.router.navigate(['/chat/', mensaje.idCurso?.toString(), mensaje.idMensaje]);
	}

	/**
	 * Navega a la vista de un curso específico.
	 * @param {CursoChat} curso - Curso seleccionado.
	 */
	navegaCurso(curso: CursoChat) {
		this.router.navigate(['/chat/', curso.idCurso.toString()]);
	}
}
