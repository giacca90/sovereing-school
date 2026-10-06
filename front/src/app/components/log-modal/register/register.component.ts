import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { NuevoUsuario } from '../../../models/NuevoUsuario';
import { LoginModalService } from '../../../services/login-modal.service';
import { LoginService } from '../../../services/login.service';
import { RegisterService } from '../../../services/register.service';

@Component({
	selector: 'app-register',
	standalone: true,
	imports: [FormsModule],
	templateUrl: './register.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './register.component.css',
})
export class RegisterComponent implements OnInit, OnDestroy {
	@Output() oauth2: EventEmitter<string> = new EventEmitter<string>();
	@Input() keyEvents!: Subject<KeyboardEvent>;

	@ViewChild('nombreInput') nombreInput!: ElementRef<HTMLInputElement>;
	@ViewChild('passwordInput') passwordInput!: ElementRef<HTMLInputElement>;

	public nuevoUsuario: NuevoUsuario = new NuevoUsuario();
	public fase: number = 0;
	public mensajeError: string = '';
	public passwordConfirm: string = '';
	private keySub?: Subscription;

	/**
	 * Constructor del componente.
	 * @param {LoginModalService} modalService - Servicio para controlar la visibilidad del modal.
	 * @param {LoginService} loginService - Servicio para manejar la autenticación.
	 * @param {RegisterService} registerService - Servicio para manejar el registro.
	 */
	constructor(
		private readonly modalService: LoginModalService,
		private readonly loginService: LoginService,
		private readonly registerService: RegisterService,
	) {}

	/**
	 * Inicialización del componente.
	 */
	ngOnInit(): void {
		if (this.keyEvents) {
			this.keySub = this.keyEvents.subscribe((e: KeyboardEvent) => {
				if (e.key === 'Enter') {
					if (this.fase === 0) {
						this.compruebaCorreo();
					} else if (this.fase === 1) {
						this.compruebaPassword();
					} else {
						this.close();
					}
				}
				if (e.key === 'Escape' || e.key === 'Esc' || e.key === 'Delete') {
					this.close();
				}
			});
		}

		// Foco inicial en el nombre
		setTimeout(() => {
			this.nombreInput?.nativeElement?.focus();
		});
	}

	/**
	 * Limpieza de recursos al destruir el componente.
	 */
	ngOnDestroy(): void {
		this.keySub?.unsubscribe();
		this.nuevoUsuario = new NuevoUsuario();
	}

	/**
	 * Cierra el modal de registro.
	 */
	close() {
		this.keySub?.unsubscribe();
		this.modalService.hide();
	}

	/**
	 * Comprueba la validez del correo electrónico.
	 */
	async compruebaCorreo() {
		this.mensajeError = '';

		if (!this.nuevoUsuario.nombreUsuario || this.nuevoUsuario.nombreUsuario.length === 0) {
			this.mensajeError = 'El nombre no puede estar vacío.';
			return;
		}
		if (!this.nuevoUsuario.correoElectronico || this.nuevoUsuario.correoElectronico.length === 0) {
			this.mensajeError = 'No has puesto el correo!!!';
			return;
		}

		if (!this.compruebaEmail(this.nuevoUsuario.correoElectronico)) {
			this.mensajeError = 'El correo electrónico no tiene un formato válido!!!';
			return;
		}

		if ((await this.loginService.compruebaCorreo(this.nuevoUsuario.correoElectronico)) === false) {
			this.fase = 1;
			setTimeout(() => {
				this.passwordInput?.nativeElement?.focus();
			});
		} else {
			this.mensajeError = 'Este correo ya está registrado!!!';
		}
	}

	/**
	 * Comprueba la validez de la contraseña.
	 */
	compruebaPassword() {
		this.mensajeError = '';
		if (!this.nuevoUsuario.password || this.nuevoUsuario.password.length === 0) {
			this.mensajeError = 'La contraseña no puede estar vacía';
			return;
		}
		if (this.passwordConfirm.length === 0) {
			this.mensajeError = 'La contraseña no puede estar vacía';
			return;
		}
		if (this.nuevoUsuario.password === this.passwordConfirm) {
			this.nuevoUsuario.fechaRegistroUsuario = new Date();
			this.registerService.registrarNuevoUsuario(this.nuevoUsuario).then((resp: boolean) => {
				if (resp) {
					this.fase = 2;
				}
			});
		} else {
			this.mensajeError = 'Las contraseñas no coinciden';
		}
	}

	compruebaEmail(email: string): boolean {
		const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
		return emailRegex.test(email);
	}
}
