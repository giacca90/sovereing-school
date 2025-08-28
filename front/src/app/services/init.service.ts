import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID, TransferState, makeStateKey } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Curso } from '../models/Curso';
import { Estadistica } from '../models/Estadistica';
import { Init } from '../models/Init';
import { Usuario } from '../models/Usuario';
import { CursosService } from './cursos.service';
import { LoginService } from './login.service';
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
		const tareas: Promise<any>[] = [];

		// 1️⃣ Carga de init-data
		tareas.push(this.cargaInitData());

		// 2️⃣ Carga de usuario desde /auth (solo en navegador)
		if (isPlatformBrowser(this.platformId)) {
			const usuarioPromise = firstValueFrom(this.http.get<Usuario | null>(this.apiUrl + '/auth', { headers: this.headers, withCredentials: true }))
				.then((usuario) => {
					this.loginService.usuario = usuario ?? null;
				})
				.catch((err) => {
					console.warn('[InitService] No hay usuario logueado:', err);
					this.loginService.usuario = null;
				});

			tareas.push(usuarioPromise);
		}

		// 3️⃣ Espera a que ambas terminen antes de continuar
		await Promise.allSettled(tareas);

		return true;
	}

	// -------------------
	// Lógica de carga init-data
	private async cargaInitData(): Promise<boolean> {
		// 1️⃣ Cache local en SPA
		if (this.initDataCache) {
			this.cargarEnServicios(this.initDataCache);
			return true;
		}

		// 2️⃣ TransferState del SSR al browser
		if (this.transferState.hasKey(INIT_KEY)) {
			const data = this.transferState.get(INIT_KEY, null as any);
			this.cargarEnServicios(data);
			return true;
		}

		// 3️⃣ Cache global SSR
		if (isPlatformServer(this.platformId)) {
			const isValid = globalCache.init && Date.now() - (globalCache.timestamp ?? 0) < 60 * 1000;
			if (isValid) {
				this.cargarEnServicios(globalCache.init!);
				this.transferState.set(INIT_KEY, globalCache.init!);
				return true;
			}
		}

		// 4️⃣ Fetch real al backend
		try {
			const response = await firstValueFrom(this.http.get<Init>(this.apiUrl, { headers: this.headers, withCredentials: true }));

			if (isPlatformServer(this.platformId)) {
				globalCache.init = response;
				globalCache.timestamp = Date.now();
				this.transferState.set(INIT_KEY, response);
			} else {
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
}
