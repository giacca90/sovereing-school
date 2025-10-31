import { Component, OnDestroy } from '@angular/core';
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
	imports: [CompraCursoComponent],
	templateUrl: './curso.component.html',
	styleUrl: './curso.component.css',
})
export class CursoComponent implements OnDestroy {
	private idCurso: number = 0;
	public curso: Curso | null = null;
	public nombresProfesores: string | undefined = '';
	private readonly subscription: Subscription = new Subscription();
	public modalCourse: Curso | null = null;
	private escKeyListener: any;

	constructor(
		private readonly route: ActivatedRoute,
		private readonly cursoService: CursosService,
		private readonly usuarioService: UsuariosService,
		public loginService: LoginService,
		public router: Router,
	) {
		this.subscription.add(
			this.route.params.subscribe((params) => {
				this.idCurso = Number(params['idCurso']);
			}),
		);

		this.cursoService.getCurso(this.idCurso).then((curso) => {
			this.curso = curso;
			if (this.curso) {
				if (this.curso.profesoresCurso.length == 1) this.nombresProfesores = this.usuarioService.getNombreProfe(this.curso.profesoresCurso[0].idUsuario);
				else {
					let nombres: string | undefined = this.usuarioService.getNombreProfe(this.curso.profesoresCurso[0].idUsuario)?.toString();
					for (let i = 1; i < this.curso.profesoresCurso.length; i++) {
						nombres = nombres + ' y ' + this.usuarioService.getNombreProfe(this.curso.profesoresCurso[i].idUsuario);
					}
					this.nombresProfesores = nombres;
				}
			}
		});
	}

	compruebaPlan(planUsuario: Plan | undefined): Plan | null {
		if (planUsuario !== undefined && planUsuario !== null && this.curso?.planesCurso) {
			for (const plan of this.curso.planesCurso) {
				if (plan.idPlan == planUsuario.idPlan) {
					return plan;
				}
			}
		}
		return null;
	}

	ngOnDestroy(): void {
		this.subscription.unsubscribe();
	}

	compraCurso(curso: Curso) {
		this.modalCourse = curso;
		document.body.classList.add('overflow-hidden');

		this.escKeyListener = (event: KeyboardEvent) => {
			if (event.key === 'Escape') {
				event.preventDefault();
				this.closeModal();
			}
		};

		document.addEventListener('keydown', this.escKeyListener);
	}

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

	closeModal() {
		document.body.classList.remove('overflow-hidden');
		document.removeEventListener('keydown', this.escKeyListener);
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
}
