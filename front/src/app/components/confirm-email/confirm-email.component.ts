import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { Auth } from '../../models/Auth';
import { LoginService } from '../../services/login.service';

@Component({
	selector: 'app-confirm-email',
	imports: [],
	templateUrl: './confirm-email.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './confirm-email.component.css',
})
export class ConfirmEmailComponent implements OnInit, OnDestroy {
	private token: string | null = null;
	mensajeText: string = 'Confirmando tu correo electrónico...';
	mensaje2Text: string = 'Espera un momento...';
	isError: boolean = false;
	private sub?: Subscription;

	/**
	 * Constructor del componente.
	 * @param {ActivatedRoute} route - Ruta activada.
	 * @param {Router} router - Router de Angular.
	 * @param {HttpClient} http - Cliente HTTP.
	 * @param {LoginService} loginService - Servicio de autenticación.
	 */
	constructor(
		private readonly route: ActivatedRoute,
		private readonly router: Router,
		private readonly http: HttpClient,
		private readonly loginService: LoginService,
	) {}

	/**
	 * Limpieza de recursos al destruir el componente.
	 */
	ngOnDestroy(): void {
		this.sub?.unsubscribe();
	}

	/**
	 * Obtiene la URL base del backend desde el entorno.
	 * @returns {string} URL base del backend.
	 */
	get backURL(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			return (globalThis.window as any).__env.BACK_BASE ?? '';
		}
		return '';
	}

	/**
	 * Inicialización del componente.
	 */
	ngOnInit(): void {
		this.token = this.route.snapshot.queryParams['token'] || null;
		if (this.token === null) {
			this.router.navigate(['/']);
			return;
		}
		this.sub = this.http.post<Auth>(this.backURL + '/usuario/confirmation', this.token, { observe: 'response', responseType: 'text' as 'json', withCredentials: true }).subscribe({
			next: (response: HttpResponse<Auth>) => {
				if (response.ok && response.body) {
					this.loginService.usuario = response.body.usuario;
					localStorage.setItem('Token', response.body.accessToken);
					this.mensajeText = 'Tu correo electrónico ha sido confirmado!!!';
					this.mensaje2Text = 'Vas a ser redirigido en un momento...';
					this.isError = false;
					setTimeout(() => {
						this.router.navigate(['/']).then(() => {
							globalThis.window.location.reload();
						});
					}, 1500);
				}
			},
			error: (error: HttpErrorResponse) => {
				this.isError = true;
				this.mensajeText = 'Ha habido un error al confirmar tu correo electrónico:';
				this.mensaje2Text = error.error;
				setTimeout(() => {
					this.router.navigate(['/']).then(() => {
						globalThis.window.location.reload();
					});
				}, 3000);
			},
		});
	}
}
