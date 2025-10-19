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
		private readonly http: HttpClient,
		@Inject(PLATFORM_ID) private readonly platformId: object,
	) {}

	get backURL(): string {
		if (typeof globalThis.window !== 'undefined' && (globalThis.window as any).__env) {
			return (globalThis.window as any).__env.BACK_BASE ?? '';
		}
		return '';
	}

	get backURLStreaming(): string {
		if (typeof globalThis.window !== 'undefined' && (globalThis.window as any).__env) {
			return (globalThis.window as any).__env.BACK_STREAM ?? '';
		}
		return '';
	}

	async getCurso(id_curso: number, fromServer = false): Promise<Curso | null> {
		if (!id_curso) return null;

		const curso = this.cursos.find((c) => c.id_curso === id_curso);
		if (!curso) return null;

		// Si ya tiene clases cargadas y no se fuerza la recarga → devolver directamente
		if (curso.clases_curso && !fromServer) return curso;

		try {
			const response = await firstValueFrom(this.http.get<Curso>(`${this.backURL}/cursos/getCurso/${id_curso}`));

			if (!response) {
				console.error('Respuesta vacía al cargar curso');
				return null;
			}

			// Actualiza solo las propiedades relevantes sin romper la referencia
			Object.assign(curso, {
				clases_curso: response.clases_curso?.sort((a, b) => a.posicion_clase - b.posicion_clase),
				descriccion_larga: response.descriccion_larga,
				fecha_publicacion_curso: response.fecha_publicacion_curso,
				planes_curso: response.planes_curso,
				precio_curso: response.precio_curso,
			});

			curso.clases_curso?.forEach((clase) => (clase.curso_clase = curso.id_curso));

			return curso;
		} catch (error) {
			console.error('Error en cargar curso:', error);
			return null;
		}
	}

	updateCurso(curso: Curso | null): Observable<Curso> {
		if (curso === null) {
			console.error('El curso no existe!!!');
			throw new Error('El curso no existe!!!');
		}
		return this.http.put<Curso>(`${this.backURL}/cursos/update`, curso, { observe: 'response', responseType: 'json' }).pipe(
			map((response: HttpResponse<Curso>) => {
				if (response.ok && response.body) {
					const old = this.cursos.find((c) => c.id_curso === curso.id_curso);
					if (!old) {
						this.cursos.push(response.body);
					}
					return response.body;
				} else {
					console.error('Respuesta del back: ' + response.body);
					throw new Error('Respuesta del back: ' + response.body);
				}
			}),
			catchError((e: Error) => {
				console.error('Error en actualizar el curso: ' + e.message);
				throw e;
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
					this.cursos = this.cursos.filter((c) => c.id_curso !== curso.id_curso);
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
