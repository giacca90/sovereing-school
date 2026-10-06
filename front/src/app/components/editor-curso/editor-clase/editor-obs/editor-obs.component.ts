import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, NgZone, OnDestroy, Output, Renderer2, SimpleChanges, ViewChild } from '@angular/core';

@Component({
	selector: 'app-editor-obs',
	imports: [],
	templateUrl: './editor-obs.component.html',
	styleUrl: './editor-obs.component.css',
})
export class EditorObsComponent implements AfterViewInit, OnDestroy {
	@Input() emitiendo?: boolean; // Avisa cuando está listo para emitir (opcional)
	@Input() status?: string; // Estado del servicio de streaming (opcional)
	@Input() urlPreview?: string;
	@Input() rtmpUrl?: string;
	@Output() obsEvent: EventEmitter<{ type: string; message: string }> = new EventEmitter();

	@ViewChild('videoOBS') videoOBSRef!: ElementRef<HTMLVideoElement>;
	@ViewChild('audioLevel') audioLevelRef!: ElementRef<HTMLDivElement>;

	m3u8Loaded: boolean = false;
	isBrowser: boolean;
	player: any;
	rutaOBS: string = '';
	tiempoGrabacion: string = '00:00:00';

	rtmpServer: string = '';
	rtmpKey: string = '';
	tooltipText: string = 'Haz click para copiar';
	showTooltip: boolean = false;
	tooltipPos = { x: 0, y: 0 };

	/**
	 * Constructor del componente.
	 * @param {Renderer2} renderer - Renderer2 de Angular.
	 * @param {ChangeDetectorRef} cdr - Detección de cambios.
	 */
	constructor(
		private readonly renderer: Renderer2,
		private readonly cdr: ChangeDetectorRef,
	) {
		this.isBrowser = globalThis.window !== undefined;
		console.log('¿EditorObsComponent está en la zona?:', NgZone.isInAngularZone());
	}

	/**
	 * Detecta cambios en las propiedades de entrada (@Input).
	 * @param {SimpleChanges} changes - Cambios detectados.
	 */
	/* ngOnChanges(changes: SimpleChanges): void {
		if (changes['rtmpUrl']) {
			this.prepareRTMPData();
		}
		if (changes['urlPreview'] && !changes['urlPreview'].firstChange && this.player) {
			this.player.src({
				src: this.urlPreview,
				type: 'application/x-mpegURL',
				withCredentials: true,
			});
		}
		if (changes['status'] || changes['emitiendo'] || changes['urlPreview'] || changes['rtmpUrl']) {
			this.cdr.markForCheck();
		}
	} */

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
		this.player.dispose();
	}

	/**
	 * Inicia el servicio OBS.
	 */
	async startOBS() {
		if (!this.isBrowser) return; // Evita ejecutar en SSR

		this.obsEvent.emit({ type: 'startOBS', message: '' });

		// Esperar a que el DOM esté listo
		setTimeout(async () => {
			const videoOBS = this.videoOBSRef.nativeElement;
			if (!videoOBS) return console.error('Elemento con ID "OBS" no encontrado');

			await this.initVideoJS(videoOBS);
			this.initMediaStream(videoOBS);
			this.prepareRTMPData();
		}, 300);
	}

	/**
	 * Inicializa el reproductor Video.js.
	 * @param {HTMLVideoElement} videoEl - Elemento de video.
	 */
	private async initVideoJS(videoEl: HTMLVideoElement): Promise<void> {
		if (!this.urlPreview) {
			console.log('usrPreview vacío');
			setTimeout(() => {
				this.initVideoJS(videoEl);
			}, 100);
		}
		const videojsModule = await import('video.js');
		const videojs = videojsModule.default;
		console.log('¿initVideoJS está en la zona?:', NgZone.isInAngularZone());
		console.log('urlPreview:', this.urlPreview);

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
			src: this.urlPreview,
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
				const audioLevel = this.audioLevelRef?.nativeElement;
				if (audioLevel) this.visualizeAudio(mediaStream, audioLevel);
			}
		});
	}

	/** Prepara los datos RTMP para el template */
	private prepareRTMPData(): void {
		if (!this.rtmpUrl) return;
		this.rtmpServer = this.rtmpUrl.substring(0, this.rtmpUrl.lastIndexOf('/'));
		this.rtmpKey = this.rtmpUrl.substring(this.rtmpUrl.lastIndexOf('/') + 1);
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
		this.showTooltip = true;
	}

	onMouseLeave() {
		this.showTooltip = false;
	}

	async emiteOBS() {
		if (!this.m3u8Loaded) {
			alert('Debes conectarte primero con OBS');
			return;
		}

		if (!this.rtmpUrl) return;

		this.obsEvent.emit({ type: 'emiteOBS', message: this.rtmpUrl });
		this.calculaTiempoGrabacion();
	}

	detenerEmision() {
		this.obsEvent.emit({ type: 'stopOBS', message: '' });
		this.player.src = '';
		this.player.srcObject = null;
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
			console.log('tiempo: ' + tiempo);
			if (this.emitiendo) {
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
