import { isPlatformServer } from '@angular/common';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { of } from 'rxjs';
import { Curso } from '../models/Curso';
import { Estadistica } from '../models/Estadistica';
import { Init } from '../models/Init';
import { Usuario } from '../models/Usuario';
import { CursosService } from './cursos.service';
import { UsuariosService } from './usuarios.service';

@Injectable({
	providedIn: 'root',
})
export class InitService {
	public estadistica: Estadistica | null = null;
	constructor(
		private http: HttpClient,
		private cursoService: CursosService,
		private usuarioService: UsuariosService,
		@Inject(PLATFORM_ID) private platformId: Object,
	) {
		this.carga();
	}

	get apiUrl(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			const url = (window as any).__env.BACK_BASE ?? '';
			return url + '/init';
		}
		return '';
	}

	async carga() {
		if (isPlatformServer(this.platformId)) {
			return;
		}
		this.usuarioService.profes = [];
		this.cursoService.cursos = [];
		this.http.get<Init>(this.apiUrl, { observe: 'response', withCredentials: true }).subscribe({
			next: (response: HttpResponse<Init>) => {
				if (response.ok && response.body) {
					response.body.profesInit.forEach((profe) => {
						this.usuarioService.profes.push(new Usuario(profe.id_usuario, profe.nombre_usuario, profe.foto_usuario, profe.presentacion));
					});
					const cursos: Curso[] = [];
					response.body.cursosInit.forEach((curso) => {
						const profes: Usuario[] = [];
						curso.profesores_curso.forEach((id) => {
							const prof = response.body?.profesInit.find((profe) => profe.id_usuario === id);
							if (prof) {
								profes.push(prof);
							}
						});
						cursos.push(new Curso(curso.id_curso, curso.nombre_curso, profes, curso.descriccion_corta, curso.imagen_curso));
					});
					this.cursoService.cursos = cursos;
					this.estadistica = response.body.estadistica;
				}
				return true;
			},
			error(e: Error) {
				console.error('Error en init: ' + e.message);
				return of(false);
			},
		});
	}
}
