import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NuevoUsuario } from '../models/NuevoUsuario';

@Injectable({
	providedIn: 'root',
})
export class RegisterService {
	constructor(private readonly http: HttpClient) {}

	get apiUrl(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			return (globalThis.window as any).__env.BACK_BASE ?? '';
		}
		return '';
	}

	async registrarNuevoUsuario(nuevoUsuario: NuevoUsuario): Promise<boolean> {
		return new Promise((resolve, reject) => {
			const sub = this.http.post<string>(`${this.apiUrl}/usuario/nuevo`, nuevoUsuario, { observe: 'response', responseType: 'text' as 'json' }).subscribe({
				next: (response: HttpResponse<string>) => {
					if (response.status === 200 && response.body) {
						if (response.body === 'Correo enviado con éxito!!!') {
							sub.unsubscribe();
							resolve(true);
						} else {
							alert('Ha habido un error al registrarte. Por favor, inténtalo de nuevo.');
						}
					} else {
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
}
