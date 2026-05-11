import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';
import { LogModalComponent } from './components/log-modal/log-modal.component';
import { SearchComponent } from './components/search/search.component';
import { LoginModalService } from './services/login-modal.service';
import { LoginService } from './services/login.service';

@Component({
	selector: 'app-root',
	standalone: true,
	templateUrl: './app.component.html',
	styleUrl: './app.component.css',
	imports: [RouterOutlet, SearchComponent, LogModalComponent, CommonModule],
})
export class AppComponent implements OnInit, OnDestroy {
	title = 'Sovereign School';
	isModalVisible: boolean = false;
	vistaMenu: boolean = false;
	currentYear: string = new Date().getFullYear().toString();
	private readonly subscription: Subscription = new Subscription();

	/**
	 * Constructor del componente.
	 * @param {LoginModalService} modalService - Servicio para gestionar el modal de login.
	 * @param {LoginService} loginService - Servicio de autenticación.
	 * @param {Router} router - Router de Angular.
	 * @param {ChangeDetectorRef} cdr - Detección de cambios.
	 * @param {Renderer2} renderer - Renderer para manipulación del DOM.
	 */
	constructor(
		private readonly modalService: LoginModalService,
		public loginService: LoginService,
		public router: Router,
		private readonly cdr: ChangeDetectorRef,
		private readonly renderer: Renderer2,
	) {}

	/**
	 * Inicializa el componente y configura los listeners de tema.
	 */
	ngOnInit() {
		// Detecta si está en el navegador
		if (globalThis.window !== undefined) {
			globalThis.window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
				if (!('theme' in localStorage)) {
					if (e.matches) {
						this.renderer.addClass(document.documentElement, 'dark');
					} else {
						this.renderer.removeClass(document.documentElement, 'dark');
					}
				}
			});
		}

		this.subscription.add(
			this.modalService.isVisible$.subscribe((isVisible) => {
				this.isModalVisible = isVisible;
				this.cdr.detectChanges();
			}),
		);
	}

	/**
	 * Abre el modal de inicio de sesión.
	 */
	openModal() {
		this.modalService.show();
	}

	/**
	 * Cierra la sesión del usuario y redirige al inicio.
	 */
	salir() {
		this.vistaMenu = false;
		this.loginService.logout();
		this.router.navigate(['/']);
	}

	/**
	 * Destruye el componente y cancela las suscripciones.
	 */
	ngOnDestroy(): void {
		this.subscription.unsubscribe();
	}

	/**
	 * Cambia el tema de la aplicación (claro/oscuro).
	 */
	changeTheme() {
		const isDark = localStorage.getItem('theme') === 'dark';
		const newTheme = isDark ? 'light' : 'dark';
		localStorage.setItem('theme', newTheme);
		if (newTheme === 'dark') {
			this.renderer.addClass(document.documentElement, 'dark');
		} else {
			this.renderer.removeClass(document.documentElement, 'dark');
		}
	}
}
