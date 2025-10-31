import { afterNextRender, ChangeDetectorRef, Component, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { CursoChat } from '../../../models/CursoChat';
import { InitChatUsuario } from '../../../models/InitChatUsuario';
import { MensajeChat } from '../../../models/MensajeChat';
import { ChatService } from '../../../services/chat.service';
import { LoginService } from '../../../services/login.service';

@Component({
	selector: 'app-home-chat',
	standalone: true,
	imports: [],
	templateUrl: './home-chat.component.html',
	styleUrl: './home-chat.component.css',
})
export class HomeChatComponent {
	chats: MensajeChat[] = [];
	cursos: CursoChat[] = [];
	cargando: boolean = true;
	public Math = Math;

	constructor(
		public router: Router,
		public loginService: LoginService,
		private readonly chatService: ChatService,
		private readonly cdr: ChangeDetectorRef,
		private readonly ngZone: NgZone,
	) {
		afterNextRender(() => {
			if (this.loginService.usuario) {
				this.chatService.initSubject.subscribe({
					next: (init: InitChatUsuario | null) => {
						if (init?.mensajes && init.cursos && init.idUsuario === this.loginService.usuario?.idUsuario) {
							this.chats = init.mensajes;
							this.cursos = init.cursos;
							this.cargando = false;
							this.cdr.detectChanges();
						} else {
							this.cargando = false;
							this.cdr.detectChanges();
						}
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

	navegaMensaje(mensaje: MensajeChat) {
		this.ngZone.run(() => {
			this.router.navigate(['/chat/', mensaje.idCurso?.toString(), mensaje.idMensaje]);
		});
	}

	navegaCurso(curso: CursoChat) {
		this.ngZone.run(() => {
			this.router.navigate(['/chat/', curso.idCurso.toString()]);
		});
	}
}
