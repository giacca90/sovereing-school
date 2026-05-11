import { HttpClient, HttpResponse } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { catchError, firstValueFrom, map, Observable, of } from 'rxjs';
import { Curso } from '../models/Curso';
import { Plan } from '../models/Plan';
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

	/**
	 * Obtiene la URL base del backend desde las variables de entorno.
	 * @returns {string} URL base del backend.
	 */
	get backURL(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			return (globalThis.window as any).__env.BACK_BASE ?? '';
		}
		return '';
	}

	/**
	 * Obtiene la URL base del servicio de streaming desde las variables de entorno.
	 * @returns {string} URL base del streaming.
	 */
	get backURLStreaming(): string {
		if (globalThis.window !== undefined && (globalThis.window as any).__env) {
			return (globalThis.window as any).__env.BACK_STREAM ?? '';
		}
		return '';
	}

	/**
	 * Obtiene un curso por su ID, con opción de forzar la carga desde el servidor.
	 * @param {number} idCurso - ID del curso a obtener.
	 * @param {boolean} fromServer - Si es true, fuerza la petición al servidor.
	 * @returns {Promise<Curso | null>} Promesa con el curso encontrado o null.
	 */
	async getCurso(idCurso: number, fromServer = false): Promise<Curso | null> {
		if (!idCurso) return null;

		const curso = this.cursos.find((c) => c.idCurso === idCurso);
		if (!curso) return null;

		// Si ya tiene clases cargadas y no se fuerza la recarga → devolver directamente
		if (curso.clasesCurso && !fromServer) return curso;

		try {
			const response: Curso = await firstValueFrom(this.http.get<Curso>(`${this.backURL}/cursos/getCurso/${idCurso}`));

			if (!response) {
				console.error('Respuesta vacía al cargar curso');
				return null;
			}

			// Actualiza solo las propiedades relevantes sin romper la referencia
			Object.assign(curso, {
				clasesCurso: response.clasesCurso?.sort((a, b) => a.posicionClase - b.posicionClase),
				descripcionLarga: response.descripcionLarga,
				fechaPublicacionCurso: response.fechaPublicacionCurso,
				planesCurso: response.planesCurso,
				precioCurso: response.precioCurso,
			});

			if (curso.clasesCurso) {
				for (const clase of curso.clasesCurso) {
					clase.cursoClase = curso.idCurso;
				}
			}

			return curso;
		} catch (error) {
			console.error('Error en cargar curso:', error);
			return null;
		}
	}

	/**
	 * Actualiza los datos de un curso en el servidor.
	 * @param {Curso | null} curso - Objeto curso con los nuevos datos.
	 * @returns {Observable<Curso>} Observable con el curso actualizado.
	 */
	updateCurso(curso: Curso | null): Observable<Curso> {
		if (curso === null) {
			console.error('El curso no existe!!!');
			throw new Error('El curso no existe!!!');
		}
		return this.http.put<Curso>(`${this.backURL}/cursos/update`, curso, { observe: 'response', responseType: 'json' }).pipe(
			map((response: HttpResponse<Curso>) => {
				if (response.ok && response.body) {
					const old = this.cursos.find((c) => c.idCurso === curso.idCurso);
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

	/**
	 * Sube una imagen para un curso.
	 * @param {FormData} target - Datos del formulario con la imagen.
	 * @returns {Observable<string | null>} Observable con la URL de la imagen o null.
	 */
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

	/**
	 * Filtra los cursos que pertenecen a un profesor específico.
	 * @param {Usuario} profe - Objeto usuario del profesor.
	 * @returns {Curso[]} Lista de cursos del profesor.
	 */
	getCursosProfe(profe: Usuario) {
		const cursosProfe: Curso[] = [];
		for (const curso of this.cursos) {
			for (const idProfe of curso.profesoresCurso) {
				if (idProfe === profe.idUsuario) {
					cursosProfe.push(curso);
				}
			}
		}
		return cursosProfe;
	}

	/**
	 * Elimina un curso del servidor.
	 * @param {Curso} curso - Objeto curso a eliminar.
	 * @returns {Observable<boolean>} Observable indicando si la operación fue exitosa.
	 */
	deleteCurso(curso: Curso): Observable<boolean> {
		return this.http.delete<string>(this.backURL + '/cursos/delete/' + curso.idCurso, { observe: 'response', responseType: 'text' as 'json' }).pipe(
			map((response: HttpResponse<string>) => {
				if (response.ok) {
					this.cursos = this.cursos.filter((c) => c.idCurso !== curso.idCurso);
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

	/**
	 * Obtiene el estado actual de un curso en el servicio de streaming.
	 * @param {number} idCurso - ID del curso.
	 * @returns {Observable<number>} Observable con el estado del curso.
	 */
	getStatusCurso(idCurso: number): Observable<number> {
		return this.http.get<number>(this.backURLStreaming + '/status/' + idCurso, { observe: 'response' }).pipe(
			map((response: HttpResponse<number>) => {
				if (response.ok && response.body) {
					return response.body;
				}
				return 0;
			}),
			catchError((e: Error) => {
				console.error('Error al obtener el estado del curso:', e.message);
				return of(0);
			}),
		);
	}

	/**
	 * Obtiene todos los cursos disponibles en el servidor.
	 * @returns {Observable<Curso[]>} Observable con la lista de todos los cursos.
	 */
	getAllCursos() {
		return this.http.get<Curso[]>(this.backURL + '/cursos/getAll', { observe: 'response' }).pipe(
			map((response: HttpResponse<Curso[]>) => {
				if (response.ok && response.body) {
					this.cursos = response.body;
					for (const curso of this.cursos) {
						curso.clasesCurso = curso.clasesCurso?.sort((a, b) => a.posicionClase - b.posicionClase);
					}
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

	/**
	 * Obtiene los planes asociados a un curso específico.
	 * @param {number} idCurso - ID del curso.
	 * @returns {Observable<Plan[]>} Observable con la lista de planes del curso.
	 */
	getPlanesCurso(idCurso: number): Observable<Plan[]> {
		return this.http.get<Plan[]>(this.backURL + '/cursos/getPlanesCurso/' + idCurso, { observe: 'response' }).pipe(
			map((response: HttpResponse<Plan[]>) => {
				if (response.ok && response.body) {
					return response.body;
				}
				return [];
			}),
			catchError((e: Error) => {
				console.error('Error en obtener todos los planes: ' + e.message);
				return of([]);
			}),
		);
	}
}
