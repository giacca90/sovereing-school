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
			buscador.addEventListener('input', () => {
				if (buscador.value.length == 0) {
					this.result = false;
					this.cdr.detectChanges();
				} else {
					this.result = true;
					this.cdr.detectChanges();
					const cortina: HTMLDivElement = document.getElementById('cortina') as HTMLDivElement;
					cortina.innerHTML = '';
					const porNombre: Curso[] = this.cursoService.cursos.filter((curso) =>
						curso.nombre_curso
							.toString()
							.toLowerCase()
							.normalize('NFD')
							.replace(/[\u0300-\u036f]/g, '')
							.includes(
								buscador.value
									.toLowerCase()
									.normalize('NFD')
									.replace(/[\u0300-\u036f]/g, ''),
							),
					);
					const porDescriccion: Curso[] = this.cursoService.cursos.filter((curso) =>
						curso.descriccion_corta
							.toString()
							.toLowerCase()
							.normalize('NFD')
							.replace(/[\u0300-\u036f]/g, '')
							.includes(
								buscador.value
									.toLowerCase()
									.normalize('NFD')
									.replace(/[\u0300-\u036f]/g, ''),
							),
					);
					if (porNombre.length > 0) {
						const p: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
						p.classList.add('text-gray-400');
						p.textContent = 'Coincidencias por nombre:';
						cortina.appendChild(p);
						porNombre.forEach((curso) => {
							const row: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
							row.classList.add('hover:bg-white', 'whitespace-nowrap', 'rounded-lg', 'pr-2', 'pl-2');
							row.tabIndex = 0;
							row.addEventListener('click', () => {
								this.result = false;
								this.cdr.detectChanges();
								this.ngZone.run(() => {
									this.router.navigate(['/curso/' + curso.id_curso]);
								});
							});
							row.addEventListener('keydown', (event) => {
								if (event.key === 'Enter') {
									this.result = false;
									this.cdr.detectChanges();
									this.ngZone.run(() => {
										this.router.navigate(['/curso/' + curso.id_curso]);
									});
								}
							});
							row.innerHTML =
								curso.nombre_curso.substring(
									0,
									curso.nombre_curso
										.toString()
										.toLocaleLowerCase()
										.normalize('NFD')
										.replace(/[\u0300-\u036f]/g, '')
										.indexOf(
											buscador.value
												.toLowerCase()
												.normalize('NFD')
												.replace(/[\u0300-\u036f]/g, ''),
										),
								) +
								'<b>' +
								curso.nombre_curso.substring(
									curso.nombre_curso
										.toString()
										.toLowerCase()
										.normalize('NFD')
										.replace(/[\u0300-\u036f]/g, '')
										.indexOf(
											buscador.value
												.toLowerCase()
												.normalize('NFD')
												.replace(/[\u0300-\u036f]/g, ''),
										),
									curso.nombre_curso
										.toString()
										.toLowerCase()
										.normalize('NFD')
										.replace(/[\u0300-\u036f]/g, '')
										.indexOf(
											buscador.value
												.toLowerCase()
												.normalize('NFD')
												.replace(/[\u0300-\u036f]/g, ''),
										) + buscador.value.length,
								) +
								'</b>' +
								curso.nombre_curso.substring(
									curso.nombre_curso
										.toString()
										.toLowerCase()
										.normalize('NFD')
										.replace(/[\u0300-\u036f]/g, '')
										.indexOf(
											buscador.value
												.toLowerCase()
												.normalize('NFD')
												.replace(/[\u0300-\u036f]/g, ''),
										) + buscador.value.length,
								) +
								' - ' +
								curso.descriccion_corta;
							cortina.appendChild(row);
						});
						if (porDescriccion.length > 0) cortina.appendChild(document.createElement('hr'));
					}
					if (porDescriccion.length > 0) {
						const p: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
						p.classList.add('text-gray-400');
						p.textContent = 'Coincidencias por descripción:';
						cortina.appendChild(p);
						porDescriccion.forEach((curso) => {
							const row: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
							row.classList.add('hover:bg-white', 'whitespace-nowrap', 'rounded-lg', 'pr-2', 'pl-2');
							row.tabIndex = 0;
							row.addEventListener('click', () => {
								this.result = false;
								this.cdr.detectChanges();
								this.ngZone.run(() => {
									this.router.navigate(['/curso/' + curso.id_curso]);
								});
							});
							row.addEventListener('keydown', (event) => {
								if (event.key === 'Enter') {
									this.result = false;
									this.cdr.detectChanges();
									this.ngZone.run(() => {
										this.router.navigate(['/curso/' + curso.id_curso]);
									});
								}
							});
							row.innerHTML =
								curso.nombre_curso +
								' - ' +
								curso.descriccion_corta.substring(
									0,
									curso.descriccion_corta
										.toString()
										.toLocaleLowerCase()
										.normalize('NFD')
										.replace(/[\u0300-\u036f]/g, '')
										.indexOf(
											buscador.value
												.toLowerCase()
												.normalize('NFD')
												.replace(/[\u0300-\u036f]/g, ''),
										),
								) +
								'<b>' +
								curso.descriccion_corta.substring(
									curso.descriccion_corta
										.toString()
										.toLowerCase()
										.normalize('NFD')
										.replace(/[\u0300-\u036f]/g, '')
										.indexOf(
											buscador.value
												.toLowerCase()
												.normalize('NFD')
												.replace(/[\u0300-\u036f]/g, ''),
										),
									curso.descriccion_corta
										.toString()
										.toLowerCase()
										.normalize('NFD')
										.replace(/[\u0300-\u036f]/g, '')
										.indexOf(
											buscador.value
												.toLowerCase()
												.normalize('NFD')
												.replace(/[\u0300-\u036f]/g, ''),
										) + buscador.value.length,
								) +
								'</b>' +
								curso.descriccion_corta.substring(
									curso.descriccion_corta
										.toString()
										.toLowerCase()
										.normalize('NFD')
										.replace(/[\u0300-\u036f]/g, '')
										.indexOf(
											buscador.value
												.toLowerCase()
												.normalize('NFD')
												.replace(/[\u0300-\u036f]/g, ''),
										) + buscador.value.length,
								);
							cortina.appendChild(row);
						});
					}

					if (porNombre.length == 0 && porDescriccion.length == 0) {
						const p: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
						p.classList.add('text-grey-400');
						p.textContent = 'No se han encontrado coincidencias...';
					}
				}
			});
		});
	}
}
