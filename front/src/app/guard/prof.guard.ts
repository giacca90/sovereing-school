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
			this.router.navigate(['/']);
			return false;
		}

		const id_curso = route.params['id_curso'];

		if (id_curso === '0') {
			return true;
		}

		try {
			const curso = await this.cursoService.getCurso(id_curso);
			if (!curso) {
				this.router.navigate(['/']);
				return false;
			}

			const isProfesor = curso.profesores_curso.some((profesor) => profesor.id_usuario === this.loginService.usuario?.id_usuario);
			if (!isProfesor) {
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
