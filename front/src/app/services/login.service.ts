import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Inject, Injectable, makeStateKey, PLATFORM_ID } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { Auth } from '../models/Auth';
import { Usuario } from '../models/Usuario';

@Injectable({
	providedIn: 'root',
})
export class LoginService {
	private id_usuario: number | null = null;
	public usuario: Usuario | null = null;
	USER_KEY = makeStateKey<Usuario>('usuario');

	constructor(
		private readonly http: HttpClient,
		@Inject(PLATFORM_ID) private readonly platformId: Object,
	) {}

	async cargarUsuarioDesdeTransferState(): Promise<void> {
		if (isPlatformBrowser(this.platformId)) {
			const rawState = (window as any)['TRANSFER_STATE'] || {};
			if (rawState['usuario']) {
				this.usuario = rawState['usuario'];
				delete rawState['usuario'];
				//console.log('[LoginService] Usuario cargado desde TransferState', this.usuario);
			} else {
				//console.log('[LoginService] Usuario no encontrado en TransferState');
				this.usuario = null;
			}
		}

		if (isPlatformServer(this.platformId)) {
			this.usuario = (globalThis as any).ssrUsuario || null;
		}
	}

	get apiUrl(): string {
		if (typeof globalThis.window !== 'undefined' && (globalThis.window as any).__env) {
			const url = (globalThis.window as any).__env.BACK_BASE ?? '';
			return url + '/login/';
		}
		return '';
	}

	get loginSSRUrl(): string {
		if (typeof globalThis.window !== 'undefined' && (globalThis.window as any).__env) {
			const url = (globalThis.window as any).__env.FRONT_URL ?? '';
			return url + '/ssr-login';
		}
		return '';
	}

	get logoutSSRUrl(): string {
		if (typeof globalThis.window !== 'undefined' && (globalThis.window as any).__env) {
			const url = (globalThis.window as any).__env.FRONT_URL ?? '';
			return url + '/ssr-logout';
		}
		return '';
	}

	async compruebaCorreo(correo: string): Promise<boolean> {
		return new Promise(async (resolve, reject) => {
			const sub = this.http.get<number>(`${this.apiUrl}${correo}`, { observe: 'response' }).subscribe({
				next: (response: HttpResponse<number>) => {
					if (response.ok) {
						if (response.body == 0) {
							resolve(false);
							sub.unsubscribe();
						} else {
							this.id_usuario = response.body;
							resolve(true);
							sub.unsubscribe();
						}
					} else {
						console.error('Error en comprobar el correo: ' + response.status);
						reject(false);
					}
				},
				error: (error: HttpErrorResponse) => {
					console.error('HTTP request failed:', error);
					reject(false);
					sub.unsubscribe();
				},
			});
		});
	}

	async compruebaPassword(password: string): Promise<boolean> {
		return new Promise(async (resolve) => {
			const sub = this.http.get<Auth>(this.apiUrl + this.id_usuario + '/' + password, { observe: 'response', withCredentials: true }).subscribe({
				next: (response: HttpResponse<Auth>) => {
					if (response.ok && response.body) {
						if (!response.body.status && response.body.usuario === null) {
							resolve(false);
							sub.unsubscribe();
							return;
						}
						this.usuario = response.body.usuario;
						// Comprueba si está en el navegador
						localStorage.setItem('Token', response.body.accessToken);
						// Avisamos al SSR de que estamos logueados
						this.loginSSR(response.body.accessToken);

						resolve(true);
						sub.unsubscribe();
					} else {
						console.error('Error en comprobar las password: ' + response.status);
					}
				},
				error: (error: HttpErrorResponse) => {
					console.error('HTTP request failed:', error);
					resolve(false);
					sub.unsubscribe();
				},
			});
		});
	}

	refreshToken(): Observable<string | null> {
		console.log('Refreshing token...');
		return this.http.post<Auth>(this.apiUrl + 'refresh', null, { observe: 'response', withCredentials: true }).pipe(
			map((response: HttpResponse<Auth>) => {
				if (response.ok && response.body) {
					return response.body.accessToken;
				}
				return null;
			}),
			catchError((e: Error) => {
				console.error('Error en refrescar el token: ' + e.message);
				return of(null);
			}),
		);
	}

	loginWithToken(token: string) {
		this.http.post<Usuario>(this.apiUrl + 'loginWithToken', token, { observe: 'response', withCredentials: true }).subscribe({
			next: (response: HttpResponse<Usuario>) => {
				if (response.ok && response.body) {
					this.usuario = response.body;
				}
			},
			error: (error: HttpErrorResponse) => {
				console.error('Error en loginWithToken:', error.message);
			},
		});
	}

	logout(): void {
		this.usuario = null;
		this.id_usuario = null;
		localStorage.clear();
		this.http.get<string>(this.apiUrl + 'logout', { observe: 'response', responseType: 'text' as 'json', withCredentials: true }).subscribe({
			next: (response: HttpResponse<string>) => {
				if (response.status !== 200) {
					console.error('Error en logout: ' + response.status);
					console.error(response.body);
				}
			},
			error: (error: HttpErrorResponse) => {
				console.error('Logout failed:', error);
			},
		});

		// Avisamos al SSR de que hacemos logout
		this.http.get<string>(this.logoutSSRUrl, { observe: 'response', responseType: 'text' as 'json', withCredentials: true }).subscribe({
			next: (response: HttpResponse<string>) => {
				if (response.status !== 200) {
					console.error('Error en avisar al SSR de que hacemos logout: ' + response.status);
					console.error(response.body);
				}
			},
			error: (error: HttpErrorResponse) => {
				console.error('Avisar al SSR de que hacemos logout fallido:', error);
			},
		});
	}

	public loginSSR(token: string) {
		// Avisamos al SSR de que estamos logueados
		this.http
			.post<{ ok: boolean }>(
				this.loginSSRUrl,
				{ token: token }, // enviar el token que recibimos del backend
				{ observe: 'response', withCredentials: true },
			)
			.subscribe({
				next: (resp: HttpResponse<{ ok: boolean }>) => {
					if (resp.status !== 200 || !resp.body?.ok) {
						console.error('Error en avisar al SSR de que estamos logueados: ' + resp.status);
						console.error(resp.body);
					}
				},
				error: (error: HttpErrorResponse) => {
					console.error('Avisar al SSR de que estamos logueados fallido:', error);
				},
			});
	}
}
