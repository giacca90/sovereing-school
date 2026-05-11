import { AfterViewInit, Component, EventEmitter, Input, OnDestroy, OnInit, Output, Renderer2 } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationStart, Router } from '@angular/router';
import { firstValueFrom, Subscription } from 'rxjs';
import { Preset, WebOBS } from 'web-obs';
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
	savedPresets: Map<string, Preset> | null = null;
	// Archivos guardados para WebOBS
	savedFiles: File[] = [];

	readyService: boolean = false;
	readyComponent: boolean = false;
	isUploadingVideo: boolean = false;
	/**
	 * Constructor del componente.
	 * @param {CursosService} cursoService - Servicio de cursos.
	 * @param {StreamingService} streamingService - Servicio de streaming.
	 * @param {LoginService} loginService - Servicio de autenticación.
	 * @param {InitService} initService - Servicio de inicialización.
	 * @param {Router} router - Router de Angular.
	 * @param {Renderer2} renderer - Renderer2 de Angular.
	 */
	constructor(
		private readonly cursoService: CursosService,
		public readonly streamingService: StreamingService,
		private readonly loginService: LoginService,
		private readonly initService: InitService,
		private readonly router: Router,
		private readonly renderer: Renderer2,
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
		globalThis.window?.scrollTo(0, 0); // Subir la vista al inicio de la página
		this.renderer.setStyle(document.body, 'overflow', 'hidden');
		this.claseOriginal = { ...this.clase };
	}

	/**
	 * Guarda los cambios realizados en la clase.
	 *
	 * Valida si es necesario subir un video.
	 *
	 * Actualiza la clase existente o crea una nueva.
	 */
	async guardarCambiosClase(): Promise<void> {
		if (!this.confirmacion()) return;

		if (!this.validarVideo()) return;

		if (this.clase.idClase === 0) {
			this.prepararNuevaClase();
		} else {
			this.actualizarClaseExistente();
		}

		await this.procesarClasePorTipo();
	}

	/**
	 * Valida si hay cambios sin guardar antes de salir.
	 * @returns {boolean} True si se puede continuar, false en caso contrario.
	 */
	confirmacion(): boolean {
		if (this.clase.nombreClase == null || this.clase.nombreClase == '') {
			alert('Debes poner un nombre para la clase');
			this.readyComponent = false;
			return false;
		}
		if (this.clase.descripcionClase == null || this.clase.descripcionClase == '') {
			alert('Debes poner una descripción para la clase');
			this.readyComponent = false;
			return false;
		}
		if (this.clase.contenidoClase == null || this.clase.contenidoClase == '') {
			alert('Debes poner contenido para la clase');
			this.readyComponent = false;
			return false;
		}
		return true;
	}

	/**
	 * Valida si el video es válido.
	 * @returns {boolean} True si es válido, false en caso contrario.
	 */
	validarVideo(): boolean {
		if (this.clase.idClase === 0 && this.clase.tipoClase === 0 && !this.readyComponent) {
			alert('Debes primero subir un video');
			return false;
		}
		return true;
	}

	/**
	 * Prepara una nueva clase para ser guardada.
	 */
	prepararNuevaClase(): void {
		this.curso.clasesCurso ??= [];
		this.clase.posicionClase = this.curso.clasesCurso.length + 1;
	}

	/**
	 * Actualiza una clase existente.
	 */
	actualizarClaseExistente(): void {
		const clasesCurso = this.curso.clasesCurso ?? [];
		const idx = clasesCurso.findIndex((c) => c.idClase === this.clase.idClase);
		if (idx !== -1) {
			clasesCurso[idx] = { ...this.clase };
		}
	}

	/**
	 * Procesa la clase según su tipo.
	 */
	async procesarClasePorTipo(): Promise<void> {
		if (this.clase.tipoClase === 0) {
			this.curso.clasesCurso?.push(this.clase);
			if (this.clase.cursoClase === 0) {
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
			const actual = await this.cursoService.getCurso(this.curso.idCurso, true);
			Object.assign(this.curso, actual);
			this.close();
		}
	}

	/**
	 * Envia la señal que cierra este componente.
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
			this.curso.clasesCurso = this.curso.clasesCurso?.filter((c) => c.idClase !== clase.idClase);
			this.subscriptions.push(
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
				}),
			);
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
		if (!this.clase) return;
		globalThis.window?.scrollTo(0, 0); // Subir la vista al inicio de la página
		this.renderer.setStyle(document.body, 'overflow', 'hidden');

		switch (tipo) {
			case 0: {
				// Video estatico
				this.clase.tipoClase = 0;
				break;
			}
			case 1: {
				// OBS
				this.clase.tipoClase = 1;
				break;
			}
			case 2: {
				// WebOBS
				// Iniciamos la conexión WebSocket
				this.streamingService.startWebOBS();
				// Recuperar los presets del usuario
				if (this.savedPresets === null) {
					this.preparaWebcam();
					this.subscriptions.push(
						this.streamingService.getPresets().subscribe({
							next: (res) => {
								try {
									this.savedPresets = new Map(Object.entries(res));
								} catch (error) {
									console.error('Error al procesar presets:', error);
									this.savedPresets = new Map();
								}

								if (this.clase) this.clase.tipoClase = 2;
							},
							error: (error) => {
								console.error('Error al obtener presets:', error);
								this.savedPresets = new Map();
								if (this.clase) this.clase.tipoClase = 2;
							},
						}),
					);
				} else {
					this.clase.tipoClase = 2;
				}
			}
		}
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
		this.renderer.setStyle(document.body, 'overflow', 'auto');
	}

	subeVideo(file: File) {
		this.isUploadingVideo = true;
		this.subscriptions.push(
			this.streamingService.subeVideo(file, this.clase.cursoClase, this.clase.idClase).subscribe((result) => {
				if (result) {
					this.clase.direccionClase = result;
					this.isUploadingVideo = false;
					this.readyComponent = true;
				}
			}),
		);
	}

	/**
	 * Recupera las imagenes del curso y del usuario para el componente WebOBS
	 */
	async preparaWebcam() {
		if (this.curso?.imagenCurso) {
			fetch(this.curso.imagenCurso, { credentials: 'include' }).then((response) => {
				response.blob().then((blob) => {
					if (!this.curso) return;
					const fileName = this.curso.imagenCurso.split('/').pop();
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

		const fotos = this.loginService.usuario?.fotoUsuario ?? [];

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

		if (this.curso.idCurso == 0) {
			if (!confirm('El curso no existe. \nPara emitir en directo, primero hay que crear la clase\n¿Desea crear el curso con los datos actuales?')) {
				this.readyComponent = false;
				return;
			}
			if (!this.streamingService.streamId) return;
			this.clase.direccionClase = this.streamingService.streamId;
			const clasesCurso = this.curso.clasesCurso;
			if (!clasesCurso) {
				this.curso.clasesCurso = new Array<Clase>();
			}
			if (this.curso.clasesCurso) {
				this.clase.posicionClase = this.curso.clasesCurso.length + 1;
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

		if (this.curso.idCurso == 0) {
			if (!confirm('El curso no existe. \nPara emitir en directo, primero hay que crear la clase\n¿Desea crear el curso con los datos actuales?')) {
				return;
			}
		}

		this.readyComponent = true;

		try {
			this.clase.direccionClase = streamUrl;
			this.curso.clasesCurso ??= [];

			this.clase.posicionClase = this.curso.clasesCurso.length + 1;
			const url = this.streamingService.rtmpUrl;
			if (url) {
				this.clase.direccionClase = url.substring(url.lastIndexOf('/') + 1);
			}

			this.curso.clasesCurso?.push(this.clase);
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
	savePresets(data: Map<string, Preset>) {
		this.streamingService.savePresets(data);
	}
}
