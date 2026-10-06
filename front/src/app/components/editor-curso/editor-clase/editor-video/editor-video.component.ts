import { AfterViewInit, Component, ElementRef, EventEmitter, Input, Output, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import Player from 'video.js/dist/types/player';
import { Clase } from '../../../../models/Clase';

@Component({
	selector: 'app-editor-video',
	imports: [],
	templateUrl: './editor-video.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './editor-video.component.css',
})
export class EditorVideoComponent implements AfterViewInit {
	@Input() clase!: Clase;
	@Input() isUploading: boolean = false;
	@Output() videoSeleccionado = new EventEmitter<File>();

	@ViewChild('videoPlayer') videoPlayerRef!: ElementRef<HTMLVideoElement>;
	@ViewChild('videoUploadButton') videoUploadButtonRef!: ElementRef<HTMLSpanElement>;
	@ViewChild('videoUploadInput') videoUploadInputRef!: ElementRef<HTMLInputElement>;

	player: Player | null = null;
	backStream: string = '';

	/**
	 * Constructor del componente.
	 */
	constructor() {}

	/**
	 * Inicializa el reproductor de video después de que la vista ha sido inicializada.
	 */
	ngAfterViewInit(): void {
		if (!this.clase?.direccionClase) return;

		console.log('📡 Cargando video desde:', this.clase.direccionClase);

		this.backStream = (globalThis.window as any).__env?.BACK_STREAM ?? '';
		const videoPlayer = this.videoPlayerRef.nativeElement;

		if (!videoPlayer) {
			console.error('No se pudo obtener el elemento videoPlayer');
			return;
		}

		// Importar Video.js y plugin de calidad
		import('video.js')
			.then((videojsModule) => {
				const videojs = videojsModule.default;
				return import('videojs-contrib-quality-levels').then(() => videojs);
			})
			.then((videojs) => {
				// Limpiar player anterior si existe
				if (this.player) {
					this.player.dispose();
					this.player = null;
				}

				// Inicializar nuevo player
				this.player = videojs(videoPlayer, {
					controls: true,
					autoplay: true,
					preload: 'auto',
					techOrder: ['html5'],
					html5: { vhs: { withCredentials: true } },
				});

				// Asignar fuente HLS
				const srcUrl = `${this.backStream}/${this.clase.cursoClase}/${this.clase.idClase}/master.m3u8`;
				this.player.src({
					src: srcUrl,
					type: 'application/x-mpegURL',
					withCredentials: true,
				});
			})
			.catch((err) => {
				console.error('Error al inicializar el video:', err);
			});
	}

	/**
	 * 📤 Carga un video, muestra previsualización y emite el archivo al padre
	 */
	cargaVideo(event: Event) {
		const input = event.target as HTMLInputElement;
		if (!input.files || input.files.length === 0) {
			alert('¡Sube un video válido!');
			return;
		}

		const file = input.files[0];

		// Mostrar previsualización
		const reader = new FileReader();
		reader.onload = (e: ProgressEvent<FileReader>) => {
			const vid = this.videoPlayerRef.nativeElement;
			if (e.target?.result) {
				vid.src = e.target.result as string;
			}

			// Asegurar que la clase tenga id inicial
			if (this.clase && !this.clase.idClase) {
				this.clase.idClase = 0;
			}

			// ✅ Emitir el archivo al padre
			this.videoSeleccionado.emit(file);
		};

		reader.readAsDataURL(file);
	}

	keyEvent(event: KeyboardEvent) {
		if (event.key === 'Enter') {
			this.videoUploadInputRef.nativeElement.click();
		}
	}
}
