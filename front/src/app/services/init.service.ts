import { isPlatformBrowser, isPlatformServer } from '@angular/common';
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

// Caché global para SSR (vive mientras dure el proceso del servidor)
const globalCache: { init?: Init; timestamp?: number } = {};

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
		} else if (process.env['BACK_BASE']) {
			return process.env['BACK_BASE'] + '/init';
		}
		return 'https://localhost:8080/init';
	}

	async carga(): Promise<boolean> {
		// 1. Cache en memoria del cliente (SPA)
		if (this.initDataCache) {
			//console.log('>>> Usando initCache en memoria');
			this.cargarEnServicios(this.initDataCache);
			return true;
		}

		// 2. TransferState (del SSR al browser)
		if (this.transferState.hasKey(INIT_KEY)) {
			//console.log('>>> Usando TransferState');
			const data = this.transferState.get(INIT_KEY, null as any);

			// Hacemos la llamada para que el backend setee la cookie en el browser
			if (isPlatformBrowser(this.platformId)) {
				this.http.get<String>(this.apiUrl + '/auth', { headers: this.headers, withCredentials: true, responseType: 'text' as 'json' }).subscribe();
			}

			this.cargarEnServicios(data);
			return true;
		}

		// 3. Caché global para SSR (evita llamar al backend en cada request)
		if (isPlatformServer(this.platformId)) {
			const isValid = globalCache.init && Date.now() - (globalCache.timestamp ?? 0) < 60 * 1000; // 1 minuto
			if (isValid) {
				//console.log('>>> Usando cache global SSR');
				this.cargarEnServicios(globalCache.init!);
				this.transferState.set(INIT_KEY, globalCache.init!);
				return true;
			}
		}

		// 4. Fetch real al backend
		try {
			//console.log('>>> Pidiendo /init al backend');
			const response = await firstValueFrom(this.http.get<Init>(this.apiUrl, { headers: this.headers, withCredentials: true }));

			if (isPlatformServer(this.platformId)) {
				// Guardamos en cache global SSR
				globalCache.init = response;
				globalCache.timestamp = Date.now();

				// Pasamos datos al browser
				this.transferState.set(INIT_KEY, response);
			} else {
				// Guardamos en cache local del cliente
				this.initDataCache = response;
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

	auth() {
		this.http.get<String>(this.apiUrl + '/auth', { headers: this.headers, withCredentials: true, responseType: 'text' as 'json' }).subscribe();
	}
}
