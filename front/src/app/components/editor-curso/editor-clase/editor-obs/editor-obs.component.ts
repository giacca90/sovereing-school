import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, Inject, Input, PLATFORM_ID } from '@angular/core';
import { Observable } from 'rxjs';
import videojs from 'video.js';
import Player from 'video.js/dist/types/player';
import { LoginService } from '../../../../services/login.service';
import { StreamingService } from '../../../../services/streaming.service';

@Component({
	selector: 'app-editor-obs',
	imports: [],
	templateUrl: './editor-obs.component.html',
	styleUrl: './editor-obs.component.css',
})
export class EditorObsComponent implements AfterViewInit {
	@Input() readyObserve?: Observable<boolean>;
	m3u8Loaded: boolean = false;
	isBrowser: boolean;
	player: Player | null = null;
	videojs = videojs;

	constructor(
		public streamingService: StreamingService,
		private loginService: LoginService,
		@Inject(PLATFORM_ID) private platformId: Object,
	) {
		this.isBrowser = isPlatformBrowser(platformId);
	}
	ngAfterViewInit(): void {
		if (this.isBrowser) {
			//this.videojs = require('video.js'); // 👈 importante: cargarlo dinámicamente
			this.startOBS();
		}
	}

	emiteOBS() {
		if (!this.m3u8Loaded) {
			alert('Debes conectarte primero con OBS');
			return;
		}
	}

	detenerEmision() {
		this.streamingService.stopMediaStreaming();
	}

	async startOBS() {
		if (!this.isBrowser) return; // ✅ Evita ejecutar en SSR

		const userId = this.loginService.usuario?.id_usuario;
		if (!userId) return;

		this.streamingService.startOBS(userId);

		// Esperar a que el DOM esté listo
		setTimeout(async () => {
			const videoOBS = document.getElementById('OBS') as HTMLVideoElement;
			if (!videoOBS) {
				console.error('Elemento con ID "OBS" no encontrado');
				return;
			}

			// ✅ Importar dinámicamente video.js
			const videojsModule = await import('video.js');
			const videojs = videojsModule.default;

			this.player = videojs(videoOBS, {
				aspectRatio: '16:9',
				controls: false,
				autoplay: true,
				preload: 'auto',
				html5: {
					hls: {
						overrideNative: true,
						enableLowLatency: true,
					},
					vhs: {
						lowLatencyMode: true,
					},
				},
				liveui: true,
			});

			this.player.src({
				src: this.streamingService.UrlPreview,
				type: 'application/x-mpegURL',
				withCredentials: true,
			});

			this.player.on('loadeddata', () => {
				console.log('Archivo .m3u8 cargado correctamente');
				this.m3u8Loaded = true;

				const techEl = this.player?.tech(true)?.el() as HTMLVideoElement & { captureStream(): MediaStream };
				if (techEl?.captureStream) {
					const mediaStream = techEl.captureStream();
					const audioLevel = document.getElementById('audio-level') as HTMLDivElement;
					if (audioLevel) {
						this.visualizeAudio(mediaStream, audioLevel);
					}
				}
			});

			// ✅ Asegura el estilo solo si el elemento existe
			videoOBS.style.height = 'auto'; // Usar 'auto' en lugar de 'content' (no válido en CSS)
		}, 300);
	}

	visualizeAudio(stream: MediaStream, audioLevel: HTMLDivElement) {
		const audioContext = new AudioContext();
		const analyser = audioContext.createAnalyser();
		const source = audioContext.createMediaStreamSource(stream);

		source.connect(analyser);

		analyser.fftSize = 256; // Ajusta la resolución de frecuencia
		const dataArray = new Uint8Array(analyser.frequencyBinCount);

		function updateAudioLevel() {
			analyser.getByteFrequencyData(dataArray);
			const volume = Math.max(...dataArray) / 255; // Escalar de 0 a 1
			const percentage = Math.min(volume * 100, 100); // Limitar a 100%
			audioLevel.style.width = `${percentage}%`; // Ajustar ancho de la barra

			requestAnimationFrame(updateAudioLevel); // Continuar la animación
		}

		updateAudioLevel(); // Iniciar la visualización
	}
}
