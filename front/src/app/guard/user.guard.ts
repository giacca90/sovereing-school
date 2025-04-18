import { isPlatformServer } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { Usuario } from '../models/Usuario';
import { CursosService } from '../services/cursos.service';

@Injectable({
	providedIn: 'root',
})
export class UserGuard implements CanActivate {
	constructor(
		private cursoService: CursosService,
		private router: Router,
		@Inject(PLATFORM_ID) private platformId: object,
	) {}

	async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
		if (isPlatformServer(this.platformId)) return false;

		if (!localStorage.getItem('Usuario')) {
			this.router.navigate(['']);
			return false;
		}

		const usuario: Usuario = JSON.parse(localStorage.getItem('Usuario') as string);

		// Permitir acceso inmediato a usuarios con roll 0 o 1
		if (usuario.roll_usuario === 0 || usuario.roll_usuario === 1) {
			return true;
		}

		const id_curso = route.params['id_curso'];

		try {
			const curso = await this.cursoService.getCurso(id_curso);
			if (!curso || !usuario.cursos_usuario) {
				this.router.navigate(['']);
				return false;
			}

			const hasAccess = usuario.cursos_usuario?.some((cursoUs) => cursoUs.id_curso === curso.id_curso);
			if (!hasAccess) {
				this.router.navigate(['']);
			}
			return hasAccess;
		} catch (error) {
			console.error('Error al obtener el curso:', error);
			this.router.navigate(['']);
			return false;
		}
	}
}
