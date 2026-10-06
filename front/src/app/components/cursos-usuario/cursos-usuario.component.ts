import { ChangeDetectorRef, Component, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { CursosService } from '../../services/cursos.service';
import { LoginService } from '../../services/login.service';

@Component({
	selector: 'app-cursos-usuario',
	standalone: true,
	imports: [],
	templateUrl: './cursos-usuario.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './cursos-usuario.component.css',
})
export class CursosUsuarioComponent {
	/**
	 * Constructor del componente.
	 * @param {LoginService} loginService - Servicio de autenticación.
	 * @param {CursosService} cursoService - Servicio de gestión de cursos.
	 * @param {ChangeDetectorRef} cdr - Referencia para la detección de cambios.
	 * @param {Router} router - Router de Angular para navegación.
	 */
	constructor(
		public loginService: LoginService,
		public cursoService: CursosService,
		private readonly cdr: ChangeDetectorRef,
		public router: Router,
	) {}
}
