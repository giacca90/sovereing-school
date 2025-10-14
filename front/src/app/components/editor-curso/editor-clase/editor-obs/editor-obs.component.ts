import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, EventEmitter, Inject, Input, OnDestroy, Output, PLATFORM_ID } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { StreamingService } from '../../../../services/streaming.service';

@Component({
	selector: 'app-editor-obs',
	imports: [],
	templateUrl: './editor-obs.component.html',
	styleUrl: './editor-obs.component.css',
})
export class EditorObsComponent implements AfterViewInit, OnDestroy {
	@Input() readyObserve?: Observable<boolean>; // Avisa cuando está listo para emitir (opcional)
	@Output() emision: EventEmitter<string | null> = new EventEmitter();
	m3u8Loaded: boolean = false;
	isBrowser: boolean;
	player: any;
	ready: boolean = false;
	rutaOBS: string = '';

	constructor(
		public streamingService: StreamingService,
		@Inject(PLATFORM_ID) private platformId: Object,
	) {
		this.isBrowser = isPlatformBrowser(platformId);
		this.readyObserve?.subscribe((res) => {
			this.ready = res;
		});
	}

	ngAfterViewInit(): void {
		if (this.isBrowser) {
			this.startOBS();
		}
	}

	ngOnDestroy(): void {
		this.player.dispose();
	}

	async emiteOBS() {
		if (!this.m3u8Loaded) {
			alert('Debes conectarte primero con OBS');
			return;
		}

		if (this.ready) {
			const url = await firstValueFrom(this.streamingService.rtmpUrl$);
			this.emision.emit(url);
		}
	}

	detenerEmision() {
		this.streamingService.stopMediaStreaming();
		this.player.src = '';
		this.player.srcObject = null;
		this.emision.emit(null);
	}

	async startOBS() {
		if (!this.isBrowser) return; // ✅ Evita ejecutar en SSR

		this.streamingService.startOBS();

		// Esperar a que el DOM esté listo
		setTimeout(async () => {
			const videoOBS = document.getElementById('OBS') as HTMLVideoElement;
			if (!videoOBS) {
				console.error('Elemento con ID "OBS" no encontrado');
				return;
			}

			// 📦 Importación dinámica
			const videojsModule = await import('video.js');
			const videojs = videojsModule.default;

			this.player = videojs(videoOBS, {
				aspectRatio: '16:9',
				controls: false,
				autoplay: true,
				preload: 'auto',
				muted: true,
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
				//console.log('Archivo .m3u8 cargado correctamente');
				this.m3u8Loaded = true;

				const techEl = this.player.tech(true)?.el() as HTMLVideoElement & { captureStream(): MediaStream };
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

			// Mostrar la URL RTMP al usuario
			const enlaces: HTMLDivElement = document.getElementById('enlaces') as HTMLDivElement;
			if (enlaces) {
				const server: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
				server.textContent = 'URL del servidor';
				const lServer: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
				lServer.classList.add('p-2', 'cursor-pointer', 'rounded-lg', 'border', 'border-black', 'text-blue-700');

				const url = await firstValueFrom(this.streamingService.rtmpUrl$);
				if (url) {
					lServer.textContent = url.substring(0, url.lastIndexOf('/'));
				} else {
					console.error('No se pudo obtener la URL del servidor');
					return;
				}
				lServer.onclick = () =>
					navigator.clipboard.writeText(lServer.textContent || '').then(() => {
						const tooltip = document.getElementById('tooltip') as HTMLDivElement;
						if (tooltip) {
							tooltip.textContent = 'Copiado al portapapeles';
							setTimeout(() => {
								tooltip.textContent = 'Haz click para copiar';
							}, 3000);
						}
					});
				// Crear tooltip dinámico para lServer
				lServer.addEventListener('mouseover', (event: MouseEvent) => {
					const tooltip = document.createElement('div');
					tooltip.id = 'tooltip';
					tooltip.textContent = 'Haz click para copiar';
					tooltip.classList.add('absolute', 'bg-black', 'text-white', 'text-xs', 'p-1', 'rounded-sm', 'tooltip');
					// Posicionar el tooltip basado en la posición del ratón
					tooltip.style.position = 'fixed'; // Usamos 'fixed' para que funcione con las coordenadas del mouse
					tooltip.style.top = `${event.clientY - 30}px`; // Ajustar posición encima del ratón
					tooltip.style.left = `${event.clientX}px`; // Alinear con el puntero del ratón
					enlaces.appendChild(tooltip);
				});

				lServer.addEventListener('mousemove', (event: MouseEvent) => moveTooltip(event));

				lServer.addEventListener('mouseleave', () => {
					const tooltip = document.getElementById('tooltip') as HTMLDivElement;
					if (tooltip) {
						tooltip.remove();
					}
				});

				const key: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
				key.textContent = 'Clave del stream';
				const lKey: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
				lKey.classList.add('p-2', 'cursor-pointer', 'rounded-lg', 'border', 'border-black', 'text-blue-700');

				lKey.textContent = url.substring(0, url.lastIndexOf('/'));

				lKey.onclick = () =>
					navigator.clipboard.writeText(lKey.textContent || '').then(() => {
						const tooltip = document.getElementById('tooltip') as HTMLDivElement;
						if (tooltip) {
							tooltip.textContent = 'Copiado al portapapeles';
							setTimeout(() => {
								tooltip.textContent = 'Haz click para copiar';
							}, 3000);
						}
					});
				lKey.addEventListener('mouseover', (event: MouseEvent) => {
					const tooltip = document.createElement('div');
					tooltip.id = 'tooltip';
					tooltip.textContent = 'Haz click para copiar';
					tooltip.classList.add('absolute', 'bg-black', 'text-white', 'text-xs', 'p-1', 'rounded-sm', 'tooltip');
					// Posicionar el tooltip basado en la posición del ratón
					tooltip.style.position = 'fixed'; // Usamos 'fixed' para que funcione con las coordenadas del mouse
					tooltip.style.top = `${event.clientY - 40}px`; // Ajustar posición encima del ratón
					tooltip.style.left = `${event.clientX}px`; // Alinear con el puntero del ratón
					enlaces.appendChild(tooltip);
				});

				lKey.addEventListener('mousemove', (event: MouseEvent) => moveTooltip(event));

				lKey.addEventListener('mouseleave', () => {
					const tooltip = document.getElementById('tooltip') as HTMLDivElement;
					if (tooltip) {
						tooltip.remove();
					}
				});

				// Mover el tooltip con el ratón mientras esté en el elemento
				const moveTooltip = (moveEvent: MouseEvent) => {
					const tooltip = document.getElementById('tooltip') as HTMLDivElement;
					tooltip.style.top = `${moveEvent.clientY - 30}px`;
					tooltip.style.left = `${moveEvent.clientX}px`;
				};
				enlaces.appendChild(server);
				enlaces.appendChild(lServer);
				enlaces.appendChild(key);
				enlaces.appendChild(lKey);
			}
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
