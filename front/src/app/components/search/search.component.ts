import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Curso } from '../../models/Curso';
import { CursosService } from '../../services/cursos.service';
import { UsuariosService } from '../../services/usuarios.service';

@Component({
	selector: 'app-search',
	imports: [],
	templateUrl: './search.component.html',
	styleUrl: './search.component.css',
})

// TODO: Añadir busqueda por profesor
export class SearchComponent {
	result = signal<boolean>(false);
	porNombre = signal<Curso[]>([]);
	porDescripcion = signal<Curso[]>([]);
	porProfesor = signal<Curso[]>([]);
	busqueda: string = '';

	/**
	 * Constructor del componente.
	 * @param {CursosService} cursoService - Servicio de cursos.
	 * @param {Router} router - Router de Angular.
	 */
	constructor(
		private readonly cursoService: CursosService,
		private readonly usuarioService: UsuariosService,
		public router: Router,
	) {}

	/**
	 * Realiza la búsqueda de cursos.
	 * @param {Event} $event - Evento de entrada.
	 */
	busca($event: Event) {
		const buscador = $event.target as HTMLInputElement;
		this.busqueda = buscador.value;
		if (this.busqueda.length == 0) {
			this.result.set(false);
			this.porNombre.set([]);
			this.porDescripcion.set([]);
			this.porProfesor.set([]);
		} else {
			this.result.set(true);
			this.porNombre.set(this.cursoService.cursos().filter((curso) => this.normalize(curso.nombreCurso).includes(this.normalize(this.busqueda))));
			this.porDescripcion.set(this.cursoService.cursos().filter((curso) => this.normalize(curso.descripcionCorta).includes(this.normalize(this.busqueda))));
			this.porProfesor.set(
				this.cursoService.cursos().filter((curso) =>
					(curso.profesoresCurso || []).some((profesorId) => {
						// 1. Obtenemos el objeto usuario a partir del ID
						const profesor = this.usuarioService.profes.find((profesor) => profesor.idUsuario === profesorId);

						// Asegúrate de extraer la propiedad del nombre (ej. usuario.nombre o usuario.nombreCompleto)
						// Dependiendo de cómo sea tu objeto usuario, ajústalo aquí:
						const nombreProfesor = profesor?.nombreUsuario || '';

						// 2. Comprobamos si el nombre normalizado incluye el texto de búsqueda
						return this.normalize(nombreProfesor).includes(this.normalize(this.busqueda));
					}),
				),
			);
		}
	}

	/**
	 * Navega a la vista de un curso específico.
	 * @param {number} idCurso - ID del curso.
	 */
	navega(idCurso: number) {
		this.result.set(false);
		this.router.navigate(['/curso/' + idCurso]);
	}

	/**
	 * Normaliza el texto para la búsqueda.
	 * @param {string} texto - Texto a normalizar.
	 * @returns {string} Texto normalizado.
	 */
	normalize(texto: string): string {
		return texto
			.toString()
			.toLowerCase()
			.normalize('NFD')
			.replaceAll(/[\u0300-\u036f]/g, '');
	}
}
