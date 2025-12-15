import { afterNextRender, ChangeDetectorRef, Component, NgZone } from '@angular/core';
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

	constructor(
		private readonly cursoService: CursosService,
		private readonly cdr: ChangeDetectorRef,
		private readonly ngZone: NgZone,
		public router: Router,
	) {
		afterNextRender(() => {
			const buscador: HTMLInputElement = document.getElementById('buscador') as HTMLInputElement;
			buscador.addEventListener('input', () => this.busca(buscador));
		});
	}

	private busca(buscador: HTMLInputElement) {
		if (buscador.value.length == 0) {
			this.result = false;
			this.cdr.detectChanges();
		} else {
			this.result = true;
			this.cdr.detectChanges();
			const cortina: HTMLDivElement = document.getElementById('cortina') as HTMLDivElement;
			cortina.innerHTML = '';
			const porNombre: Curso[] = this.cursoService.cursos.filter((curso) => this.normalize(curso.nombreCurso).includes(this.normalize(buscador.value)));
			const pordescripcion: Curso[] = this.cursoService.cursos.filter((curso) => this.normalize(curso.descripcionCorta).includes(this.normalize(buscador.value)));
			if (porNombre.length > 0) {
				const p: HTMLParagraphElement = document.createElement('p');
				p.classList.add('text-gray-400');
				p.textContent = 'Coincidencias por nombre:';
				cortina.appendChild(p);
				for (const curso of porNombre) {
					const row: HTMLParagraphElement = document.createElement('p');
					row.classList.add('hover:bg-white', 'whitespace-nowrap', 'rounded-lg', 'pr-2', 'pl-2');
					row.tabIndex = 0;
					this.addEvent(row, curso.idCurso);
					row.innerHTML = curso.nombreCurso.substring(0, this.normalize(curso.nombreCurso).indexOf(this.normalize(buscador.value))) + '<b>' + curso.nombreCurso.substring(this.normalize(curso.nombreCurso).indexOf(this.normalize(buscador.value)), this.normalize(curso.nombreCurso).indexOf(this.normalize(buscador.value)) + buscador.value.length) + '</b>' + curso.nombreCurso.substring(this.normalize(curso.nombreCurso).indexOf(this.normalize(buscador.value)) + buscador.value.length) + ' - ' + curso.descripcionCorta;
					cortina.appendChild(row);
				}
				if (pordescripcion.length > 0) cortina.appendChild(document.createElement('hr'));
			}
			if (pordescripcion.length > 0) {
				const p: HTMLParagraphElement = document.createElement('p');
				p.classList.add('text-gray-400');
				p.textContent = 'Coincidencias por descripción:';
				cortina.appendChild(p);
				for (const curso of pordescripcion) {
					const row: HTMLParagraphElement = document.createElement('p');
					row.classList.add('hover:bg-white', 'whitespace-nowrap', 'rounded-lg', 'pr-2', 'pl-2');
					row.tabIndex = 0;
					this.addEvent(row, curso.idCurso);
					row.innerHTML = curso.nombreCurso + ' - ' + curso.descripcionCorta.substring(0, this.normalize(curso.descripcionCorta).indexOf(this.normalize(buscador.value))) + '<b>' + curso.descripcionCorta.substring(this.normalize(curso.descripcionCorta).indexOf(this.normalize(buscador.value)), this.normalize(curso.descripcionCorta).indexOf(this.normalize(buscador.value)) + buscador.value.length) + '</b>' + curso.descripcionCorta.substring(this.normalize(curso.descripcionCorta).indexOf(this.normalize(buscador.value)) + buscador.value.length);
					cortina.appendChild(row);
				}
			}

			if (porNombre.length == 0 && pordescripcion.length == 0) {
				const p: HTMLParagraphElement = document.createElement('p');
				p.classList.add('text-grey-400');
				p.textContent = 'No se han encontrado coincidencias...';
			}
		}
	}

	private addEvent(element: HTMLElement, idCurso: number) {
		element.addEventListener('click', () => {
			this.navega(idCurso);
		});
		element.addEventListener('keydown', (event) => {
			if (event.key === 'Enter') {
				this.navega(idCurso);
			}
		});
	}

	private navega(idCurso: number) {
		this.result = false;
		this.cdr.detectChanges();
		this.ngZone.run(() => {
			this.router.navigate(['/curso/' + idCurso]);
		});
	}

	private normalize(texto: string): string {
		return texto
			.toString()
			.toLowerCase()
			.normalize('NFD')
			.replaceAll(/[\u0300-\u036f]/g, '');
	}
}
