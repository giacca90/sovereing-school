import { isPlatformServer } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { Usuario } from '../models/Usuario';
import { CursosService } from '../services/cursos.service';
import { LoginService } from '../services/login.service';

@Injectable({
	providedIn: 'root',
})
export class UserGuard implements CanActivate {
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

		const usuario: Usuario = this.loginService.usuario;
		// Permitir acceso inmediato a usuarios con roll 0 o 1
		if (usuario.roll_usuario === 'ADMIN' || usuario.roll_usuario === 'PROF') {
			return true;
		}

		const id_curso = route.params['id_curso'];

		try {
			const curso = await this.cursoService.getCurso(id_curso);
			if (!curso || !usuario.cursos_usuario) {
				this.router.navigate(['/']);
				return false;
			}

			const hasAccess = usuario.cursos_usuario?.some((cursoUs) => cursoUs.id_curso === curso.id_curso);
			if (!hasAccess) {
				this.router.navigate(['/']);
			}
			return hasAccess;
		} catch (error) {
			console.error('Error al obtener el curso:', error);
			this.router.navigate(['/']);
			return false;
		}
	}
}
