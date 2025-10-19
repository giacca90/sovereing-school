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
	private id_curso: number = 0;
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
				this.id_curso = Number(params['id_curso']);
			}),
		);

		this.cursoService.getCurso(this.id_curso).then((curso) => {
			this.curso = curso;
			if (this.curso) {
				if (this.curso.profesores_curso.length == 1) this.nombresProfesores = this.usuarioService.getNombreProfe(this.curso.profesores_curso[0].id_usuario);
				else {
					let nombres: string | undefined = this.usuarioService.getNombreProfe(this.curso.profesores_curso[0].id_usuario)?.toString();
					for (let i = 1; i < this.curso.profesores_curso.length; i++) {
						nombres = nombres + ' y ' + this.usuarioService.getNombreProfe(this.curso.profesores_curso[i].id_usuario);
					}
					this.nombresProfesores = nombres;
				}
			}
		});
	}

	compruebaPlan(planUsuario: Plan | undefined): Plan | null {
		if (planUsuario !== undefined && planUsuario !== null && this.curso?.planes_curso) {
			for (const plan of this.curso.planes_curso) {
				if (plan.id_plan == planUsuario.id_plan) {
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
		if (!usuario?.cursos_usuario) {
			return false;
		}
		return usuario.cursos_usuario.some((c: any) => c.id_curso === curso.id_curso);
	}
}
