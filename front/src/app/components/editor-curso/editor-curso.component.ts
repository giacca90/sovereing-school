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
				this.idCurso = Number(params['idCurso']);
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
					if (this.curso.clasesCurso) {
						for (const clase of this.curso.clasesCurso) {
							clase.cursoClase = this.curso.idCurso;
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

		if (this.curso?.clasesCurso) {
			const draggedIndex = this.curso.clasesCurso.findIndex((clase) => clase.idClase === this.draggedElementId);
			const targetIndex = this.curso.clasesCurso.findIndex((clase) => clase.idClase === id);

			if (draggedIndex > -1 && targetIndex > -1 && draggedIndex !== targetIndex) {
				const draggedClase = this.curso.clasesCurso[draggedIndex];
				this.compruebaCambios();
				const temp: number = draggedClase.posicionClase;
				draggedClase.posicionClase = this.curso.clasesCurso[targetIndex].posicionClase;
				this.curso.clasesCurso[targetIndex].posicionClase = temp;
				this.curso.clasesCurso.splice(draggedIndex, 1);
				this.curso.clasesCurso.splice(targetIndex, 0, draggedClase);
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
		this.cursoService.getCurso(this.curso.idCurso).then((curso) => {
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
		const curso: Curso | null = await this.cursoService.getCurso(clase.cursoClase, true);
		Object.assign(this.curso, curso);
		if (!curso) {
			console.error('No se pudo obtener el curso');
			return;
		}
		const actual = curso.clasesCurso?.find((c) => c.idClase === clase.idClase);
		if (!actual) {
			console.error('No se pudo obtener la clase');
			return;
		}
		this.claseEditar = { ...actual };
	}

	nuevaClase() {
		if (this.curso.clasesCurso) {
			this.claseEditar = new Clase(0, '', '', '', 0, '', this.curso.clasesCurso?.length + 1, this.idCurso);
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
						if (this.curso && response) this.curso.imagenCurso = response;
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
			this.curso.clasesCurso = this.curso.clasesCurso?.filter((c) => c.idClase !== clase.idClase);
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
				.getCurso(this.curso.idCurso, true)
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
