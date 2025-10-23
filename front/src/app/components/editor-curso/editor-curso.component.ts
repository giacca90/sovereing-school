import { isPlatformBrowser } from '@angular/common';
import { Component, HostListener, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CanComponentDeactivate } from '../../interfaces/can-component-deactivate';
import { Clase } from '../../models/Clase';
import { Curso } from '../../models/Curso';
import { CursosService } from '../../services/cursos.service';
import { InitService } from '../../services/init.service';
import { LoginService } from '../../services/login.service';
import { StreamingService } from '../../services/streaming.service';
import { EditorClaseComponent } from './editor-clase/editor-clase.component';

@Component({
	selector: 'app-editor-curso',
	standalone: true,
	imports: [FormsModule, EditorClaseComponent],
	templateUrl: './editor-curso.component.html',
	styleUrl: './editor-curso.component.css',
})
export class EditorCursoComponent implements OnInit, OnDestroy, CanComponentDeactivate {
	private readonly subscription: Subscription = new Subscription();
	idCurso!: number;
	curso!: Curso;
	backBase!: string;
	draggedElementId: number | null = null;
	editado: boolean = false;
	claseEditar: Clase | null = null;
	isBrowser: boolean;

	constructor(
		private readonly route: ActivatedRoute,
		private readonly router: Router,
		public cursoService: CursosService,
		public loginService: LoginService,
		public streamingService: StreamingService,
		private readonly initService: InitService,
		@Inject(PLATFORM_ID) private readonly platformId: Object,
	) {
		this.subscription.add(
			this.route.params.subscribe((params) => {
				this.idCurso = Number(params['id_curso']);
			}),
		);
		this.isBrowser = isPlatformBrowser(platformId);
	}
	canDeactivate(): boolean {
		if (this.streamingService.emitiendo) {
			return confirm('Estás emitiendo. ¿Seguro que quieres salir?');
		}
		if (this.editado) {
			return confirm('Tienes cambios sin guardar. ¿Seguro que quieres salir?');
		}
		return true;
	}

	@HostListener('window:beforeunload', ['$event'])
	unloadNotification($event: BeforeUnloadEvent) {
		if (this.streamingService.emitiendo || this.editado) {
			$event.preventDefault();
		}
	}

	ngOnInit(): void {
		if (this.idCurso === 0) {
			if (this.loginService.usuario) {
				this.curso = new Curso(0, '', [this.loginService.usuario], '', '', new Date(), [], [], '', 0);
			} else {
				console.error('No hay usuario logueado');
				this.router.navigate(['/']);
				return;
			}
		} else {
			this.cursoService
				.getCurso(this.idCurso)
				.then((curso) => {
					if (!curso) {
						console.error('Curso no encontrado: ' + this.idCurso);
						this.router.navigate(['/']);
						return;
					}

					this.curso = structuredClone(curso);
					if (this.curso.clases_curso) {
						for (const clase of this.curso.clases_curso) {
							clase.curso_clase = this.curso.id_curso;
						}
					}
				})
				.catch((err) => {
					console.error('Error al obtener el curso:', err);
					this.router.navigate(['/']);
				});
		}

		if (isPlatformBrowser(this.platformId)) {
			this.backBase = (globalThis.window as any).__env?.BACK_BASE ?? '';
		}
	}

	ngOnDestroy(): void {
		this.subscription.unsubscribe();
	}

	/**
	 * Evento de arrastre para posicionar los cursos
	 * @param event Evento de arrastre
	 * @param id Id del curso
	 */
	onDragStart(event: Event, id: number) {
		const event2: DragEvent = event as DragEvent;
		const div = event2.target as HTMLDivElement;
		const img = div.cloneNode(true) as HTMLDivElement;
		img.className = div.className;
		img.style.position = 'absolute';
		img.style.top = '-9999px';
		document.body.appendChild(img);
		this.draggedElementId = id;
		event2.dataTransfer?.setData('text/plain', id.toString());
		event2.dataTransfer?.setDragImage(img, 0, 0);
		setTimeout(() => {
			img.remove();
		}, 0);
		div.classList.add('opacity-0');
	}

	/**
	 * Efecto de mover los otros cursos al arrastrar
	 * @param event Evento de arrastre
	 * @param id Id del curso
	 */
	onDragOver(event: Event, id: number) {
		event.preventDefault();
		if (this.draggedElementId === null || this.draggedElementId === id) {
			return;
		}

		if (this.curso?.clases_curso) {
			const draggedIndex = this.curso.clases_curso.findIndex((clase) => clase.id_clase === this.draggedElementId);
			const targetIndex = this.curso.clases_curso.findIndex((clase) => clase.id_clase === id);

			if (draggedIndex > -1 && targetIndex > -1 && draggedIndex !== targetIndex) {
				const draggedClase = this.curso.clases_curso[draggedIndex];
				this.compruebaCambios();
				const temp: number = draggedClase.posicion_clase;
				draggedClase.posicion_clase = this.curso.clases_curso[targetIndex].posicion_clase;
				this.curso.clases_curso[targetIndex].posicion_clase = temp;
				this.curso.clases_curso.splice(draggedIndex, 1);
				this.curso.clases_curso.splice(targetIndex, 0, draggedClase);
			}
		}
	}

	/**
	 * Evento de soltar los cursos
	 * @param event Evento de soltar
	 */
	onDrop(event: Event) {
		event.preventDefault();
		this.draggedElementId = null;
	}

	getClosestElementId(event: DragEvent): number | null {
		const elements = Array.from(document.querySelectorAll('[id^="clase-"]'));
		const y = event.clientY;
		const closestElement = elements.reduce(
			(closest, element) => {
				const box = element.getBoundingClientRect();
				const offset = y - box.top - box.height / 2;
				if (offset < 0 && offset > closest.offset) {
					return { offset: offset, element };
				} else {
					return closest;
				}
			},
			{ offset: Number.NEGATIVE_INFINITY, element: null } as { offset: number; element: Element | null },
		).element;
		return closestElement ? Number.parseInt(closestElement.id.split('-')[1], 10) : null;
	}

	onDragEnd(event: Event): void {
		const event2 = event as DragEvent;
		(event2.target as HTMLDivElement).classList.remove('opacity-0');
		this.draggedElementId = null;
	}

	compruebaCambios() {
		this.cursoService.getCurso(this.curso.id_curso).then((curso) => {
			console.log('this.curso: ' + JSON.stringify(this.curso));
			console.log('curso: ' + JSON.stringify(curso));
			this.editado = JSON.stringify(this.curso) !== JSON.stringify(curso);
		});
	}

	updateCurso() {
		this.subscription.add(
			this.cursoService.updateCurso(this.curso).subscribe({
				next: (success: Curso) => {
					if (success) {
						this.initService.carga();
						this.editado = false;
						if (this.idCurso == 0) {
							this.cursoService.cursos.push(success);
						}
						this.router.navigate(['/cursosUsuario']);
					} else {
						console.error('Falló la actualización del curso en editor-curso');
					}
				},
				error: (error) => {
					console.error('Error al actualizar el curso:', error);
				},
			}),
		);
	}

	async editarClase(clase: Clase) {
		const curso: Curso | null = await this.cursoService.getCurso(clase.curso_clase, true);
		Object.assign(this.curso, curso);
		if (!curso) {
			console.error('No se pudo obtener el curso');
			return;
		}
		const actual = curso.clases_curso?.find((c) => c.id_clase === clase.id_clase);
		if (!actual) {
			console.error('No se pudo obtener la clase');
			return;
		}
		this.claseEditar = { ...actual };
	}

	nuevaClase() {
		if (this.curso.clases_curso) {
			this.claseEditar = new Clase(0, '', '', '', 0, '', this.curso.clases_curso?.length + 1, this.idCurso);
		}
	}

	/**
	 * Carga de imagen del curso
	 * @param event Evento de carga de imagen
	 */
	cargaImagenCurso(event: Event) {
		const input = event.target as HTMLInputElement;
		if (!input.files) {
			return;
		}
		const reader = new FileReader();
		reader.onload = (e: ProgressEvent<FileReader>) => {
			if (e.target && input.files && this.curso) {
				const formData = new FormData();
				formData.append('files', input.files[0], input.files[0].name);

				this.cursoService.addImagenCurso(formData).subscribe({
					next: (response) => {
						if (this.curso && response) this.curso.imagen_curso = response;
						this.compruebaCambios();
					},
					error: (e: Error) => {
						console.error('Error en añadir la imagen al curso: ' + e.message);
					},
				});
			}
		};
		reader.readAsDataURL(input.files[0]);
	}

	deleteCurso() {
		const confirm = globalThis.window.confirm('Esta acción borrará definitivamente este curso, incluida todas sus clases con su contenido. \n Tampoco el administrador de la plataforma podrá recuperar el curso una vez borrado.');
		if (confirm) {
			const confirm2 = globalThis.window.confirm('ESTÁS ABSOLUTAMENTE SEGURO DE LO QUE HACES??');
			if (confirm2 && this.curso) {
				this.cursoService.deleteCurso(this.curso).subscribe({
					next: (result: boolean) => {
						if (result) {
							this.router.navigate(['/cursosUsuario']);
						}
					},
					error: (e: Error) => {
						console.error('Error en eliminar el curso: ' + e.message);
					},
				});
			}
		}
	}

	// metodo para eliminar una clase desde la vista del curso
	eliminaClase(clase: Clase) {
		if (confirm('Esto eliminará definitivamente la clase. Estás seguro??')) {
			this.curso.clases_curso = this.curso.clases_curso?.filter((c) => c.id_clase !== clase.id_clase);
			this.cursoService.updateCurso(this.curso).subscribe({
				next: (success: Curso) => {
					if (!success) {
						console.error('Falló la actualización del curso en editor-clase');
					}
					this.initService.carga();
				},
				error: (error) => {
					console.error('Error al actualizar el curso: ' + error);
				},
			});
		}
	}

	async claseGuardada(event: boolean) {
		if (event) {
			if (this.idCurso === 0) {
				this.compruebaCambios();
				this.claseEditar = null;
				document.body.style.overflow = 'auto';
				return;
			}
			this.cursoService
				.getCurso(this.curso.id_curso, true)
				.then((curso) => {
					if (curso) {
						this.curso = curso;
						this.compruebaCambios();
						this.claseEditar = null;

						document.body.style.overflow = 'auto';
					} else {
						console.error('No se pudo obtener el curso');
					}
				})
				.catch((error) => {
					console.error('Error al obtener el curso:', error);
				});
		}
	}
}
