import { AfterViewInit, Component, EventEmitter, Input, Output } from '@angular/core';
import Player from 'video.js/dist/types/player';
import { Clase } from '../../../../models/Clase';

@Component({
	selector: 'app-editor-video',
	imports: [],
	templateUrl: './editor-video.component.html',
	styleUrl: './editor-video.component.css',
})
export class EditorVideoComponent implements AfterViewInit {
	@Input() clase!: Clase;
	@Output() videoSeleccionado = new EventEmitter<File>();

	player: Player | null = null;
	backStream: string = '';

	constructor() {}

	ngAfterViewInit(): void {
		if (!this.clase?.direccionClase) return;

		console.log('📡 Cargando video desde:', this.clase.direccionClase);

		this.backStream = (globalThis.window as any).__env?.BACK_STREAM ?? '';
		const videoPlayer = document.getElementById('videoPlayer') as HTMLVideoElement;

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

		// Cambiar estilos de botones
		const button = document.getElementById('video-upload-button') as HTMLSpanElement;
		const buttonGuardar = document.getElementById('button-guardar-clase') as HTMLButtonElement;
		button.classList.remove('border-black');
		button.classList.add('border-gray-500', 'text-gray-500');
		buttonGuardar.classList.remove('border-black');
		buttonGuardar.classList.add('border-gray-500', 'text-gray-500');
		buttonGuardar.disabled = true;

		// Mostrar previsualización
		const reader = new FileReader();
		reader.onload = (e: ProgressEvent<FileReader>) => {
			const vid = document.getElementById('videoPlayer') as HTMLVideoElement;
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
			document.getElementById('video-upload')?.click();
		}
	}
}
