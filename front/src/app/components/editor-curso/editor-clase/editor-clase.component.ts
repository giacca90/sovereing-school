import { AfterViewInit, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationStart, Router } from '@angular/router';
import { firstValueFrom, Subscription } from 'rxjs';
import { VideoElement, WebOBS } from 'web-obs';
import { Clase } from '../../../models/Clase';
import { Curso } from '../../../models/Curso';
import { CursosService } from '../../../services/cursos.service';
import { InitService } from '../../../services/init.service';
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
export class EditorClaseComponent implements OnInit, AfterViewInit, OnDestroy {
	@Input() clase!: Clase;
	@Input() curso!: Curso;
	@Output() claseGuardada: EventEmitter<boolean> = new EventEmitter();
	private readonly subscriptions: Subscription[] = new Array<Subscription>();
	private readonly navControl: Subscription = new Subscription();
	private claseOriginal!: Clase;
	backBase = '';
	// Presets guardados para WebOBS
	savedPresets: Map<string, { elements: VideoElement[]; shortcut: string }> | null = null;
	// Archivos guardados para WebOBS
	savedFiles: File[] = [];

	readyService: boolean = false;
	readyComponent: boolean = false;
	constructor(
		private readonly cursoService: CursosService,
		public readonly streamingService: StreamingService,
		private readonly loginService: LoginService,
		private readonly initService: InitService,
		private readonly router: Router,
	) {}

	/**
	 * Inicializa el componente
	 *
	 * Maneja eventos de navegación y subscripciones a los componentes hijos
	 */
	ngOnInit() {
		this.navControl.add(
			this.router.events.subscribe((event) => {
				if (event instanceof NavigationStart) {
					if (JSON.stringify(this.claseOriginal) !== JSON.stringify(this.clase)) {
						const userConfirmed = globalThis.window.confirm('Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?');
						if (!userConfirmed) {
							return;
						}
					}
				}
			}),
		);
	}

	/**
	 * Inicializa el componente después de haber sido renderizado
	 *
	 * Sube la vista y clona la clase original
	 */
	ngAfterViewInit() {
		window.scrollTo(0, 0); // Subir la vista al inicio de la página
		document.body.style.overflow = 'hidden';
		this.claseOriginal = { ...this.clase };
	}

	/**
	 * Guarda los cambios realizados en la clase
	 *
	 * Valida si es necesario subir un video
	 *
	 * Actualiza la clase existente o crea una nueva
	 */
	async guardarCambiosClase(): Promise<void> {
		if (!this.confirmacion()) return;

		if (!this.validarVideo()) return;

		if (this.clase.id_clase === 0) {
			this.prepararNuevaClase();
		} else {
			this.actualizarClaseExistente();
		}

		await this.procesarClasePorTipo();
	}

	/**
	 * Envia la señal que cierra este componente
	 */
	close() {
		this.claseGuardada.emit(true);
	}

	/**
	 * Elimina una clase
	 * @param clase {Clase} Clase a eliminar
	 */
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

	/**
	 * Cambia el tipo de clase:
	 *
	 * Tipo de clase 0: Video estático
	 *
	 * Tipo de clase 1: OBS
	 *
	 * Tipo de clase 2: WebOBS
	 * @param tipo {number} Tipo de clase
	 */
	cambiaTipoClase(tipo: number) {
		this.streamingService.stopMediaStreaming();
		this.readyComponent = false;
		this.readyService = false;
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
					// WebOBS
					// Iniciamos la conexión WebSocket
					this.streamingService.startWebOBS();
					// Recuperar los presets del usuario
					if (this.savedPresets === null) {
						this.preparaWebcam();
						this.streamingService.getPresets().subscribe({
							next: (res) => {
								try {
									this.savedPresets = new Map(Object.entries(res));
								} catch (error) {
									console.error('Error al procesar presets:', error);
									this.savedPresets = new Map();
								}

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

	/**
	 * Destruye el componente
	 *
	 * Cancela las subscripciones y restaura el estado del componente
	 */
	ngOnDestroy() {
		this.navControl.unsubscribe();
		for (const subscription of this.subscriptions) {
			subscription.unsubscribe();
		}
		document.body.style.overflow = 'auto';
	}

	subeVideo(file: File) {
		this.streamingService.subeVideo(file, this.clase.curso_clase, this.clase.id_clase).subscribe((result) => {
			if (result) {
				this.clase.direccion_clase = result;

				// Actualizar botones
				const button = document.getElementById('video-upload-button') as HTMLSpanElement;
				const buttonGuardar = document.getElementById('button-guardar-clase') as HTMLButtonElement;
				button.classList.remove('border-gray-500', 'text-gray-500');
				button.classList.add('border-black');
				buttonGuardar.classList.remove('border-gray-500', 'text-gray-500');
				buttonGuardar.classList.add('border-black');
				buttonGuardar.disabled = false;

				this.readyComponent = true;
			}
		});
	}

	// Recuperar imagenes del curso y del usuario para el componente WebOBS
	/**
	 * Recupera las imagenes del curso y del usuario para el componente WebOBS
	 */
	async preparaWebcam() {
		if (this.curso?.imagen_curso) {
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

		const fotos = this.loginService.usuario?.foto_usuario ?? [];

		for (const url of fotos) {
			console.log('📸 Foto del usuario:', url);

			try {
				const response = await fetch(url, { credentials: 'include' });
				if (!response.ok) {
					console.warn(`⚠️ No se pudo descargar ${url}: ${response.statusText}`);
					continue;
				}

				const blob = await response.blob();
				const fileName = url.split('/').pop();
				const mimeType = blob.type || 'application/octet-stream';

				if (!fileName) continue;

				const yaExiste = this.savedFiles?.some((file) => file.name === fileName);
				if (!yaExiste) {
					const file = new File([blob], fileName, { type: mimeType });
					this.savedFiles?.push(file);
				}
			} catch (error) {
				console.error(`❌ Error al procesar ${url}:`, error);
			}
		}
	}

	/**
	 * Emite un video a través de WebOBS
	 *
	 * @param mediaStream {MediaStream | null} Stream de la webcam. Si es null indica que el stream a terminado
	 */
	emiteWebOBS(mediaStream: MediaStream | null) {
		// Puede ser la señal de que se acaba de emitir, o que no se ha seleccionado ninguna cámara
		if (mediaStream === null) {
			if (this.streamingService.emitiendo) {
				this.streamingService.detenerWebOBS();
				this.readyComponent = false;
				return;
			}
			alert('Debes conectarte primero con la webcam');
			this.readyComponent = false;
			return;
		}

		if (!this.confirmacion()) {
			this.readyComponent = false;
			return;
		}

		if (this.curso.id_curso == 0) {
			if (!confirm('El curso no existe. \nPara emitir en directo, primero hay que crear la clase\n¿Desea crear el curso con los datos actuales?')) {
				this.readyComponent = false;
				return;
			}
			if (!this.streamingService.streamId) return;
			this.clase.direccion_clase = this.streamingService.streamId;
			const clasesCurso = this.curso.clases_curso;
			if (!clasesCurso) {
				this.curso.clases_curso = new Array<Clase>();
			}
			if (this.curso.clases_curso) {
				this.clase.posicion_clase = this.curso.clases_curso.length + 1;
			}
			clasesCurso?.push(this.clase);
			this.cursoService.updateCurso(this.curso).subscribe({
				next: (success: Curso | null) => {
					if (!success) {
						console.error('Falló la actualización del curso en emitirOBS');
						return;
					}
					Object.assign(this.curso, success);

					this.readyComponent = true;

					this.streamingService.emitirWebOBS(mediaStream).catch((error) => {
						console.error('Error al emitir webcam:', error);
					});
				},
				error: (error) => {
					console.error('Falló la actualización del curso en emitirOBS:', error);
				},
			});
		}
	}

	obsEvent($event: { type: string; message: string }) {
		console.log('obsEvent: ', $event);
		const { type, message } = $event;

		try {
			switch (type) {
				case 'startOBS':
					this.streamingService.startOBS();
					break;

				case 'emiteOBS':
					this.emiteOBS(message);
					break;

				case 'stopOBS':
					this.streamingService.stopMediaStreaming();
					this.readyComponent = false;
					break;

				default:
					console.warn('Acción desconocida desde Editor-OBS:', $event);
			}
		} catch (err) {
			console.error('Error manejando evento obsEvent:', err);
		}
	}

	/**
	 * Emite un video a través de OBS
	 *
	 * @param streamUrl {string | null} URL del stream de la webcam.
	 */
	async emiteOBS(streamUrl: string | null) {
		// Puede ser la señal de que se acaba de emitir, o que no se ha recibido ninguna URL
		if (streamUrl === null) {
			console.error('No se pudo obtener la URL del servidor');
			this.readyComponent = false;
			return;
		}

		if (!this.confirmacion()) {
			this.readyComponent = false;
			return;
		}

		if (this.curso.id_curso == 0) {
			if (!confirm('El curso no existe. \nPara emitir en directo, primero hay que crear la clase\n¿Desea crear el curso con los datos actuales?')) {
				return;
			}
		}

		this.readyComponent = true;

		try {
			this.clase.direccion_clase = streamUrl;
			this.curso.clases_curso ??= [];

			this.clase.posicion_clase = this.curso.clases_curso.length + 1;
			const url = this.streamingService.rtmpUrl;
			if (url) {
				this.clase.direccion_clase = url.substring(url.lastIndexOf('/') + 1);
			}

			this.curso.clases_curso?.push(this.clase);
			this.cursoService.updateCurso(this.curso).subscribe({
				next: (success: Curso | null) => {
					if (!success) {
						console.error('Falló la actualización del curso en emitirOBS');
						return;
					}
					Object.assign(this.curso, success);
					this.streamingService.emitirOBS();
				},
				error: (error) => {
					console.error('Falló la actualización del curso en emitirOBS: ' + error);
				},
			});
		} catch (error) {
			console.error('Error al emitir OBS:', error);
		}
	}

	/** Guarda los presets de WebOBS */
	savePresets(data: Map<string, { elements: VideoElement[]; shortcut: string }>) {
		this.streamingService.savePresets(data);
	}

	/**
	 * función para confirmar que la clase está completa
	 * @returns {boolean} true si la clase está completa
	 */
	private confirmacion(): boolean {
		if (this.clase.nombre_clase == null || this.clase.nombre_clase == '') {
			alert('Debes poner un nombre para la clase');
			this.readyComponent = false;
			return false;
		}
		if (this.clase.descriccion_clase == null || this.clase.descriccion_clase == '') {
			alert('Debes poner una descripción para la clase');
			this.readyComponent = false;
			return false;
		}
		if (this.clase.contenido_clase == null || this.clase.contenido_clase == '') {
			alert('Debes poner contenido para la clase');
			this.readyComponent = false;
			return false;
		}
		return true;
	}

	/** Valida si es necesario subir un video
	 * @returns {boolean} true si hay video
	 */
	private validarVideo(): boolean {
		if (this.clase.id_clase === 0 && this.clase.tipo_clase === 0 && !this.readyComponent) {
			alert('Debes primero subir un video');
			return false;
		}
		return true;
	}

	/** Prepara la clase si es nueva */
	private prepararNuevaClase() {
		this.curso.clases_curso ??= [];
		this.clase.posicion_clase = this.curso.clases_curso.length + 1;
	}

	/** Actualiza la clase existente en el array */
	private actualizarClaseExistente() {
		const clasesCurso = this.curso.clases_curso ?? [];
		const idx = clasesCurso.findIndex((c) => c.id_clase === this.clase.id_clase);
		if (idx !== -1) {
			clasesCurso[idx] = { ...this.clase };
		}
	}

	/** Procesa la clase según su tipo */
	private async procesarClasePorTipo(): Promise<void> {
		if (this.clase.tipo_clase === 0) {
			if (this.clase.curso_clase === 0) {
				this.curso.clases_curso?.push(this.clase);
				this.close();
				return;
			}

			try {
				const success = await firstValueFrom(this.cursoService.updateCurso(this.curso));
				if (!success) {
					console.error('Falló la actualización del curso en editor-clase');
				}
				Object.assign(this.curso, success);
				this.close();
			} catch (error) {
				console.error('Error al actualizar el curso:', error);
			}
			return;
		}

		// Clase de tipo distinto a 0
		if (!this.readyService) {
			const actual = await this.cursoService.getCurso(this.curso.id_curso, true);
			Object.assign(this.curso, actual);
			this.close();
		}
	}
}
