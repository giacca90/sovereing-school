import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { Clase } from '../models/Clase';
import { CursosService } from './cursos.service';

@Injectable({
	providedIn: 'root',
})
export class ClaseService {
	constructor(
		private http: HttpClient,
		private cursoService: CursosService,
	) {}
	get backURL(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			return (window as any).__env.BACK_BASE ?? '';
		}
		return '';
	}

	deleteClase(clase: Clase): Observable<boolean> {
		const curso_clase: number | undefined = clase.curso_clase;
		return this.http.delete<string>(this.backURL + '/cursos/' + curso_clase + '/deleteClase/' + clase.id_clase, { observe: 'response', responseType: 'text' as 'json' }).pipe(
			map((response: HttpResponse<string>) => {
				if (response.ok) {
					//	this.cursos[this.cursos.findIndex((curso) => curso.id_curso === curso_clase)].clases_curso = undefined;
					return true;
				}
				return false;
			}),
			catchError((e: Error) => {
				console.error('Error en crear la clase: ' + e.message);
				return of(false);
			}),
		);
	}

	subeVideo(file: File, idCurso: number, idClase: number): Observable<string | null> {
		const formData = new FormData();
		formData.append('video', file, file.name);

		return this.http.post<string>(this.backURL + '/cursos/subeVideo/' + idCurso + '/' + idClase, formData, { observe: 'response', responseType: 'text' as 'json' }).pipe(
			map((response: HttpResponse<string>) => {
				if (response.ok) {
					return response.body;
				}
				return null;
			}),
			catchError((e: Error) => {
				console.error('Error en subir el video: ' + e.message);
				return of(null);
			}),
		);
	}
}
