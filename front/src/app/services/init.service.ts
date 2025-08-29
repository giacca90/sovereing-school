import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID, TransferState, makeStateKey } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { getGlobalInitCache, setGlobalInitCache } from '../../init-cache';
import { Curso } from '../models/Curso';
import { Estadistica } from '../models/Estadistica';
import { Init } from '../models/Init';
import { Usuario } from '../models/Usuario';
import { CursosService } from './cursos.service';
import { LoginService } from './login.service';
import { UsuariosService } from './usuarios.service';

const INIT_KEY = makeStateKey<Init>('init-data');

// Caché global para SSR (vive mientras dure el proceso del servidor)
//const globalCache: { init?: Init; timestamp?: number } = {};

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
		private loginService: LoginService,
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
		const platform: string = isPlatformBrowser(this.platformId) ? 'browser' : 'server';
		// 1. Cache en memoria del cliente (SPA)
		if (this.initDataCache) {
			console.log('>>> Usando initCache en memoria en ' + platform);
			console.log(this.initDataCache.estadistica);
			this.cargarEnServicios(this.initDataCache);
			// En SPA el usuario se obtiene solo desde /auth en el navegador
			if (isPlatformBrowser(this.platformId) && this.loginService.usuario === undefined) {
				await this.cargarUsuario();
			}
			return true;
		}

		// 2️⃣ TransferState (SSR → cliente)
		if (this.transferState.hasKey(INIT_KEY)) {
			console.log('>>> Usando TransferState en ' + platform);
			const data: Init = this.transferState.get(INIT_KEY, null as any);
			console.log(data.estadistica);

			if (isPlatformBrowser(this.platformId)) {
				await this.cargarUsuario(); // Obtener usuario desde cookie HttpOnly
			}

			return true;
		}

		// 3️⃣ Cache global SSR
		if (isPlatformServer(this.platformId)) {
			const cached = getGlobalInitCache();
			if (cached) {
				console.log('>>> Usando cache global SSR en ' + platform);
				console.log(cached.estadistica);
				this.cargarEnServicios(cached);
				this.transferState.set(INIT_KEY, cached);
				return true;
			}
		}

		// 4️⃣ Fetch real al backend
		try {
			console.log('>>> Pidiendo /init al backend en ' + platform);
			const response = await firstValueFrom(this.http.get<Init>(this.apiUrl, { headers: this.headers, withCredentials: true }));

			if (isPlatformServer(this.platformId)) {
				// Guardamos en cache global SSR
				setGlobalInitCache(response);
				// Pasamos datos al browser
				this.transferState.set(INIT_KEY, response);
			} else {
				this.initDataCache = response;
			}

			this.cargarEnServicios(response);

			// En navegador, cargar usuario en paralelo después
			if (isPlatformBrowser(this.platformId)) {
				await this.cargarUsuario();
			}

			return true;
		} catch (e) {
			console.error('[InitService] Error al cargar init-data:', e);
			return false;
		}
	}

	private async cargarUsuario() {
		try {
			const usuario = await firstValueFrom(this.http.get<Usuario | null>(this.apiUrl + '/auth', { headers: this.headers, withCredentials: true }));
			this.loginService.usuario = usuario ?? null;
		} catch (err) {
			console.warn('[InitService] Error al obtener usuario desde /auth:', err);
			this.loginService.usuario = null;
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

	preloadFromGlobalCache() {
		const cached = getGlobalInitCache();
		if (cached) {
			console.log('[InitService] Refrescando TransferState desde cache global SSR');
			this.transferState.set(INIT_KEY, cached);
		}
	}
}
