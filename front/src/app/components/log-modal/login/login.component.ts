import { Component, ElementRef, input, OnDestroy, OnInit, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { LoginModalService } from '../../../services/login-modal.service';
import { LoginService } from '../../../services/login.service';

@Component({
	selector: 'app-login',
	imports: [FormsModule],
	templateUrl: './login.component.html',
	styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit, OnDestroy {
	oauth2 = output<string>();
	keyEvents = input<Subject<KeyboardEvent>>();

	fase = signal<number>(0);
	correo: string = '';
	password: string = '';
	mensajeError = signal<string>('');
	private keySub?: Subscription;

	correoInput = viewChild<ElementRef<HTMLInputElement>>('correoInput');
	passwordInput = viewChild<ElementRef<HTMLInputElement>>('passwordInput');

	/**
	 * Constructor del componente.
	 * @param {LoginModalService} modalService - Servicio para controlar la visibilidad del modal.
	 * @param {LoginService} loginService - Servicio para manejar la autenticación.
	 */
	constructor(
		private readonly modalService: LoginModalService,
		private readonly loginService: LoginService,
	) {}

	/**
	 * Inicialización del componente.
	 */
	ngOnInit(): void {
		if (this.keyEvents()) {
			this.keySub = this.keyEvents()!.subscribe((e: KeyboardEvent) => {
				if (e.key === 'Enter') {
					this.fase() === 0 ? this.compruebaCorreo() : this.compruebaPassword();
				} else if (e.key === 'Escape' || e.key === 'Esc' || e.key === 'Delete') {
					this.close();
				}
			});
		}

		// Ponemos el foco inicial en el campo correo
		requestAnimationFrame(() => {
			this.correoInput()?.nativeElement?.focus();
		});
	}

	/**
	 * Limpieza de recursos al destruir el componente.
	 */
	ngOnDestroy(): void {
		this.keySub?.unsubscribe();
	}

	/**
	 * Cierra el modal de inicio de sesión.
	 */
	close() {
		this.keySub?.unsubscribe();
		this.modalService.hide();
	}

	/**
	 * Comprueba la validez del correo electrónico proporcionado.
	 */
	async compruebaCorreo() {
		this.mensajeError.set('');
		if (this.correo.length == 0) {
			this.mensajeError.set('El correo no puede estar vacío!!!');
			return;
		}

		if (!this.compruebaEmail(this.correo)) {
			this.mensajeError.set('El formato del correo no es válido!!!');
			return;
		}
		if ((await this.loginService.compruebaCorreo(this.correo)) === true) {
			this.fase.set(1);
			requestAnimationFrame(() => {
				this.passwordInput()?.nativeElement?.focus();
			});
		} else {
			this.mensajeError.set('El correo no existe!!!');
		}
	}

	/**
	 * Comprueba la validez de la contraseña proporcionada.
	 */
	async compruebaPassword() {
		this.mensajeError.set('');
		if (this.password.length == 0) {
			this.mensajeError.set('La contraseña no puede estar vacía!!!');
			return;
		}
		const success = await this.loginService.compruebaPassword(this.password);
		if (success) {
			this.close();
			//globalThis.window.location.reload();
		} else {
			this.mensajeError.set('La contraseña no es correcta!!!');
		}
	}

	/**
	 * Verifica si el formato del correo es válido.
	 * @param {string} email - Correo a verificar.
	 * @returns {boolean} True si el formato es válido, false en caso contrario.
	 */
	compruebaEmail(email: string): boolean {
		const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
		return emailRegex.test(email);
	}
}
