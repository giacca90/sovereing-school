import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, EventEmitter, Inject, Input, OnDestroy, Output, PLATFORM_ID } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { StreamingService } from '../../../../services/streaming.service';

@Component({
	selector: 'app-editor-obs',
	imports: [],
	templateUrl: './editor-obs.component.html',
	styleUrl: './editor-obs.component.css',
})
export class EditorObsComponent implements AfterViewInit, OnDestroy {
	@Input() ready?: boolean; // Avisa cuando está listo para emitir (opcional)
	@Input() status?: string; // Estado del servicio de streaming (opcional)
	@Output() emision: EventEmitter<string | null> = new EventEmitter();
	m3u8Loaded: boolean = false;
	isBrowser: boolean;
	player: any;
	rutaOBS: string = '';

	constructor(
		public streamingService: StreamingService,
		@Inject(PLATFORM_ID) private readonly platformId: Object,
	) {
		this.isBrowser = isPlatformBrowser(platformId);
	}

	ngAfterViewInit(): void {
		if (this.isBrowser) {
			this.startOBS();
		}
	}

	ngOnDestroy(): void {
		this.player.dispose();
	}

	async startOBS() {
		if (!this.isBrowser) return; // Evita ejecutar en SSR

		this.streamingService.startOBS();

		// Esperar a que el DOM esté listo
		setTimeout(async () => {
			const videoOBS = document.getElementById('OBS') as HTMLVideoElement;
			if (!videoOBS) return console.error('Elemento con ID "OBS" no encontrado');

			await this.initVideoJS(videoOBS);
			this.initMediaStream(videoOBS);
			await this.renderRTMPLinks();
		}, 300);
	}

	/** Inicializa el reproductor Video.js */
	private async initVideoJS(videoEl: HTMLVideoElement): Promise<void> {
		const videojsModule = await import('video.js');
		const videojs = videojsModule.default;

		this.player = videojs(videoEl, {
			aspectRatio: '16:9',
			controls: false,
			autoplay: true,
			preload: 'auto',
			muted: true,
			html5: {
				hls: { overrideNative: true, enableLowLatency: true },
				vhs: { lowLatencyMode: true },
			},
			liveui: true,
		});

		this.player.src({
			src: this.streamingService.UrlPreview,
			type: 'application/x-mpegURL',
			withCredentials: true,
		});

		videoEl.style.height = 'auto';
	}

	/** Captura el stream y visualiza audio */
	private initMediaStream(videoEl: HTMLVideoElement): void {
		this.player.on('loadeddata', () => {
			this.m3u8Loaded = true;
			this.status = 'Todo listo!!';

			const techEl = this.player.tech(true)?.el() as HTMLVideoElement & { captureStream(): MediaStream };
			if (techEl?.captureStream) {
				const mediaStream = techEl.captureStream();
				const audioLevel = document.getElementById('audio-level') as HTMLDivElement;
				if (audioLevel) this.visualizeAudio(mediaStream, audioLevel);
			}
		});
	}

	/** Renderiza dinámicamente los enlaces RTMP y sus tooltips */
	private async renderRTMPLinks(): Promise<void> {
		const enlaces = document.getElementById('enlaces') as HTMLDivElement;
		if (!enlaces) return;

		const url = await firstValueFrom(this.streamingService.rtmpUrl$);
		if (!url) return console.error('No se pudo obtener la URL del servidor');

		const server = this.createParagraph('URL del servidor');
		const lServer = this.createLinkParagraph(url.substring(0, url.lastIndexOf('/')), enlaces);
		const key = this.createParagraph('Clave del stream');
		const lKey = this.createLinkParagraph(url.substring(url.lastIndexOf('/') + 1), enlaces);

		enlaces.append(server, lServer, key, lKey);
	}

	/** Crea un párrafo simple */
	private createParagraph(text: string): HTMLParagraphElement {
		const p = document.createElement('p');
		p.textContent = text;
		return p;
	}

	/** Crea un link de texto con tooltip y copia al portapapeles */
	private createLinkParagraph(text: string, container: HTMLElement): HTMLParagraphElement {
		const p = document.createElement('p');
		p.classList.add('p-2', 'cursor-pointer', 'rounded-lg', 'border', 'border-black', 'text-blue-700');
		p.textContent = text;

		const tooltip = document.createElement('div');
		tooltip.id = 'tooltip';
		tooltip.textContent = 'Haz click para copiar';
		tooltip.classList.add('absolute', 'bg-black', 'text-white', 'text-xs', 'p-1', 'rounded-sm', 'tooltip');

		const moveTooltip = (event: MouseEvent) => {
			tooltip.style.top = `${event.clientY - 30}px`;
			tooltip.style.left = `${event.clientX}px`;
		};

		p.onclick = () => navigator.clipboard.writeText(p.textContent || '').then(() => (tooltip.textContent = 'Copiado al portapapeles'));
		p.addEventListener('mouseover', (e: MouseEvent) => {
			tooltip.style.position = 'fixed';
			moveTooltip(e);
			container.appendChild(tooltip);
		});
		p.addEventListener('mousemove', moveTooltip);
		p.addEventListener('mouseleave', () => tooltip.remove());

		return p;
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
