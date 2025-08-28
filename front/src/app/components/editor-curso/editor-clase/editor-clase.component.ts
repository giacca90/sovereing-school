import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, EventEmitter, Inject, Input, OnDestroy, Output, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationStart, Router } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { VideoElement, WebOBS } from 'web-obs';
import { Clase } from '../../../models/Clase';
import { Curso } from '../../../models/Curso';
import { ClaseService } from '../../../services/clase.service';
import { CursosService } from '../../../services/cursos.service';
import { LoginService } from '../../../services/login.service';
import { StreamingService } from '../../../services/streaming.service';
import { EditorObsComponent } from './editor-obs/editor-obs.component';
import { EditorVideoComponent } from './editor-video/editor-video.component';

@Component({
	selector: 'app-editor-clase',
	imports: [FormsModule, EditorObsComponent, EditorVideoComponent, WebOBS],
	templateUrl: './editor-clase.component.html',
	styleUrl: './editor-clase.component.css',
})
export class EditorClaseComponent implements AfterViewInit, OnDestroy {
	@Input() clase!: Clase;
	@Input() curso!: Curso;
	@Output() claseGuardada: EventEmitter<boolean> = new EventEmitter();
	private subscription: Subscription = new Subscription();
	private claseOriginal!: Clase;
	//streamWebcam: MediaStream | null = null;
	savedPresets: Map<string, { elements: VideoElement[]; shortcut: string }> | null = null;
	isBrowser: boolean;
	backBase = '';
	savedFiles: File[] = [];
	readyObserver: Subject<boolean> = new Subject<boolean>();
	readyEstatico: boolean = false;
	constructor(
		private claseService: ClaseService,
		private cursoService: CursosService,
		private streamingService: StreamingService,
		private loginService: LoginService,
		private router: Router,
		@Inject(PLATFORM_ID) private platformId: Object,
	) {
		this.isBrowser = isPlatformBrowser(platformId);
	}

	ngOnInit(): void {
		this.subscription.add(
			this.router.events.subscribe((event) => {
				if (event instanceof NavigationStart) {
					if (JSON.stringify(this.claseOriginal) !== JSON.stringify(this.clase)) {
						const userConfirmed = window.confirm('Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?');
						if (!userConfirmed) {
							return;
						}
					}
				}
			}),
		);
	}

	ngAfterViewInit(): void {
		window.scrollTo(0, 0); // Subir la vista al inicio de la página
		document.body.style.overflow = 'hidden';
		this.claseOriginal = { ...this.clase };
	}

	guardarCambiosClase() {
		if (this.clase.nombre_clase == null || this.clase.nombre_clase == '') {
			alert('Debes poner un nombre para la clase');
			return;
		}
		if (this.clase.descriccion_clase == null || this.clase.descriccion_clase == '') {
			alert('Debes poner una descripción para la clase');
			return;
		}
		if (this.clase.contenido_clase == null || this.clase.contenido_clase == '') {
			alert('Debes poner contenido para la clase');
			return;
		}
		if (this.clase.id_clase === 0 && this.clase.tipo_clase === 0) {
			if (!this.readyEstatico) {
				alert('Debes primero subir un video');
				return;
			}
		}

		let idx: number;
		if (this.clase.id_clase === 0) {
			const clasesCurso = this.curso.clases_curso;
			if (!clasesCurso) {
				this.curso.clases_curso = new Array<Clase>();
			}
			if (this.curso.clases_curso) {
				this.clase.posicion_clase = this.curso.clases_curso.length + 1;
				idx = this.curso.clases_curso.push(this.clase) - 1;
			}
		} else {
			const clasesCurso = this.curso.clases_curso;
			if (clasesCurso) {
				idx = clasesCurso.findIndex((clase) => clase.id_clase === this.clase.id_clase);
				if (idx !== -1) {
					clasesCurso[idx] = { ...this.clase };
				}
			}
		}

		// TODO: quitar esto cuando se arregle el problema de que el usuario puede cambiar el curso de una clase
		this.clase.curso_clase = Number(this.clase.curso_clase);
		if (this.clase.tipo_clase === 0) {
			if (this.clase.curso_clase === 0) {
				this.close();
				return;
			}
			this.cursoService.updateCurso(this.curso).subscribe({
				next: (success: Curso) => {
					if (!success) {
						console.error('Falló la actualización del curso');
					}
					this.close();
				},
				error: (error) => {
					console.error('Error al actualizar el curso:', error);
				},
			});
		}

		if (this.clase.tipo_clase !== 0 && !this.streamingService.enGrabacion) {
			this.close();
		}
	}

	close() {
		this.claseGuardada.emit(true);
	}

	eliminaClase(clase: Clase) {
		if (confirm('Esto eliminará definitivamente la clase. Estás seguro??')) {
			this.subscription.add(
				this.claseService.deleteClase(clase).subscribe({
					next: (resp: boolean) => {
						if (!resp) {
							alert('Error al eliminar la clase');
							return;
						}
					},
					error: (e: Error) => {
						console.error('Error en eliminar Clase: ' + e.message);
					},
				}),
			);
		}
	}

	cambiaTipoClase(tipo: number) {
		this.streamingService.closeConnections();
		setTimeout(() => {
			if (!this.clase) return;
			const videoButton: HTMLButtonElement = document.getElementById('claseVideo') as HTMLButtonElement;
			const obsButton: HTMLButtonElement = document.getElementById('claseOBS') as HTMLButtonElement;
			const webcamButton: HTMLButtonElement = document.getElementById('claseWebCam') as HTMLButtonElement;
			if (videoButton) {
				videoButton.classList.remove('text-blue-700');
			}
			if (obsButton) {
				obsButton.classList.remove('text-blue-700');
			}
			if (webcamButton) {
				webcamButton.classList.remove('text-blue-700');
			}
			window.scrollTo(0, 0); // Subir la vista al inicio de la página
			document.body.style.overflow = 'hidden';

			//this.player?.dispose();
			//this.player = null;
			switch (tipo) {
				case 0: {
					// Video estatico
					this.clase.tipo_clase = 0;
					videoButton.classList.add('text-blue-700');
					break;
				}
				case 1: {
					// OBS
					this.clase.tipo_clase = 1;
					obsButton.classList.add('text-blue-700');
					break;
				}
				case 2: {
					// Webcam
					// Recuperar los presets del usuario
					if (this.savedPresets === null) {
						this.preparaWebcam();
					}
					if (this.savedPresets === null) {
						this.streamingService.getPresets().subscribe({
							next: (res) => {
								try {
									console.log('Presets recibidos:', res);
									this.savedPresets = new Map(Object.entries(res));
								} catch (error) {
									console.error('Error al parsear los presets:', error);
									this.savedPresets = new Map();
								}
								console.log('Presets actualizados:', this.savedPresets);
								if (this.clase) this.clase.tipo_clase = 2;
								webcamButton.classList.add('text-blue-700');
							},
							error: (error) => {
								console.error('Error al obtener presets:', error);
								this.savedPresets = new Map();
								if (this.clase) this.clase.tipo_clase = 2;
								webcamButton.classList.add('text-blue-700');
							},
						});
					} else {
						this.clase.tipo_clase = 2;
						webcamButton.classList.add('text-blue-700');
					}
				}
			}
		}, 100);
	}

	ngOnDestroy(): void {
		this.subscription.unsubscribe();
		document.body.style.overflow = 'auto';
	}

	readyEvent($event: boolean) {
		this.readyEstatico = $event;
	}

	// Recuperar imagenes del curso y del usuario para el componente WebOBS
	async preparaWebcam() {
		if (this.curso && this.curso.imagen_curso) {
			fetch(this.curso.imagen_curso, { credentials: 'include' }).then((response) => {
				response.blob().then((blob) => {
					if (!this.curso) return;
					const fileName = this.curso.imagen_curso.split('/').pop();
					// Detectar el tipo MIME del Blob
					const mimeType = blob.type || 'application/octet-stream';
					if (fileName) {
						const test = this.savedFiles?.find((file) => file.name === fileName);
						if (!test) {
							const file = new File([blob], fileName, { type: mimeType });
							this.savedFiles?.push(file);
						}
					}
				});
			});
		}

		this.loginService.usuario?.foto_usuario.forEach((foto) => {
			console.log('Foto del usuario:', foto);
			fetch(foto, { credentials: 'include' }).then((response) => {
				response.blob().then((blob) => {
					const fileName = foto.split('/').pop();
					const mimeType = blob.type || 'application/octet-stream';
					if (fileName) {
						const test = this.savedFiles?.find((file) => file.name === fileName);
						if (!test) {
							const file = new File([blob], fileName, { type: mimeType });
							this.savedFiles?.push(file);
						}
					}
				});
			});
		});
	}

	emiteWebcam(event: MediaStream | null) {
		if (this.clase) {
			if (event === null) {
				this.streamingService.stopMediaStreaming();
				this.readyObserver.next(false);
				return;
			}
			if (this.clase.nombre_clase == null || this.clase.nombre_clase == '') {
				alert('Debes poner un nombre para la clase');
				this.readyObserver.next(false);
				return;
			}
			if (this.clase.descriccion_clase == null || this.clase.descriccion_clase == '') {
				alert('Debes poner una descripción para la clase');
				this.readyObserver.next(false);
				return;
			}
			if (this.clase.contenido_clase == null || this.clase.contenido_clase == '') {
				alert('Debes poner contenido para la clase');
				this.readyObserver.next(false);
				return;
			}
			if (event === null) {
				alert('Debes conectarte primero con la webcam');
				this.readyObserver.next(false);
				return;
			} else {
				this.readyObserver.next(true);
				this.streamingService.emitirWebcam(event, this.clase);
			}
		}
	}

	savePresets(data: any) {
		this.streamingService.savePresets(data);
	}
}
