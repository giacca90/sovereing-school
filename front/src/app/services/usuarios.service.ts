import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { Curso } from '../models/Curso';
import { Usuario } from '../models/Usuario';
import { LoginService } from './login.service';

@Injectable({
	providedIn: 'root',
})
export class UsuariosService {
	public profes: Usuario[] = [];
	constructor(
		private readonly http: HttpClient,
		private readonly loginService: LoginService,
	) {}

	get apiUrl(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			const url = (globalThis.window as any).__env.BACK_BASE ?? '';
			return url + '/usuario/';
		}
		return '';
	}

	getUsuario(idUsuario: number) {
		const sub = this.http.get<Usuario>(this.apiUrl + idUsuario, { observe: 'response' }).subscribe({
			next: (response: HttpResponse<Usuario>) => {
				if (response.ok && response.body) {
					sub.unsubscribe();
					return response.body;
				}
				return null;
			},
			error: (e: Error) => {
				sub.unsubscribe();
				console.error(e.message);
				return null;
			},
		});
	}

	getNombreProfe(id: number): string | undefined {
		return this.profes.find((profe: Usuario) => profe.idUsuario === id)?.nombreUsuario.toString();
	}

	save(formData: FormData): Observable<string[] | null> {
		return this.http.post<string[]>(this.apiUrl + 'subeFotos', formData, { observe: 'response' }).pipe(
			map((response: HttpResponse<string[]>) => {
				if (response.status === 200) {
					return response.body;
				}
				return null;
			}),
			catchError((e: Error) => {
				console.error('Error en guardar las fotos: ' + e.message);
				return of(null);
			}),
		);
	}

	actualizaUsuario(temp: Usuario): Observable<boolean> {
		return this.http.put<string>(this.apiUrl + 'edit', temp, { observe: 'response', responseType: 'text' as 'json' }).pipe(
			map((response: HttpResponse<string>) => {
				if (response.status === 200) {
					return true;
				}
				return false;
			}),
			catchError((e: Error) => {
				console.error('Error en actualizar el usuario: ' + e.message);
				return of(false);
			}),
		);
	}

	getAllUsuarios(): Observable<Usuario[] | null> {
		return this.http.get<Usuario[]>(this.apiUrl + 'getAll', { observe: 'response' }).pipe(
			map((response: HttpResponse<Usuario[]>) => {
				if (response.status === 200) {
					return response.body;
				}
				return [];
			}),
			catchError((e: Error) => {
				console.error('Error en obtener todos los usuarios: ' + e.message);
				return of([]);
			}),
		);
	}

	eliminaUsuario(usuario: Usuario): Observable<boolean> {
		return this.http.delete<string>(this.apiUrl + 'delete/' + usuario.idUsuario, { observe: 'response', responseType: 'text' as 'json' }).pipe(
			map((response: HttpResponse<string>) => {
				if (response.ok) {
					this.getAllUsuarios();
					return true;
				}
				return false;
			}),
			catchError((e: Error) => {
				console.error('Error en eliminar el usuario: ' + e.message);
				return of(false);
			}),
		);
	}

	cursoComprado(curso: Curso): Observable<boolean> {
		const usuario = structuredClone(this.loginService.usuario);
		if (!usuario) {
			console.error('Usuario no inicializado');
			return of(false);
		}

		usuario.cursosUsuario ??= [];

		// Si el curso ya está comprado
		if (usuario.cursosUsuario.some((cur: Curso) => cur.idCurso === curso.idCurso)) {
			return of(true);
		}

		// Si no estaba comprado → añadir y enviar al backend
		usuario.cursosUsuario.push(curso);

		const cursosUsuario: { idUsuario: number; idsCursos: number[] } = { idUsuario: usuario.idUsuario, idsCursos: usuario.cursosUsuario.map((cur: Curso) => cur.idCurso) };

		return this.http
			.put<string>(this.apiUrl + 'cursos', cursosUsuario, {
				observe: 'response',
				responseType: 'text' as 'json',
				withCredentials: true,
			})
			.pipe(
				map((response: HttpResponse<string>) => {
					if (response.status === 200) {
						this.loginService.usuario = usuario;
						return true;
					}
					return false;
				}),
				catchError((e: any) => {
					console.error('Error en actualizar el usuario:', e.message);
					return of(false);
				}),
			);
	}
}
