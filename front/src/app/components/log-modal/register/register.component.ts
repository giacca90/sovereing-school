import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { NuevoUsuario } from '../../../models/NuevoUsuario';
import { LoginModalService } from '../../../services/login-modal.service';
import { LoginService } from '../../../services/login.service';
import { RegisterService } from '../../../services/register.service';

@Component({
	selector: 'app-register',
	standalone: true,
	imports: [],
	templateUrl: './register.component.html',
	styleUrl: './register.component.css',
})
export class RegisterComponent implements OnInit, OnDestroy {
	@Output() oauth2: EventEmitter<string> = new EventEmitter<string>();
	@Input() keyEvents!: Subject<KeyboardEvent>;

	private nuevoUsuario: NuevoUsuario = new NuevoUsuario();
	private fase: number = 0;
	private keySub?: Subscription;

	constructor(
		private readonly modalService: LoginModalService,
		private readonly loginService: LoginService,
		private readonly registerService: RegisterService,
	) {}

	ngOnInit(): void {
		if (this.keyEvents) {
			this.keySub = this.keyEvents.subscribe((e: KeyboardEvent) => {
				if (e.key === 'Enter') {
					this.fase === 0 ? this.compruebaCorreo() : this.fase === 1 ? this.compruebaPassword() : this.close();
				}
				if (e.key === 'Escape' || e.key === 'Esc' || e.key === 'Delete') {
					this.close();
				}
			});
		}

		// Foco inicial en el nombre
		setTimeout(() => {
			document.getElementById('nombre2')?.focus();
		});
	}

	ngOnDestroy(): void {
		this.keySub?.unsubscribe();
		this.nuevoUsuario = new NuevoUsuario();
	}

	close() {
		this.keySub?.unsubscribe();
		this.modalService.hide();
	}

	async compruebaCorreo() {
		const message: HTMLDivElement = document.getElementById('message2') as HTMLDivElement;
		message.innerHTML = '';
		const correo: string = (document.getElementById('correo2') as HTMLInputElement).value;

		if ((document.getElementById('nombre2') as HTMLInputElement).value.length == 0) {
			const mex: HTMLParagraphElement = document.createElement('p');
			mex.textContent = 'El nombre no puede estar vacío.';
			message.appendChild(mex);
			return;
		}
		if (correo.length == 0) {
			const mex: HTMLParagraphElement = document.createElement('p');
			mex.textContent = 'No has puesto el correo!!!';
			message.appendChild(mex);
			return;
		}

		if (!this.compruebaEmail(correo)) {
			const mex: HTMLParagraphElement = document.createElement('p');
			mex.textContent = 'El correo electrónico no tiene un formato válido!!!';
			message.appendChild(mex);
			return;
		}

		if ((await this.loginService.compruebaCorreo(correo)) === false) {
			this.fase = 1;
			this.nuevoUsuario.correo_electronico = correo;
			this.nuevoUsuario.nombre_usuario = (document.getElementById('nombre2') as HTMLInputElement).value;
			const content: HTMLDivElement = document.getElementById('content2') as HTMLDivElement;
			content.innerHTML = `
				<br />
				<div id="message4" class="text-red-600 ml-4 mr-4"></div>
				<span class="m-4 rounded-lg p-1">Contraseña:</span>
				<br />
				<input type="password" id="password" class="m-4 rounded-lg border border-black p-1" placeholder="Contraseña" />
				<br />
				<span class="m-4 rounded-lg p-1">Repite la contraseña:</span>
				<br />
				<input type="password" id="password2" class="m-4 rounded-lg border border-black p-1" placeholder="Repite la contraseña" />
				<br />
				<button id="nextButton2" class="cursor-pointer m-4 rounded-lg border text-black border-black bg-green-300 p-1">Siguiente</button>
				<button id="cancelButton2" class="cursor-pointer m-4 rounded-lg border text-black border-black bg-red-300 p-1">Cancelar</button>
			`;

			const nextButton = document.getElementById('nextButton2') as HTMLButtonElement;
			const cancelButton = document.getElementById('cancelButton2') as HTMLButtonElement;
			document.getElementById('password')?.focus();

			if (nextButton) {
				nextButton.addEventListener('click', () => this.compruebaPassword());
			}

			if (cancelButton) {
				cancelButton.addEventListener('click', () => this.close());
			}
		} else {
			const mex: HTMLParagraphElement = document.createElement('p');
			mex.textContent = 'Este correo ya está registrado!!!';
			message.appendChild(mex);
		}
	}

	compruebaPassword() {
		const message: HTMLDivElement = document.getElementById('message4') as HTMLDivElement;
		message.innerHTML = '';
		const pass: string = (document.getElementById('password') as HTMLInputElement).value;
		const pass2: string = (document.getElementById('password2') as HTMLInputElement).value;
		if (pass.length === 0) {
			message.textContent = 'La contraseña no puede estar vacía';
			return;
		}
		if (pass2.length === 0) {
			message.textContent = 'La contraseña no puede estar vacía';
			return;
		}
		if (pass == pass2) {
			this.nuevoUsuario.password = pass;
			this.nuevoUsuario.fecha_registro_usuario = new Date();
			this.registerService.registrarNuevoUsuario(this.nuevoUsuario).then((resp: boolean) => {
				if (resp) {
					this.fase = 2;
					const content: HTMLDivElement = document.getElementById('content2') as HTMLDivElement;
					content.innerHTML = `
					<br />
					<div class="text-green-700 ml-4 mr-4">
					<p>Te hemos enviado un correo electrónico con el enlace para activar tu cuenta.</p>
					<p>El enlace expirará en 24 horas.</p>
					</div>
					<button id="button" class="cursor-pointer m-4 rounded-lg border text-black border-black bg-green-300 p-1">Cerrar</button>
					`;
				}
				const button: HTMLButtonElement = document.getElementById('button') as HTMLButtonElement;
				if (button) {
					button.addEventListener('click', () => this.close());
				}
			});
		} else {
			message.textContent = 'Las dos contraseñas no coinciden';
		}
	}

	compruebaEmail(email: string): boolean {
		const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
		return emailRegex.test(email);
	}
}
