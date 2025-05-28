import { HttpClient, HttpResponse } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { catchError, firstValueFrom, map, Observable, of } from 'rxjs';
import { Curso } from '../models/Curso';
import { Usuario } from '../models/Usuario';

@Injectable({
	providedIn: 'root',
})
export class CursosService {
	public cursos: Curso[] = [];

	constructor(
		private http: HttpClient,
		@Inject(PLATFORM_ID) private platformId: object,
	) {}

	get backURL(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			return (window as any).__env.BACK_BASE ?? '';
		}
		return '';
	}

	get backURLStreaming(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			return (window as any).__env.BACK_STREAM ?? '';
		}
		return '';
	}

	async getCurso(id_curso: number, fromServer?: boolean): Promise<Curso | null> {
		for (let i = 0; i < this.cursos.length; i++) {
			if (this.cursos[i].id_curso == id_curso) {
				if (this.cursos[i].clases_curso === undefined || fromServer) {
					try {
						const response = await firstValueFrom(this.http.get<Curso>(`${this.backURL}/cursos/getCurso/${id_curso}`));
						this.cursos[i].clases_curso = response.clases_curso?.sort((a, b) => a.posicion_clase - b.posicion_clase);
						this.cursos[i].descriccion_larga = response.descriccion_larga;
						this.cursos[i].fecha_publicacion_curso = response.fecha_publicacion_curso;
						this.cursos[i].planes_curso = response.planes_curso;
						this.cursos[i].precio_curso = response.precio_curso;
						return this.cursos[i];
					} catch (error) {
						console.error('Error en cargar curso:', error);
						return null;
					}
				} else return this.cursos[i];
			}
		}
		return null;
	}

	updateCurso(curso: Curso | null): Observable<boolean> {
		if (curso === null) {
			console.error('El curso no existe!!!');
			return of(false);
		}
		return this.http.put<string>(`${this.backURL}/cursos/update`, curso, { observe: 'response', responseType: 'text' as 'json' }).pipe(
			map((response) => {
				if (response.ok) {
					return true;
				} else {
					console.error('Respuesta del back: ' + response.body);
					return false;
				}
			}),
			catchError((e: Error) => {
				console.error('Error en actualizar el curso: ' + e.message);
				return of(false);
			}),
		);
	}

	addImagenCurso(target: FormData): Observable<string | null> {
		return this.http.post<string[]>(this.backURL + '/usuario/subeFotos', target, { observe: 'response' }).pipe(
			map((response: HttpResponse<string[]>) => {
				if (response.ok && response.body) {
					return response.body[0];
				}
				return null;
			}),
			catchError((e: Error) => {
				console.error('Error en subir la imagen: ' + e.message);
				return of(null);
			}),
		);
	}

	getCursosProfe(profe: Usuario) {
		const cursosProfe: Curso[] = [];
		this.cursos.forEach((curso) => {
			curso.profesores_curso.forEach((profe2) => {
				if (profe2.id_usuario === profe.id_usuario) {
					cursosProfe.push(curso);
				}
			});
		});
		return cursosProfe;
	}

	deleteCurso(curso: Curso): Observable<boolean> {
		return this.http.delete<string>(this.backURL + '/cursos/delete/' + curso.id_curso, { observe: 'response', responseType: 'text' as 'json' }).pipe(
			map((response: HttpResponse<string>) => {
				if (response.ok) {
					this.cursos = this.cursos.slice(
						this.cursos.findIndex((curso2) => curso2.id_curso === curso.id_curso),
						1,
					);
					return true;
				}
				return false;
			}),
			catchError((e: Error) => {
				console.error('Error en eliminar el curso: ' + e.message);
				return of(false);
			}),
		);
	}

	getStatusCurso(id_curso: number): Observable<number | boolean> {
		return this.http.get<number>(this.backURLStreaming + '/status/' + id_curso, { observe: 'response' }).pipe(
			map((response: HttpResponse<number>) => {
				if (response.ok && response.body) {
					return response.body;
				}
				return false;
			}),
			catchError((e: Error) => {
				console.error('Error al obtener el estado del curso:', e.message);
				return of(false);
			}),
		);
	}

	getAllCursos() {
		return this.http.get<Curso[]>(this.backURL + '/cursos/getAll', { observe: 'response' }).pipe(
			map((response: HttpResponse<Curso[]>) => {
				if (response.ok && response.body) {
					this.cursos = response.body;
					this.cursos.forEach((curso) => {
						curso.clases_curso = curso.clases_curso?.sort((a, b) => a.posicion_clase - b.posicion_clase);
					});
					return this.cursos;
				}
				return [];
			}),
			catchError((e: Error) => {
				console.error('Error en obtener todos los cursos: ' + e.message);
				return of([]);
			}),
		);
	}
}
