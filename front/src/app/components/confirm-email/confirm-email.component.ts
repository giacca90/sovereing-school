import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Auth } from '../../models/Auth';
import { LoginService } from '../../services/login.service';

@Component({
	selector: 'app-confirm-email',
	imports: [],
	templateUrl: './confirm-email.component.html',
	styleUrl: './confirm-email.component.css',
})
export class ConfirmEmailComponent implements OnInit {
	private token: string | null = null;
	constructor(
		private readonly route: ActivatedRoute,
		private readonly router: Router,
		private readonly http: HttpClient,
		private readonly loginService: LoginService,
	) {}

	get backURL(): string {
		if (typeof globalThis.window !== 'undefined' && (globalThis.window as any).__env) {
			return (globalThis.window as any).__env.BACK_BASE ?? '';
		}
		return '';
	}

	ngOnInit(): void {
		this.route.queryParams.subscribe((qparams) => {
			this.token = qparams['token'] || null;
		});
		if (this.token === null) {
			this.router.navigate(['/']);
			return;
		}
		this.http.post<Auth>(this.backURL + '/usuario/confirmation', this.token, { observe: 'response', responseType: 'text' as 'json', withCredentials: true }).subscribe({
			next: (response: HttpResponse<Auth>) => {
				if (response.ok && response.body) {
					this.loginService.usuario = response.body.usuario;
					localStorage.setItem('Token', response.body.accessToken);
					const mensaje: HTMLParagraphElement = document.getElementById('mensaje') as HTMLParagraphElement;
					mensaje.innerHTML = 'Tu correo electrónico ha sido confirmado!!!';
					const mensaje2: HTMLParagraphElement = document.getElementById('mensaje2') as HTMLParagraphElement;
					mensaje2.innerHTML = 'Vas a ser redirigido en un momento...';
					setTimeout(() => {
						this.router.navigate(['/']).then(() => {
							globalThis.window.location.reload();
						});
					}, 1500);
				}
			},
			error: (error: HttpErrorResponse) => {
				const mensaje: HTMLParagraphElement = document.getElementById('mensaje') as HTMLParagraphElement;
				mensaje.classList.add('text-red-500');
				mensaje.innerHTML = 'Ha habido un error al confirmar tu correo electrónico:';
				const mensaje2: HTMLParagraphElement = document.getElementById('mensaje2') as HTMLParagraphElement;
				mensaje2.classList.add('text-red-500');
				mensaje2.innerHTML = error.error;
				setTimeout(() => {
					this.router.navigate(['/']).then(() => {
						globalThis.window.location.reload();
					});
				}, 3000);
			},
		});
	}
}
