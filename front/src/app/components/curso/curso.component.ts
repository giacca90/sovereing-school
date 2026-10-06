import { CommonModule, DOCUMENT, isPlatformBrowser } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, Inject, OnDestroy, PLATFORM_ID, Renderer2, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { Curso } from '../../models/Curso';
import { Plan } from '../../models/Plan';
import { CursosService } from '../../services/cursos.service';
import { LoginService } from '../../services/login.service';
import { UsuariosService } from '../../services/usuarios.service';
import { CompraCursoComponent } from './compra-curso/compra-curso.component';

@Component({
	selector: 'app-curso',
	standalone: true,
	imports: [CompraCursoComponent, CommonModule],
	templateUrl: './curso.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './curso.component.css',
})
export class CursoComponent implements OnDestroy {
	private idCurso: number = 0;
	public curso: Curso | null = null;
	public nombresProfesores: string | undefined = '';
	private readonly subscription: Subscription = new Subscription();
	public modalCourse: Curso | null = null;
	private readonly isBrowser: boolean;

	/**
	 * Constructor del componente.
	 * @param {ActivatedRoute} route - Ruta activada.
	 * @param {CursosService} cursoService - Servicio de cursos.
	 * @param {UsuariosService} usuarioService - Servicio de usuarios.
	 * @param {ChangeDetectorRef} cdr - Detección de cambios.
	 * @param {LoginService} loginService - Servicio de autenticación.
	 * @param {Renderer2} renderer - Renderer2 de Angular.
	 * @param {Router} router - Router de Angular.
	 * @param {Object} platformId - ID de la plataforma.
	 * @param {Document} document - Documento.
	 */
	constructor(
		private readonly route: ActivatedRoute,
		private readonly cursoService: CursosService,
		private readonly usuarioService: UsuariosService,
		private readonly cdr: ChangeDetectorRef,
		public loginService: LoginService,
		private readonly renderer: Renderer2,
		public router: Router,
		@Inject(PLATFORM_ID) private readonly platformId: Object,
		@Inject(DOCUMENT) private readonly document: Document,
	) {
		this.isBrowser = isPlatformBrowser(this.platformId);
		this.subscription.add(
			this.route.params.subscribe((params) => {
				this.idCurso = Number(params['idCurso']);
				this.getCurso();
			}),
		);
	}

	/**
	 * Comprueba si el usuario tiene acceso al plan del curso.
	 * @param {Plan | undefined} planUsuario - Plan del usuario.
	 * @returns {Plan | null} Plan si tiene acceso, null en caso contrario.
	 */
	compruebaPlan(planUsuario: Plan | undefined): Plan | null {
		if (planUsuario !== undefined && planUsuario !== null && this.curso?.planesCurso) {
			for (const idPlan of this.curso.planesCurso) {
				if (idPlan == planUsuario.idPlan) {
					return this.loginService.usuario?.planUsuario ?? null;
				}
			}
		}
		return null;
	}

	/**
	 * Limpieza de recursos al destruir el componente.
	 */
	ngOnDestroy(): void {
		this.subscription.unsubscribe();
	}

	/**
	 * Manejador para cerrar el modal con la tecla escape.
	 * @param {Event} event - Evento de teclado.
	 */
	@HostListener('window:keydown.escape', ['$event'])
	onKeydownHandler(event: Event) {
		if (this.modalCourse) {
			this.closeModal();
		}
	}

	/**
	 * Inicia el proceso de compra del curso.
	 * @param {Curso} curso - Curso a comprar.
	 */
	compraCurso(curso: Curso) {
		this.modalCourse = curso;
		if (this.isBrowser) {
			this.renderer.addClass(this.document.body, 'overflow-hidden');
		}
	}

	/**
	 * Finaliza la compra del curso.
	 * @param {Curso} curso - Curso comprado.
	 */
	cursoComprado(curso: Curso) {
		this.usuarioService.cursoComprado(curso).subscribe({
			next: (resp: boolean) => {
				if (resp) {
					alert('¡Curso comprado con éxito!');
					this.closeModal();
				} else {
					alert('Error al comprar el curso');
				}
			},
			error: (err) => {
				console.error(err);
				alert('Error al comprar el curso');
			},
		});
	}

	/**
	 * Cierra el modal de compra.
	 */
	closeModal() {
		if (this.isBrowser) {
			this.renderer.removeClass(this.document.body, 'overflow-hidden');
		}
		this.modalCourse = null;
	}

	/**
	 * Función para saber si el usuario tiene el curso
	 * @param curso el curso que se quiere comprobar Type: Curso
	 * @returns boolean si el usuario tiene el curso
	 */
	hasCurso(curso: Curso): boolean {
		const usuario = this.loginService.usuario;
		if (!usuario?.cursosUsuario) {
			return false;
		}
		return usuario.cursosUsuario.some((c: any) => c.idCurso === curso.idCurso);
	}

	getCurso() {
		this.cursoService.getCurso(this.idCurso).then((curso) => {
			this.curso = curso;
			if (this.curso) {
				if (this.curso.profesoresCurso.length == 1) this.nombresProfesores = this.usuarioService.getNombreProfe(this.curso.profesoresCurso[0]);
				else {
					let nombres: string | undefined = this.usuarioService.getNombreProfe(this.curso.profesoresCurso[0])?.toString();
					for (let i = 1; i < this.curso.profesoresCurso.length; i++) {
						nombres = nombres + ' y ' + this.usuarioService.getNombreProfe(this.curso.profesoresCurso[i]);
					}
					this.nombresProfesores = nombres;
				}
			}
			this.cdr.detectChanges();
		});
	}

	addCursoAUsuario(curso: Curso) {
		this.cursoComprado(curso);
		this.router.navigate(['/repro/' + curso.idCurso + '/0']);
	}
}
