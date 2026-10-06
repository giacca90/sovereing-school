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
	private idUsuario: number | null = null;
	public usuario: Usuario | null = null;
	USER_KEY = makeStateKey<Usuario>('usuario');

	constructor(
		private readonly http: HttpClient,
		@Inject(PLATFORM_ID) private readonly platformId: Object,
	) {}

	/**
	 * Carga la información del usuario desde TransferState (para SSR).
	 * @returns {Promise<void>} Promesa que se resuelve cuando el usuario ha sido cargado.
	 */
	async cargarUsuarioDesdeTransferState(): Promise<void> {
		if (isPlatformBrowser(this.platformId)) {
			const rawState = (globalThis.window as any)['TRANSFER_STATE'] || {};
			if (rawState['usuario']) {
				this.usuario = rawState['usuario'];
				delete rawState['usuario'];
			} else {
				this.usuario = null;
			}
		}

		if (isPlatformServer(this.platformId)) {
			this.usuario = (globalThis as any).ssrUsuario || null;
		}
	}

	/**
	 * Obtiene la URL base del API de login.
	 * @returns {string} URL base del API.
	 */
	get apiUrl(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			const url = (globalThis.window as any).__env.BACK_BASE ?? '';
			return url + '/login/';
		}
		return '';
	}

	/**
	 * Obtiene la URL del endpoint de login para SSR.
	 * @returns {string} URL del endpoint de login SSR.
	 */
	get loginSSRUrl(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			const url = (globalThis.window as any).__env.FRONTURL ?? '';
			return url + '/ssr-login';
		}
		return '';
	}

	/**
	 * Obtiene la URL del endpoint de logout para SSR.
	 * @returns {string} URL del endpoint de logout SSR.
	 */
	get logoutSSRUrl(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			const url = (globalThis.window as any).__env.FRONTURL ?? '';
			return url + '/ssr-logout';
		}
		return '';
	}

	/**
	 * Comprueba si un correo electrónico ya está registrado.
	 * @param {string} correo - Correo electrónico a comprobar.
	 * @returns {Promise<boolean>} Promesa que se resuelve a true si el correo está registrado, false en caso contrario.
	 */
	async compruebaCorreo(correo: string): Promise<boolean> {
		return new Promise((resolve, reject) => {
			const sub = this.http.get<number>(`${this.apiUrl}${correo}`, { observe: 'response' }).subscribe({
				next: (response: HttpResponse<number>) => {
					if (response.ok) {
						if (response.body == 0) {
							resolve(false);
							sub.unsubscribe();
						} else {
							this.idUsuario = response.body;
							resolve(true);
							sub.unsubscribe();
						}
					} else {
						console.error('Error en comprobar el correo: ' + response.status);
						resolve(false);
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

	/**
	 * Comprueba la contraseña de un usuario.
	 * @param {string} password - Contraseña a comprobar.
	 * @returns {Promise<boolean>} Promesa que se resuelve a true si la contraseña es correcta, false en caso contrario.
	 */
	async compruebaPassword(password: string): Promise<boolean> {
		return new Promise((resolve) => {
			const sub = this.http.get<Auth>(this.apiUrl + this.idUsuario + '/' + password, { observe: 'response', withCredentials: true }).subscribe({
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

	/**
	 * Refresca el token de acceso del usuario.
	 * @returns {Observable<string | null>} Observable con el nuevo token o null si falla.
	 */
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

	/**
	 * Inicia sesión con un token existente.
	 * @param {string} token - Token de acceso.
	 */
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

	/**
	 * Cierra la sesión del usuario.
	 */
	logout(): void {
		this.usuario = null;
		this.idUsuario = null;
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

	/**
	 * Notifica al servidor SSR que el usuario ha iniciado sesión.
	 * @param {string} token - Token de acceso del usuario.
	 */
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
