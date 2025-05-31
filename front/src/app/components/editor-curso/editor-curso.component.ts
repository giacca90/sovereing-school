import { isPlatformBrowser } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, NavigationStart, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { Clase } from '../../models/Clase';
import { Curso } from '../../models/Curso';
import { ClaseService } from '../../services/clase.service';
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
export class EditorCursoComponent implements OnInit, OnDestroy {
	private subscription: Subscription = new Subscription();
	idCurso!: number;
	curso!: Curso;
	backBase!: string;
	draggedElementId: number | null = null;
	editado: boolean = false;
	claseEditar: Clase | null = null;
	isBrowser: boolean;
	//editar: Clase | null = null;
	//streamWebcam: MediaStream | null = null;
	//m3u8Loaded: boolean = false;
	//player: Player | null = null;
	//ready: Subject<boolean> = new Subject<boolean>();
	//savedFiles: File[] = [];
	//savedPresets: Map<string, { elements: VideoElement[]; shortcut: string }> | null = null;
	//videojs: any;

	constructor(
		private route: ActivatedRoute,
		private router: Router,
		public cursoService: CursosService,
		private claseService: ClaseService,
		public loginService: LoginService,
		public streamingService: StreamingService,
		private initService: InitService,
		@Inject(PLATFORM_ID) private platformId: Object,
	) {
		this.subscription.add(
			this.route.params.subscribe((params) => {
				this.idCurso = params['id_curso'];
			}),
		);
		this.isBrowser = isPlatformBrowser(platformId);
	}

	async ngOnInit() {
		if (this.idCurso === 0 && this.loginService.usuario) {
			this.curso = new Curso(0, '', [this.loginService.usuario], '', '', new Date(), [], [], '', 0);
		} else {
			await this.cursoService.getCurso(this.idCurso).then((curso) => {
				this.curso = JSON.parse(JSON.stringify(curso));
				if (!this.curso) {
					console.log('Curso no encontrado: ' + this.idCurso);
					this.router.navigate(['/']);
				}
				this.curso.clases_curso?.forEach((clase) => {
					clase.curso_clase = this.curso.id_curso;
				});
			});
		}

		this.subscription.add(
			this.router.events.subscribe((event) => {
				if (event instanceof NavigationStart && this.editado) {
					this.cursoService.getCurso(this.curso.id_curso).then((curso) => {
						if (curso) {
							this.curso = curso;
						}
					});
					const userConfirmed = window.confirm('Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?');
					if (!userConfirmed) {
						this.router.navigateByUrl(this.router.url); // Mantén al usuario en la misma página
					}
				}
			}),
		);

		if (isPlatformBrowser(this.platformId)) {
			this.backBase = (window as any).__env?.BACK_BASE ?? '';
		}
	}

	ngOnDestroy(): void {
		this.subscription.unsubscribe();
	}

	/* @HostListener('window:beforeunload', ['$event'])
	unloadNotification($event: { returnValue: string }): void {
		if (!this.isBrowser) return;

		if (this.editado) {
			$event.returnValue = 'Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?';
		}
	} */

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
			document.body.removeChild(img);
		}, 0);
		div.classList.add('opacity-0');
	}

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
		return closestElement ? parseInt(closestElement.id.split('-')[1], 10) : null;
	}

	onDragEnd(event: Event): void {
		const event2 = event as DragEvent;
		(event2.target as HTMLDivElement).classList.remove('opacity-0');
		this.draggedElementId = null;
	}

	compruebaCambios() {
		this.cursoService.getCurso(this.curso.id_curso).then((curso) => {
			this.editado = JSON.stringify(this.curso) !== JSON.stringify(curso);
		});
	}

	updateCurso() {
		this.subscription.add(
			this.cursoService.updateCurso(this.curso).subscribe({
				next: (success: boolean) => {
					if (success) {
						this.initService.carga();
						this.editado = false;
						this.router.navigate(['/cursosUsuario']);
					} else {
						console.error('Falló la actualización del curso');
					}
				},
				error: (error) => {
					console.error('Error al actualizar el curso:', error);
				},
			}),
		);
	}

	editarClase(clase: Clase) {
		this.claseEditar = { ...clase };
	}

	nuevaClase() {
		if (this.curso.clases_curso) {
			this.claseEditar = new Clase(0, '', '', '', 0, '', this.curso.clases_curso?.length + 1, this.idCurso);
		}
	}

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
		const confirm = window.confirm('Esta acción borrará definitivamente este curso, incluida todas sus clases con su contenido. \n Tampoco el administrador de la plataforma podrá recuperar el curso una vez borrado.');
		if (confirm) {
			const confirm2 = window.confirm('ESTÁS ABSOLUTAMENTE SEGURO DE LO QUE HACES??');
			if (confirm2 && this.curso) {
				this.cursoService.deleteCurso(this.curso).subscribe({
					next: (result: boolean) => {
						if (result) {
							this.initService.carga();
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
			this.subscription.add(
				this.claseService.deleteClase(clase).subscribe({
					next: (resp: boolean) => {
						if (!resp) {
							alert('Error al eliminar la clase');
							return;
						}
						this.curso.clases_curso = this.curso.clases_curso?.filter((c) => c.id_clase !== clase.id_clase);
						this.initService.carga();
					},
					error: (e: Error) => {
						console.error('Error en eliminar Clase: ' + e.message);
					},
				}),
			);
		}
	}

	async claseGuardada(event: boolean) {
		if (event) {
			console.log('ID Curso:', this.idCurso);
			this.cursoService
				.getCurso(this.idCurso, true)
				.then((curso) => {
					console.log('Curso actualizado:', curso);
					if (curso) {
						this.curso = curso;
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
