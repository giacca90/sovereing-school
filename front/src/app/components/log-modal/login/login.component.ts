import { ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { LoginModalService } from '../../../services/login-modal.service';
import { LoginService } from '../../../services/login.service';

@Component({
	selector: 'app-login',
	standalone: true,
	imports: [FormsModule],
	templateUrl: './login.component.html',
	styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit, OnDestroy {
	@Output() oauth2: EventEmitter<string> = new EventEmitter<string>();
	@Input() keyEvents!: Subject<KeyboardEvent>;

	fase: number = 0;
	correo: string = '';
	password: string = '';
	mensajeError: string = '';
	private keySub?: Subscription;

	@ViewChild('correoInput') correoInput!: ElementRef<HTMLInputElement>;
	@ViewChild('passwordInput') passwordInput!: ElementRef<HTMLInputElement>;

	/**
	 * Constructor del componente.
	 * @param {LoginModalService} modalService - Servicio para controlar la visibilidad del modal.
	 * @param {LoginService} loginService - Servicio para manejar la autenticación.
	 * @param {ChangeDetectorRef} cdr - Detección de cambios.
	 */
	constructor(
		private readonly modalService: LoginModalService,
		private readonly loginService: LoginService,
		private readonly cdr: ChangeDetectorRef,
	) {}

	/**
	 * Inicialización del componente.
	 */
	ngOnInit(): void {
		if (this.keyEvents) {
			this.keySub = this.keyEvents.subscribe((e: KeyboardEvent) => {
				if (e.key === 'Enter') {
					this.fase === 0 ? this.compruebaCorreo() : this.compruebaPassword();
				} else if (e.key === 'Escape' || e.key === 'Esc' || e.key === 'Delete') {
					this.close();
				}
			});
		}

		// Ponemos el foco inicial en el campo correo
		setTimeout(() => {
			this.correoInput?.nativeElement?.focus();
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
		this.mensajeError = '';
		if (this.correo.length == 0) {
			this.mensajeError = 'El correo electrónico no puede estar vació!!!';
			return;
		}

		if (!this.compruebaEmail(this.correo)) {
			this.mensajeError = 'El correo electrónico no tiene un formato valido!!!';
			return;
		}
		if ((await this.loginService.compruebaCorreo(this.correo)) === true) {
			this.fase = 1;
			this.cdr.detectChanges();
			setTimeout(() => {
				this.passwordInput?.nativeElement?.focus();
			});
		} else {
			this.mensajeError = 'Este correo no está registrado!!!';
		}
	}

	/**
	 * Comprueba la validez de la contraseña proporcionada.
	 */
	async compruebaPassword() {
		this.mensajeError = '';
		if (this.password.length == 0) {
			this.mensajeError = 'La contraseña no puede estar vacía!!!';
			return;
		}
		const success = await this.loginService.compruebaPassword(this.password);
		if (success) {
			this.close();
			globalThis.window.location.reload();
		} else {
			this.mensajeError = 'La contraseña es incorrecta!!!';
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
