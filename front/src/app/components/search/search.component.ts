import { ChangeDetectorRef, Component, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { Curso } from '../../models/Curso';
import { CursosService } from '../../services/cursos.service';

@Component({
	selector: 'app-search',
	standalone: true,
	imports: [],
	templateUrl: './search.component.html',
	styleUrl: './search.component.css',
})

// TODO: Añadir busqueda por profesor
export class SearchComponent {
	result: boolean = false;
	porNombre: Curso[] = [];
	porDescripcion: Curso[] = [];
	busqueda: string = '';

	/**
	 * Constructor del componente.
	 * @param {CursosService} cursoService - Servicio de cursos.
	 * @param {ChangeDetectorRef} cdr - Detección de cambios.
	 * @param {NgZone} ngZone - Zona de Angular.
	 * @param {Router} router - Router de Angular.
	 */
	constructor(
		private readonly cursoService: CursosService,
		private readonly cdr: ChangeDetectorRef,
		private readonly ngZone: NgZone,
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
			this.result = false;
			this.porNombre = [];
			this.porDescripcion = [];
		} else {
			this.result = true;
			this.porNombre = this.cursoService.cursos.filter((curso) => this.normalize(curso.nombreCurso).includes(this.normalize(this.busqueda)));
			this.porDescripcion = this.cursoService.cursos.filter((curso) => this.normalize(curso.descripcionCorta).includes(this.normalize(this.busqueda)));
		}
		this.cdr.detectChanges();
	}

	/**
	 * Navega a la vista de un curso específico.
	 * @param {number} idCurso - ID del curso.
	 */
	navega(idCurso: number) {
		this.result = false;
		this.ngZone.run(() => {
			this.router.navigate(['/curso/' + idCurso]);
		});
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
