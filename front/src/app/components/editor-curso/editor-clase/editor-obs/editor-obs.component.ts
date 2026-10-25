import { AfterViewInit, Component, effect, ElementRef, input, OnDestroy, output, Renderer2, signal, viewChild } from '@angular/core';

@Component({
	selector: 'app-editor-obs',
	imports: [],
	templateUrl: './editor-obs.component.html',
	styleUrl: './editor-obs.component.css',
})
export class EditorObsComponent implements AfterViewInit, OnDestroy {
	emitiendo = input<boolean>(); // Avisa cuando está listo para emitir (opcional)
	status = input<string>(); // Estado del servicio de streaming (opcional)
	urlPreview = input<string>();
	rtmpUrl = input<string>();
	obsEvent = output<{ type: string; message: string }>();

	videoOBSRef = viewChild<ElementRef<HTMLVideoElement>>('videoOBS');
	audioLevelRef = viewChild<ElementRef<HTMLDivElement>>('audioLevel');

	m3u8Loaded: boolean = false;
	isBrowser: boolean;
	player: any;
	rutaOBS: string = '';
	tiempoGrabacion: string = '00:00:00';

	rtmpServer = signal<string>('');
	rtmpKey: string = '';
	tooltipText: string = 'Haz click para copiar';
	showTooltip = signal<boolean>(false);
	tooltipPos = { x: 0, y: 0 };

	private audioContext: AudioContext | null = null;
	private animationFrameId: number | null = null;

	/**
	 * Constructor del componente.
	 * @param {Renderer2} renderer - Renderer2 de Angular.
	 */
	constructor(private readonly renderer: Renderer2) {
		this.isBrowser = globalThis.window !== undefined;
		effect(() => {
			if (!this.isBrowser) return;
			const urlPreview = this.urlPreview();
			if (!urlPreview) {
				return;
			}
			const videoOBS = this.videoOBSRef()?.nativeElement;
			if (!videoOBS) {
				console.error('Elemento con ID "OBS" no encontrado');
				return;
			}
			this.initVideoJS(videoOBS)
				.then(() => {
					this.initMediaStream(videoOBS);
					this.prepareRTMPData();
				})
				.catch((e) => console.error(e));
		});
	}

	/**
	 * Inicialización de la vista.
	 */
	ngAfterViewInit(): void {
		if (this.isBrowser) {
			this.startOBS();
		}
	}

	/**
	 * Limpieza de recursos al destruir el componente.
	 */
	ngOnDestroy(): void {
		if (this.audioContext && this.audioContext.state !== 'closed') {
			this.audioContext.close().catch(() => {});
		}
		if (this.animationFrameId) {
			cancelAnimationFrame(this.animationFrameId);
		}
		if (this.player && typeof this.player.dispose === 'function') {
			try {
				this.player.dispose();
			} catch (e) {
				console.error(e);
			}
		}
	}

	/**
	 * Inicia el servicio OBS.
	 */
	async startOBS() {
		if (!this.isBrowser) return; // Evita ejecutar en SSR
		this.obsEvent.emit({ type: 'startOBS', message: '' });
	}

	/**
	 * Inicializa el reproductor Video.js.
	 * @param {HTMLVideoElement} videoEl - Elemento de video.
	 */
	private async initVideoJS(videoEl: HTMLVideoElement): Promise<void> {
		const videojsModule = await import('video.js');
		const videojs = videojsModule.default;

		if (this.player && typeof this.player.dispose === 'function') {
			try {
				this.player.dispose();
			} catch (e) {
				console.error(e);
			}
		}

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
			src: this.urlPreview(),
			type: 'application/x-mpegURL',
			withCredentials: true,
		});

		this.renderer.setStyle(videoEl, 'height', 'auto');
	}

	/**
	 * Captura el stream y visualiza audio.
	 * @param {HTMLVideoElement} videoEl - Elemento de video.
	 */
	private initMediaStream(videoEl: HTMLVideoElement): void {
		this.player.on('loadeddata', () => {
			this.m3u8Loaded = true;
			this.obsEvent.emit({ type: 'status', message: 'Todo listo!!' });

			const techEl = this.player.tech(true)?.el() as HTMLVideoElement & { captureStream(): MediaStream };
			if (techEl?.captureStream) {
				const mediaStream = techEl.captureStream();
				const audioLevel = this.audioLevelRef()?.nativeElement;
				if (audioLevel) this.visualizeAudio(mediaStream, audioLevel);
			}
		});
	}

	/** Prepara los datos RTMP para el template */
	private prepareRTMPData(): void {
		if (!this.rtmpUrl()) return;
		this.rtmpServer.set(this.rtmpUrl()!.substring(0, this.rtmpUrl()!.lastIndexOf('/')));
		this.rtmpKey = this.rtmpUrl()!.substring(this.rtmpUrl()!.lastIndexOf('/') + 1);
	}

	copyToClipboard(text: string) {
		navigator.clipboard.writeText(text).then(() => {
			this.tooltipText = 'Copiado al portapapeles';
		});
	}

	onMouseMove(event: MouseEvent) {
		this.tooltipPos = { x: event.clientX, y: event.clientY - 30 };
	}

	onMouseOver() {
		this.tooltipText = 'Haz click para copiar';
		this.showTooltip.set(true);
	}

	onMouseLeave() {
		this.showTooltip.set(false);
	}

	async emiteOBS() {
		if (!this.m3u8Loaded) {
			alert('Debes conectarte primero con OBS');
			return;
		}

		if (!this.rtmpUrl()) return;

		this.obsEvent.emit({ type: 'emiteOBS', message: this.rtmpUrl()! });
		this.calculaTiempoGrabacion();
	}

	detenerEmision() {
		this.obsEvent.emit({ type: 'stopOBS', message: '' });
		if (typeof this.player?.src === 'function') {
			this.player.src({ src: '' });
		} else if (this.player) {
			this.player.src = '';
		}
		if (this.player) {
			this.player.srcObject = null;
		}
	}

	visualizeAudio(stream: MediaStream, audioLevel: HTMLDivElement) {
		const audioContext = new AudioContext();
		const analyser = audioContext.createAnalyser();
		const source = audioContext.createMediaStreamSource(stream);

		source.connect(analyser);

		analyser.fftSize = 256; // Ajusta la resolución de frecuencia
		const dataArray = new Uint8Array(analyser.frequencyBinCount);

		const updateAudioLevel = () => {
			analyser.getByteFrequencyData(dataArray);
			const volume = Math.max(...dataArray) / 255; // Escalar de 0 a 1
			const percentage = Math.min(volume * 100, 100); // Limitar a 100%
			this.renderer.setStyle(audioLevel, 'width', `${percentage}%`); // Ajustar ancho de la barra

			requestAnimationFrame(updateAudioLevel); // Continuar la animación
		};

		updateAudioLevel(); // Iniciar la visualización
	}

	/**
	 * Método para calcular el tiempo de grabación
	 */
	async calculaTiempoGrabacion() {
		let tiempo = -1;
		const updateTimer = () => {
			if (this.emitiendo()) {
				tiempo += 1;
				this.tiempoGrabacion = this.formatTime(tiempo);
			}
			setTimeout(updateTimer, 1000);
		};
		updateTimer();
	}

	/**
	 * Función para formatear el tiempo de grabación
	 * @param seconds segundos transcurridos (number)
	 * @returns el tiempo en formato hh:mm:ss (string)
	 */
	private formatTime(seconds: number): string {
		if (Number.isNaN(seconds) || !Number.isFinite(seconds)) {
			return '00:00:00';
		}
		const hrs = Math.floor(seconds / 3600);
		const mins = Math.floor((seconds % 3600) / 60);
		const secs = Math.floor(seconds % 60);

		return `${hrs.toString().padStart(2, '0')}:` + `${mins.toString().padStart(2, '0')}:` + `${secs.toString().padStart(2, '0')}`;
	}
}
