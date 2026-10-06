import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, ElementRef, Inject, OnDestroy, PLATFORM_ID, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { Auth } from '../../models/Auth';
import { LoginModalService } from '../../services/login-modal.service';
import { LoginService } from '../../services/login.service';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';

@Component({
	selector: 'app-log-modal',
	standalone: true,
	templateUrl: './log-modal.component.html',
	styleUrl: './log-modal.component.css',
	changeDetection: ChangeDetectionStrategy.Eager,
	imports: [LoginComponent, RegisterComponent],
})
// Ahora el padre captura los keydown y los pasa a los hijos
export class LogModalComponent implements AfterViewInit, OnDestroy {
	isLoginHidden: boolean = false;
	backBase = '';

	// ✅ Subject para propagar eventos de teclado
	keyEvents$ = new Subject<KeyboardEvent>();

	@ViewChild('modal') modalEl!: ElementRef<HTMLDivElement>;

	/**
	 * Constructor del componente.
	 * @param {LoginModalService} modalService - Servicio para controlar la visibilidad del modal.
	 * @param {LoginService} loginService - Servicio para manejar la autenticación.
	 * @param {Router} router - Router de Angular.
	 * @param {Object} platformId - ID de la plataforma (browser/server).
	 */
	constructor(
		private readonly modalService: LoginModalService,
		private readonly loginService: LoginService,
		private readonly router: Router,
		@Inject(PLATFORM_ID) private readonly platformId: Object,
	) {}

	/**
	 * Inicialización de la vista.
	 */
	ngAfterViewInit(): void {
		if (isPlatformBrowser(this.platformId)) {
			this.backBase = (globalThis.window as any).__env?.BACK_BASE ?? '';
			// ✅ Aseguramos que el modal recibe foco para captar teclas
			setTimeout(() => this.modalEl?.nativeElement?.focus(), 0);
		}
	}

	/**
	 * Limpieza de recursos al destruir el componente.
	 */
	ngOnDestroy(): void {
		// ✅ cerramos el Subject para evitar fugas
		this.keyEvents$.complete();
	}

	// ✅ método que captura las teclas y las reenvía a los hijos
	/**
	 * Maneja los eventos de teclado para propagarlos a los hijos.
	 * @param {KeyboardEvent} event - Evento de teclado.
	 */
	onKeyDown(event: KeyboardEvent) {
		this.keyEvents$.next(event);
	}

	/**
	 * Cambia a la vista de inicio de sesión.
	 */
	clickLogin() {
		this.isLoginHidden = false;
	}

	/**
	 * Cambia a la vista de registro.
	 */
	clickRegister() {
		this.isLoginHidden = true;
	}

	/**
	 * Cierra el modal.
	 */
	close() {
		alert('close externo');
		this.modalService.hide();
	}

	/**
	 * Inicia sesión con un proveedor OAuth2.
	 * @param {string} provider - Nombre del proveedor (google, github).
	 */
	oauth2LoginWith(provider: string) {
		const width = 600;
		const height = 700;
		const left = (window.innerWidth - width) / 2 + window.screenX;
		const top = (window.innerHeight - height) / 2 + window.screenY;
		if (provider === 'google') {
			window.open(this.backBase + '/oauth2/authorization/google', '_blank', `width=${width},height=${height},top=${top},left=${left}`);
		} else if (provider === 'github') {
			window.open(this.backBase + '/oauth2/authorization/github', '_blank', `width=${width},height=${height},top=${top},left=${left}`);
		}

		const messageListener = (event: MessageEvent) => {
			if (event.origin !== this.backBase) {
				console.error('Invalid origin: ', event.origin);
				return;
			}

			const authResponse: Auth = event.data;
			// Guarda el token en localStorage
			localStorage.setItem('Token', authResponse.accessToken);
			// Hacemos la llamada para que el backend setee la cookie en el browser
			this.loginService.loginWithToken(localStorage.getItem('Token') ?? '');
			this.loginService.usuario = authResponse.usuario;

			// Avisamos al SSR de que estamos logueados
			this.loginService.loginSSR(authResponse.accessToken);

			// Puedes emitir un evento o redirigir
			this.router.navigate(['/']);
			this.modalService.hide();
			// Limpia el listener
			window.removeEventListener('message', messageListener);
		};

		window.addEventListener('message', messageListener);
	}
}
