import { isPlatformServer } from '@angular/common';
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

@Injectable({ providedIn: 'root' })
export class InitService {
	public estadistica: Estadistica | null = null;
	private initDataCache: Init | null = null;

	headers = new HttpHeaders({
		'Authorization': 'Basic Visitante:visitante',
	});

	constructor(
		private readonly http: HttpClient,
		private readonly cursoService: CursosService,
		private readonly usuarioService: UsuariosService,
		private readonly transferState: TransferState,
		private readonly loginService: LoginService,
		@Inject(PLATFORM_ID) private readonly platformId: Object,
	) {}

	get apiUrl(): string {
		// Ruta al contenedor
		if (isPlatformServer(this.platformId)) {
			return process.env['BACK_BASE_DOCKER'] + '/init';
		} else if (typeof globalThis.window !== 'undefined' && (globalThis.window as any).__env) {
			return (globalThis.window as any).__env.BACK_BASE + '/init';
		} else if (process.env['BACK_BASE']) {
			return process.env['BACK_BASE'] + '/init';
		}
		return 'https://localhost:8080/init';
	}

	/**
	 * Función que carga los datos de inicio desde el backend
	 * Sirve para el pre-renderizado de la pagina, y para evitar llamadas al backend
	 * @returns
	 */
	async carga(): Promise<boolean> {
		// 1. Cache en memoria del cliente (SPA)
		if (this.initDataCache) {
			//console.log('>>> Usando initCache en memoria en ' + this.platform);
			//console.log(this.initDataCache.cursosInit);
			//console.log(this.initDataCache.estadistica);
			this.cargarEnServicios(this.initDataCache);
			return true;
		}

		// 2️⃣ TransferState (SSR → cliente)
		if (this.transferState.hasKey(INIT_KEY)) {
			//console.log('>>> Usando TransferState en ' + this.platform + ' con INIT_KEY: ' + INIT_KEY);
			//console.log(this.transferState.get(INIT_KEY, null as any).cursosInit);
			//console.log(this.transferState.get(INIT_KEY, null as any).estadistica);

			const data: Init = this.transferState.get(INIT_KEY, null as any);

			this.cargarEnServicios(data);

			return true;
		}

		// 3️⃣ Cache global SSR
		if (isPlatformServer(this.platformId)) {
			const cached = getGlobalInitCache();
			if (cached) {
				//console.log('>>> Usando cache global SSR en ' + this.platform);
				//console.log(cached.cursosInit);
				//console.log(cached.estadistica);
				this.cargarEnServicios(cached);
				this.transferState.set(INIT_KEY, cached);
				return true;
			}
		}

		// 4️⃣ Fetch real al backend
		try {
			//console.log('>>> Pidiendo /init al backend en ' + this.platform);
			const response = await firstValueFrom(this.http.get<Init>(this.apiUrl, { headers: this.headers, withCredentials: true }));

			if (isPlatformServer(this.platformId)) {
				// Guardamos en cache global SSR
				//console.log('>>> Guardando en cache global SSR');
				//console.log(response.cursosInit);
				//console.log(response.estadistica);
				setGlobalInitCache(response);
				// Pasamos datos al browser
				//console.log('>>> Pasando datos al transferState con INIT_KEY: ' + INIT_KEY);
				this.transferState.set(INIT_KEY, response);
			} else {
				this.initDataCache = response;
			}

			this.cargarEnServicios(response);

			return true;
		} catch (e) {
			console.error('[InitService] Error al cargar init-data:', e);
			return false;
		}
	}

	/**
	 * Carga el usuario y el initToken desde /auth
	 */
	public async cargarUsuario() {
		try {
			//console.log('[InitService] Cargando usuario desde /auth');
			const usuario = await firstValueFrom(this.http.get<Usuario | null>(this.apiUrl + '/auth', { headers: this.headers, withCredentials: true }));
			this.loginService.usuario = usuario;
		} catch (err) {
			console.warn('[InitService] Error al obtener usuario desde /auth:', err);
			this.loginService.usuario = null;
		}
	}

	/**
	 * Carga los datos de inicio en los servicios que los necesitan
	 * @param data Datos de inicio :Init
	 */
	private cargarEnServicios(data: Init) {
		this.usuarioService.profes = data.profesInit.map((profe) => new Usuario(profe.id_usuario, profe.nombre_usuario, profe.foto_usuario, profe.presentacion));

		this.cursoService.cursos = data.cursosInit.map((curso) => {
			const profes = curso.profesores_curso.map((id) => this.usuarioService.profes.find((p) => p.id_usuario === id)).filter(Boolean) as Usuario[];
			return new Curso(curso.id_curso, curso.nombre_curso, profes, curso.descriccion_corta, curso.imagen_curso);
		});

		this.estadistica = data.estadistica;
	}

	preloadFromGlobalCache() {
		const cached = getGlobalInitCache();
		if (cached) {
			//console.log('[InitService] Refrescando TransferState desde cache global SSR');
			//console.log(cached.cursosInit);
			//console.log(cached.estadistica);
			this.transferState.set(INIT_KEY, cached);
		}
	}
}
