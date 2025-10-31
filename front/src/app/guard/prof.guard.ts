import { isPlatformServer } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { CursosService } from '../services/cursos.service';
import { LoginService } from '../services/login.service';

@Injectable({
	providedIn: 'root',
})
export class ProfGuard implements CanActivate {
	constructor(
		private readonly cursoService: CursosService,
		private readonly router: Router,
		private readonly loginService: LoginService,
		@Inject(PLATFORM_ID) private readonly platformId: object,
	) {}

	async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
		if (isPlatformServer(this.platformId)) return false;
		if (!this.loginService.usuario) {
			console.error('No hay usuario logueado');
			this.router.navigate(['/']);
			return false;
		}

		const idCurso: number = Number(route.params['idCurso']);

		if (idCurso === 0) {
			return true;
		}

		try {
			const curso = await this.cursoService.getCurso(idCurso);
			if (!curso) {
				console.error('Curso no encontrado: ' + idCurso);
				this.router.navigate(['/']);
				return false;
			}

			const isProfesor = curso.profesoresCurso.some((profesor) => profesor.idUsuario === this.loginService.usuario?.idUsuario);
			if (!isProfesor) {
				console.error('Usuario no es profesor del curso: ' + idCurso);
				this.router.navigate(['/']);
			}
			return isProfesor;
		} catch (error) {
			console.error('Error al obtener el curso:', error);
			this.router.navigate(['/']);
			return false;
		}
	}
}
