// src/app/services/init.service.ts
import { isPlatformServer } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID, TransferState, makeStateKey } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Curso } from '../models/Curso';
import { Estadistica } from '../models/Estadistica';
import { Init } from '../models/Init';
import { Usuario } from '../models/Usuario';
import { CursosService } from './cursos.service';
import { UsuariosService } from './usuarios.service';

const INIT_KEY = makeStateKey<Init>('init-data');

@Injectable({ providedIn: 'root' })
export class InitService {
	public estadistica: Estadistica | null = null;
	private initDataCache: Init | null = null;

	headers = new HttpHeaders({
		'Authorization': 'Basic Visitante:visitante',
	});

	constructor(
		private http: HttpClient,
		private cursoService: CursosService,
		private usuarioService: UsuariosService,
		private transferState: TransferState,
		@Inject(PLATFORM_ID) private platformId: Object,
	) {}

	get apiUrl(): string {
		if (typeof window !== 'undefined' && (window as any).__env) {
			return (window as any).__env.BACK_BASE + '/init';
		}
		return 'http://sovschool-back-base:8080/init'; // o usar process.env si querés algo más flexible
	}

	async carga(): Promise<boolean> {
		if (this.initDataCache) {
			this.cargarEnServicios(this.initDataCache);
			return true;
		}

		if (this.transferState.hasKey(INIT_KEY)) {
			const data = this.transferState.get(INIT_KEY, null as any);
			this.transferState.remove(INIT_KEY);
			this.cargarEnServicios(data);
			return true;
		}

		try {
			const response = await firstValueFrom(this.http.get<Init>(this.apiUrl, { headers: this.headers, withCredentials: true }));

			if (isPlatformServer(this.platformId)) {
				this.initDataCache = response;
				this.transferState.set(INIT_KEY, response);
				console.log('[InitService] Datos cargados en cache:', response);
			}

			this.cargarEnServicios(response);
			return true;
		} catch (e) {
			console.error('[InitService] Error al cargar datos:', e);
			return false;
		}
	}

	private cargarEnServicios(data: Init) {
		this.usuarioService.profes = data.profesInit.map((profe) => new Usuario(profe.id_usuario, profe.nombre_usuario, profe.foto_usuario, profe.presentacion));

		this.cursoService.cursos = data.cursosInit.map((curso) => {
			const profes = curso.profesores_curso.map((id) => this.usuarioService.profes.find((p) => p.id_usuario === id)).filter(Boolean) as Usuario[];

			return new Curso(curso.id_curso, curso.nombre_curso, profes, curso.descriccion_corta, curso.imagen_curso);
		});

		this.estadistica = data.estadistica;
	}
}
