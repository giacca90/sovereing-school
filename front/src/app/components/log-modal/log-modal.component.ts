import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, ElementRef, Inject, OnDestroy, PLATFORM_ID, ViewChild } from '@angular/core';
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
	imports: [LoginComponent, RegisterComponent],
})
// Ahora el padre captura los keydown y los pasa a los hijos
export class LogModalComponent implements AfterViewInit, OnDestroy {
	login: HTMLButtonElement | null = null;
	register: HTMLButtonElement | null = null;
	isLoginHidden: boolean = false;
	backBase = '';

	// ✅ Subject para propagar eventos de teclado
	keyEvents$ = new Subject<KeyboardEvent>();

	@ViewChild('modal') modalEl!: ElementRef<HTMLDivElement>;

	constructor(
		private modalService: LoginModalService,
		private loginService: LoginService,
		private router: Router,
		@Inject(PLATFORM_ID) private platformId: Object,
	) {}

	ngAfterViewInit(): void {
		this.login = document.getElementById('login') as HTMLButtonElement;
		this.register = document.getElementById('register') as HTMLButtonElement;
		if (isPlatformBrowser(this.platformId)) {
			this.backBase = (window as any).__env?.BACK_BASE ?? '';
			// ✅ Aseguramos que el modal recibe foco para captar teclas
			setTimeout(() => this.modalEl?.nativeElement?.focus(), 0);
		}
	}

	ngOnDestroy(): void {
		// ✅ cerramos el Subject para evitar fugas
		this.keyEvents$.complete();
	}

	// ✅ método que captura las teclas y las reenvía a los hijos
	onKeyDown(event: KeyboardEvent) {
		this.keyEvents$.next(event);
	}

	clickLogin() {
		this.isLoginHidden = false;
		const isDarkMode = document.documentElement.classList.contains('dark');
		if (this.register?.classList.contains(isDarkMode ? 'dark:border-b-black' : 'bg-white')) {
			this.register.classList.remove(isDarkMode ? 'dark:border-b-black' : 'bg-white');
		}
		if (this.login?.classList.contains(isDarkMode ? 'dark:border-b-black' : 'bg-white') == false) {
			this.login.classList.add(isDarkMode ? 'dark:border-b-black' : 'bg-white');
		}
	}

	clickRegister() {
		this.isLoginHidden = true;
		const isDarkMode = document.documentElement.classList.contains('dark');

		if (this.login?.classList.contains(isDarkMode ? 'dark:border-b-black' : 'bg-white')) {
			this.login.classList.remove(isDarkMode ? 'dark:border-b-black' : 'bg-white');
		}
		if (this.register?.classList.contains(isDarkMode ? 'dark:border-b-black' : 'bg-white') == false) {
			this.register.classList.add(isDarkMode ? 'dark:border-b-black' : 'bg-white');
		}
	}

	close() {
		alert('close externo');
		this.modalService.hide();
	}

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
			//localStorage.setItem('Usuario', JSON.stringify(authResponse.usuario));
			this.loginService.usuario = authResponse.usuario;

			// Puedes emitir un evento o redirigir
			this.router.navigate(['/']);
			this.modalService.hide();
			// Limpia el listener
			window.removeEventListener('message', messageListener);
		};

		window.addEventListener('message', messageListener);
	}
}
