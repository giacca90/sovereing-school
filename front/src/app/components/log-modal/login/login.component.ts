import { afterNextRender, Component, EventEmitter, Output } from '@angular/core';
import { LoginModalService } from '../../../services/login-modal.service';
import { LoginService } from '../../../services/login.service';

@Component({
	selector: 'app-login',
	standalone: true,
	imports: [],
	templateUrl: './login.component.html',
	styleUrl: './login.component.css',
})
export class LoginComponent {
	@Output() oauth2: EventEmitter<string> = new EventEmitter<string>();
	private fase: number = 0;
	constructor(
		private modalService: LoginModalService,
		private loginService: LoginService,
	) {
		afterNextRender(() => {
			document.addEventListener('keydown', (e: KeyboardEvent) => {
				if (e.key === 'Enter') {
					this.fase === 0 ? this.compruebaCorreo() : this.compruebaPassword();
					this.compruebaCorreo();
				}
				if (e.key === 'Escape') {
					this.close();
				}
			});
		});
	}

	close() {
		this.modalService.hide();
	}

	async compruebaCorreo() {
		const message: HTMLDivElement = document.getElementById('message') as HTMLDivElement;
		message.innerHTML = '';
		const correo: string = (document.getElementById('correo') as HTMLInputElement).value;
		if (correo.length == 0) {
			const mex: HTMLParagraphElement = document.createElement('p');
			mex.textContent = 'El correo electrónico no puede estar vació!!!';
			message.appendChild(mex);
			return;
		}

		if (!this.compruebaEmail(correo)) {
			const mex: HTMLParagraphElement = document.createElement('p');
			mex.textContent = 'El correo electrónico no tiene un formato valido!!!';
			message.appendChild(mex);
			return;
		}
		if ((await this.loginService.compruebaCorreo(correo)) === true) {
			this.fase = 1;
			const content: HTMLDivElement = document.getElementById('content') as HTMLDivElement;
			content.innerHTML = `
				<br />
				<div id="message3" class="text-red-600 ml-4 mr-4"></div>
				<span class="m-4 rounded-lg p-1">Contraseña:</span>
				<br />
				<input type="password" id="password" class="m-4 rounded-lg border border-black p-1" placeholder="Password" />
				<br />
				<button id="nextButton" class="cursor-pointer m-4 rounded-lg text-black border border-black bg-green-300 p-1" (click)="compruebaCorreo()">Siguiente</button>
				<button id="cancelButton" class="cursor-pointer m-4 rounded-lg text-black border border-black bg-red-300 p-1" (click)="close()">Cancelar</button>
			`;
			const nextButton = document.getElementById('nextButton') as HTMLButtonElement;
			const cancelButton = document.getElementById('cancelButton') as HTMLButtonElement;

			if (nextButton) {
				nextButton.addEventListener('click', () => this.compruebaPassword());
			}

			if (cancelButton) {
				cancelButton.addEventListener('click', () => this.close());
			}

			document.removeEventListener('keydown', (e: KeyboardEvent) => {
				if (e.key === 'Enter') {
					this.compruebaCorreo();
				}
			});

			document.addEventListener('keydown', (e: KeyboardEvent) => {
				if (e.key === 'Enter') {
					this.compruebaPassword();
				}
			});
		} else {
			const mex: HTMLParagraphElement = document.createElement('p');
			mex.textContent = 'Este correo no está registrado!!!';
			message.appendChild(mex);
		}
	}

	async compruebaPassword() {
		const message3: HTMLDivElement = document.getElementById('message3') as HTMLDivElement;
		message3.innerHTML = '';
		if (await this.loginService.compruebaPassword((document.getElementById('password') as HTMLInputElement).value)) {
			console.log('Correct password');
			this.close();
		} else {
			const mex: HTMLParagraphElement = document.createElement('p');
			mex.textContent = 'La contraseña no es correcta!!!';
			message3.appendChild(mex);
			return;
		}
	}

	compruebaEmail(email: string): boolean {
		const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
		return emailRegex.test(email);
	}
}
