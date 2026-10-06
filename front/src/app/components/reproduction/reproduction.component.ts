import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, HostListener, Inject, OnDestroy, OnInit, PLATFORM_ID, Renderer2, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import Player from 'video.js/dist/types/player';
import { Clase } from '../../models/Clase';
import { ClaseChat } from '../../models/ClaseChat';
import { Curso } from '../../models/Curso';
import { CursosService } from '../../services/cursos.service';
import { StreamingService } from '../../services/streaming.service';
import { ChatComponent } from '../chat/chat/chat.component';

@Component({
	selector: 'app-reproduction',
	standalone: true,
	imports: [ChatComponent],
	templateUrl: './reproduction.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './reproduction.component.css',
})
export class ReproductionComponent implements OnInit, AfterViewInit, OnDestroy {
	public idCurso: number = 0;
	public idClase: number = 0;
	public momento: number | null = null;
	private readonly isBrowser: boolean;
	private readonly subscription: Subscription = new Subscription();
	public loading: boolean = true;
	public curso: Curso | null = null;
	public clase: Clase | null = null;
	@ViewChild(ChatComponent, { static: false }) chatComponent!: ChatComponent;
	@ViewChild('videoElement') videoElement!: ElementRef<HTMLVideoElement>;
	backStream: string = '';
	player: Player | null = null;
	vista: number = 0; // 0: Contenido, 1: Chat

	// Estado para UI dinámica
	contextMenu: { x: number; y: number; time: number } | null = null;
	hoveredMarker: { message: string; x: number; y: number; idMensaje: string } | null = null;

	/**
	 * Constructor del componente.
	 * @param {ActivatedRoute} route - Ruta activada.
	 * @param {ChangeDetectorRef} cdr - Detección de cambios.
	 * @param {Object} platformId - ID de la plataforma.
	 * @param {Document} document - Documento.
	 * @param {CursosService} cursoService - Servicio de cursos.
	 * @param {StreamingService} streamingService - Servicio de streaming.
	 * @param {Router} router - Router de Angular.
	 * @param {Renderer2} renderer - Renderer2 de Angular.
	 */
	constructor(
		private readonly route: ActivatedRoute,
		private readonly cdr: ChangeDetectorRef,
		@Inject(PLATFORM_ID) private readonly platformId: object,
		@Inject(DOCUMENT) private readonly document: Document,
		public cursoService: CursosService,
		public streamingService: StreamingService,
		public router: Router,
		private readonly renderer: Renderer2,
	) {
		this.isBrowser = isPlatformBrowser(platformId);
	}

	/**
	 * Manejador de clics en el documento para cerrar menús contextuales.
	 * @param {MouseEvent} event - Evento de ratón.
	 */
	@HostListener('document:click', ['$event'])
	onDocumentClick(event: MouseEvent) {
		if (this.contextMenu) {
			this.contextMenu = null;
		}
	}

	/**
	 * Inicialización del componente.
	 */
	ngOnInit(): void {
		if (isPlatformBrowser(this.platformId)) {
			this.backStream = (globalThis.window as any).__env?.BACK_STREAM ?? '';
		}

		this.subscription.add(
			this.route.params.subscribe((params) => {
				this.idCurso = Number(params['idCurso']);
				this.idClase = Number(params['idClase']);

				if (this.idClase === 0) {
					this.cursoService
						.getStatusCurso(this.idCurso)
						.pipe(take(1))
						.subscribe({
							next: (resp) => {
								if (resp === 0 || resp === null) {
									alert('Este curso no está disponible');
									this.router.navigate(['/']);
								} else {
									this.router.navigate(['/repro/' + this.idCurso + '/' + resp]);
								}
							},
							error: (e) => {
								console.error('Error en recibir el estado del curso: ' + e.message);
							},
						});
				} else {
					this.loadData();
				}
			}),
		);
		this.subscription.add(
			this.route.queryParams.subscribe((qparams) => {
				this.momento = Number(qparams['momento']) || null;
				this.loadData();
			}),
		);
	}

	/**
	 * Carga los datos de la clase y el curso.
	 */
	loadData() {
		this.cursoService.getCurso(this.idCurso).then((result) => {
			this.curso = result;
			if (this.curso?.clasesCurso) {
				const result = this.curso.clasesCurso.find((clase) => clase.idClase == this.idClase);
				if (result) {
					this.clase = result;
					this.getVideo();
				}
			}
			this.cdr.detectChanges();
		});
	}

	ngAfterViewInit(): void {
		if (this.isBrowser) {
			this.getVideo();
		}
	}

	private async getVideo() {
		if (!this.isBrowser) return; // 🛡️ Protección SSR

		try {
			const video: HTMLVideoElement = this.videoElement?.nativeElement;
			if (!video) {
				console.warn('Elemento de video no encontrado');
				return;
			}

			// 📦 Importación dinámica
			const videojsModule = await import('video.js');
			const videojs = videojsModule.default;

			// Importar quality-levels
			await import('videojs-contrib-quality-levels');

			this.player = videojs(video, {
				controls: true,
				autoplay: true,
				preload: 'auto',
				techOrder: ['html5'],
				html5: {
					vhs: {
						withCredentials: true,
						enableLowInitialPlaylist: false,
						maxBufferLength: 3, // segundos de buffer máximo (prueba con 10-20)
					},
				},
			});

			this.player.src({
				src: `${this.backStream}/${this.idCurso}/${this.idClase}/master.m3u8`,
				type: 'application/x-mpegURL',
				withCredentials: true,
			});

			this.player.ready(() => {
				const qualityLevels = (this.player as any).qualityLevels();
				const vhs = (this.player?.tech() as any).vhs;

				// ✅ Activar modo automático desde el inicio
				for (let ql of qualityLevels) {
					ql.enabled = true;
				}
				vhs.autoLevelEnabled = true;

				const controlBar = this.player?.getChild('ControlBar');
				const progressControl = controlBar?.getChild('ProgressControl');
				const seekBar = progressControl?.getChild('SeekBar');

				if (controlBar?.el().querySelector('#vjs-quality-selector')) return;

				const wrapper = this.renderer.createElement('div');
				this.renderer.setProperty(wrapper, 'id', 'vjs-quality-selector');
				this.renderer.setStyle(wrapper, 'position', 'relative');
				this.renderer.setStyle(wrapper, 'marginLeft', '10px');

				const button = this.renderer.createElement('button');
				this.renderer.setProperty(button, 'textContent', 'Auto ▾');
				this.renderer.setStyle(button, 'padding', '4px');
				this.renderer.setStyle(button, 'margin', '4px');
				this.renderer.setStyle(button, 'background', '#222');
				this.renderer.setStyle(button, 'color', 'white');
				this.renderer.setStyle(button, 'border', '1px solid #444');
				this.renderer.setStyle(button, 'borderRadius', '4px');
				this.renderer.setStyle(button, 'cursor', 'pointer');
				this.renderer.setStyle(button, 'fontSize', '12px');

				const menu = this.renderer.createElement('div');
				this.renderer.setStyle(menu, 'position', 'absolute');
				this.renderer.setStyle(menu, 'bottom', '120%');
				this.renderer.setStyle(menu, 'left', '0');
				this.renderer.setStyle(menu, 'background', '#222');
				this.renderer.setStyle(menu, 'border', '1px solid #444');
				this.renderer.setStyle(menu, 'borderRadius', '4px');
				this.renderer.setStyle(menu, 'padding', '4px 0');
				this.renderer.setStyle(menu, 'display', 'none');
				this.renderer.setStyle(menu, 'zIndex', '1000');
				this.renderer.setStyle(menu, 'minWidth', '80px');

				let currentSelection = 'auto';

				const updateMenuHighlight = () => {
					const items: NodeListOf<HTMLElement> = menu.querySelectorAll('[data-quality]');
					items.forEach((item) => {
						const isActive = item.dataset['quality'] === String(currentSelection);

						this.renderer.setStyle(item, 'padding', '6px 12px');
						this.renderer.setStyle(item, 'cursor', 'pointer');
						this.renderer.setStyle(item, 'background', isActive ? '#555' : 'transparent');
						this.renderer.setStyle(item, 'fontWeight', isActive ? 'bold' : 'normal');
						this.renderer.setStyle(item, 'color', 'white');
					});
					this.renderer.setProperty(button, 'textContent', `${currentSelection === 'auto' ? 'Auto' : currentSelection + 'p'} ▾`);
				};

				this.renderer.listen(button, 'click', (e: MouseEvent) => {
					e.stopPropagation();
					const isHidden = menu.style.display === 'none';
					this.renderer.setStyle(menu, 'display', isHidden ? 'block' : 'none');
				});

				this.renderer.listen(this.document, 'click', () => {
					this.renderer.setStyle(menu, 'display', 'none');
				});

				const added = new Set();
				qualityLevels.on('addqualitylevel', () => {
					let changes = false;
					for (const ql of qualityLevels) {
						if (!added.has(ql.height)) {
							added.add(ql.height);
							changes = true;
						}
					}
					if (!changes) return;

					this.renderer.setProperty(menu, 'innerHTML', '');

					// Auto
					const autoItem = this.renderer.createElement('div');
					this.renderer.setProperty(autoItem, 'textContent', 'Auto');
					this.renderer.setAttribute(autoItem, 'data-quality', 'auto');
					this.renderer.listen(autoItem, 'click', () => {
						currentSelection = 'auto';
						for (const ql of qualityLevels) {
							ql.enabled = true;
						}

						vhs.autoLevelEnabled = true;

						// Forzar el estimador de ancho de banda a un valor alto (ejemplo: 5 Mbps)
						if (vhs.bandwidthEstimator && typeof vhs.bandwidthEstimator.sample === 'function') {
							vhs.bandwidthEstimator.sample(5_000_000, 1000); // 5 Mbps en 1s
						}

						updateMenuHighlight();
						this.renderer.setStyle(menu, 'display', 'none');
					});
					this.renderer.appendChild(menu, autoItem);

					Array.from(added)
						.sort((a: any, b: any) => b - a)
						.forEach((height) => {
							const item = this.renderer.createElement('div');
							this.renderer.setProperty(item, 'textContent', `${height}p`);
							this.renderer.setAttribute(item, 'data-quality', String(height));
							this.renderer.listen(item, 'click', () => {
								currentSelection = `${height}`;
								for (const level of qualityLevels) {
									level.enabled = level.height === height;
								}
								if (vhs?.autoLevelEnabled !== undefined) {
									vhs.autoLevelEnabled = false;
								}
								const player = this.player;
								const originalTime = player?.currentTime();
								const buffered = player?.buffered();
								let seeked = false;
								let jump = null;

								if (buffered?.length && originalTime) {
									for (let i = 0; i < buffered.length; i++) {
										const start = buffered.start(i);
										const end = buffered.end(i);
										if (originalTime >= start && originalTime <= end) {
											const duration = player?.duration();
											jump = end + 0.05;
											if (duration && jump >= duration) {
												jump = start > 0.05 ? start - 0.05 : 0;
											}
											player?.currentTime(jump);
											seeked = true;
											break;
										}
									}
								}
								if (!seeked && typeof originalTime === 'number') {
									const duration = player?.duration();
									if (duration) {
										jump = originalTime + 0.1 < duration ? originalTime + 0.1 : originalTime - 0.1;
										player?.currentTime(jump);
									}
								}

								// Volver al punto original tras el seek
								if (typeof originalTime === 'number') {
									player?.one('seeked', () => {
										setTimeout(() => {
											player.currentTime(originalTime);
										}, 100); // pequeño retardo para asegurar el cambio de calidad
									});
								}

								updateMenuHighlight();
								this.renderer.setStyle(menu, 'display', 'none');
							});
							this.renderer.appendChild(menu, item);
						});

					updateMenuHighlight();
				});

				this.renderer.appendChild(wrapper, button);
				this.renderer.appendChild(wrapper, menu);
				this.renderer.appendChild(controlBar?.el(), wrapper);

				if (seekBar) {
					seekBar.on('contextmenu', (event: MouseEvent) => {
						event.preventDefault();
						const rect = seekBar.el().getBoundingClientRect();
						const clickPosition = event.clientX - rect.left;
						const clickRatio = clickPosition / rect.width;
						const duration = this.player?.duration?.();
						if (duration) {
							const timeInSeconds = clickRatio * duration;
							this.muestraCortina(event.clientX, event.clientY, timeInSeconds);
						}
					});
				}

				this.loading = false;

				if (this.momento) {
					this.player?.currentTime(this.momento > 3 ? this.momento - 3 : this.momento);
					this.router.navigate([], {
						queryParams: { momento: null },
						queryParamsHandling: 'merge',
						replaceUrl: true,
					});
				}

				this.cdr.detectChanges();
			});

			this.player.on('play', () => {
				if (this.player) {
					this.esperarChatComponent(this.player);
				}
			});

			// Registrar progreso de fragmentos
			let lastSegmentReported = -1;
			this.player.on('timeupdate', () => {
				if (this.player) {
					const currentTime = this.player.currentTime();
					if (currentTime === undefined) return;

					// Obtenemos el índice del segmento actual basado en la duración del fragmento (ej. 2s)
					// Ajusta este valor según la configuración de tu HLS
					const fragmentDuration = 2;
					const segmentIndex = Math.floor(currentTime / fragmentDuration);

					// Solo registrar si hemos cambiado de segmento
					if (segmentIndex !== lastSegmentReported) {
						lastSegmentReported = segmentIndex;
						this.streamingService
							.registrarProgreso(this.idCurso, this.idClase, segmentIndex)
							.pipe(take(1))
							.subscribe({
								error: (err) => console.error('Error al registrar progreso:', err),
							});
					}
				}
			});
		} catch (error) {
			console.error('Error loading video:', error);
		}
	}
	ngOnDestroy(): void {
		this.subscription.unsubscribe();
	}

	navega(clase: Clase) {
		this.router.navigate(['repro/' + this.idCurso + '/' + clase.idClase]);
	}

	cambiaVista(vista: number) {
		this.vista = vista;
		this.cdr.detectChanges();
	}

	// Función para mostrar la cortina en la posición del clic
	private muestraCortina(x: number, y: number, timeInSeconds: number) {
		this.contextMenu = { x, y: y + (globalThis.window?.scrollY || 0), time: timeInSeconds };
		this.cdr.detectChanges();
	}

	clickPregunta() {
		if (this.contextMenu) {
			this.cambiaVista(1);
			this.chatComponent.creaPregunta(this.idClase, this.contextMenu.time);
			this.contextMenu = null;
			this.cdr.detectChanges();
		}
	}

	async esperarChatComponent(player: Player) {
		// Si `chat` no está cargado, espera un segundo antes de continuar
		while (!this.chatComponent.chat) {
			await new Promise((resolve) => setTimeout(resolve, 300)); // Espera 0.3 segundos
		}

		// Ejecutar el código después de verificar que `chat` está definido
		const claseChat: ClaseChat | undefined = this.chatComponent.chat?.clases.find((clase) => clase.idClase == this.idClase);
		if (claseChat) {
			for (const preg of claseChat.mensajes) {
				if (!preg.pregunta) continue; // saltar mensajes sin tiempo de pregunta

				const duration = player.duration();
				const preguntaTime = preg.pregunta;

				if (!duration || preguntaTime > duration) continue;

				const clickRatio = preguntaTime / duration;
				const seekBar = player.getChild('ControlBar')?.getChild('ProgressControl')?.getChild('SeekBar');
				if (!seekBar) continue;

				const rect = seekBar.el().getBoundingClientRect();
				const preguntaPosX = rect.width * clickRatio;

				// Crear marcador
				const marcador = this.renderer.createElement('div');
				this.renderer.setStyle(marcador, 'position', 'absolute');
				this.renderer.setStyle(marcador, 'left', `${preguntaPosX}px`);
				this.renderer.setStyle(marcador, 'top', '0');
				this.renderer.setStyle(marcador, 'width', '4px');
				this.renderer.setStyle(marcador, 'height', '100%');
				this.renderer.setStyle(marcador, 'zIndex', '10');
				this.renderer.setStyle(marcador, 'backgroundColor', '#eab308');

				this.renderer.listen(marcador, 'mouseover', (event: MouseEvent) => {
					this.hoveredMarker = {
						message: preg.mensaje ?? '',
						x: event.clientX,
						y: event.clientY + 20,
						idMensaje: preg.idMensaje ?? '',
					};
					this.cdr.detectChanges();
				});

				this.renderer.listen(marcador, 'mouseout', () => {
					setTimeout(() => {
						this.hoveredMarker = null;
						this.cdr.detectChanges();
					}, 1000);
				});

				this.renderer.appendChild(seekBar.el(), marcador);
			}
		}
		this.cdr.detectChanges();
	}
}
