import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, PLATFORM_ID, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import Player from 'video.js/dist/types/player';
import { Clase } from '../../models/Clase';
import { ClaseChat } from '../../models/ClaseChat';
import { Curso } from '../../models/Curso';
import { CursosService } from '../../services/cursos.service';
import { ChatComponent } from '../chat/chat/chat.component';

@Component({
	selector: 'app-reproduction',
	standalone: true,
	imports: [ChatComponent],
	templateUrl: './reproduction.component.html',
	styleUrl: './reproduction.component.css',
})
export class ReproductionComponent implements OnInit, AfterViewInit, OnDestroy {
	public id_curso: number = 0;
	public id_clase: number = 0;
	public momento: number | null = null;
	private readonly isBrowser: boolean;
	private readonly subscription: Subscription = new Subscription();
	public loading: boolean = true;
	public curso: Curso | null = null;
	public clase: Clase | null = null;
	@ViewChild(ChatComponent, { static: false }) chatComponent!: ChatComponent;
	backStream: string = '';
	player: Player | null = null;

	constructor(
		private readonly route: ActivatedRoute,
		private readonly cdr: ChangeDetectorRef,
		@Inject(PLATFORM_ID) private readonly platformId: object,
		public cursoService: CursosService,
		public router: Router,
	) {
		this.isBrowser = isPlatformBrowser(platformId);
	}

	ngOnInit(): void {
		this.subscription.add(
			this.route.params.subscribe((params) => {
				this.id_curso = params['id_curso'];
				this.id_clase = params['id_clase'];

				if (this.id_clase == 0) {
					this.cursoService.getStatusCurso(this.id_curso).subscribe({
						next: (resp) => {
							if (resp === 0) {
								alert('Este curso no está disponible');
								this.router.navigate(['/']);
							} else {
								this.router.navigate(['/repro/' + this.id_curso + '/' + resp]);
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
				this.momento = qparams['momento'] || null;
				this.loadData();
			}),
		);
		if (isPlatformBrowser(this.platformId)) {
			this.backStream = (globalThis.window as any).__env?.BACK_STREAM ?? '';
		}
	}

	loadData() {
		this.cursoService.getCurso(this.id_curso).then((result) => {
			this.curso = result;
			if (this.curso?.clases_curso) {
				const result = this.curso.clases_curso.find((clase) => clase.id_clase == this.id_clase);
				if (result) {
					this.clase = result;
					this.cdr.detectChanges();
					this.getVideo();
				}
			}
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
			const video: HTMLVideoElement = document.getElementById('video') as HTMLVideoElement;
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
				src: `${this.backStream}/${this.id_curso}/${this.id_clase}/master.m3u8`,
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

				const wrapper = document.createElement('div');
				wrapper.id = 'vjs-quality-selector';
				wrapper.style.position = 'relative';
				wrapper.style.marginLeft = '10px';

				const button = document.createElement('button');
				button.textContent = 'Auto ▾';
				button.style.padding = '4px';
				button.style.margin = '4px';
				button.style.background = '#222';
				button.style.color = 'white';
				button.style.border = '1px solid #444';
				button.style.borderRadius = '4px';
				button.style.cursor = 'pointer';
				button.style.fontSize = '12px';

				const menu = document.createElement('div');
				menu.style.position = 'absolute';
				menu.style.bottom = '120%';
				menu.style.left = '0';
				menu.style.background = '#222';
				menu.style.border = '1px solid #444';
				menu.style.borderRadius = '4px';
				menu.style.padding = '4px 0';
				menu.style.display = 'none';
				menu.style.zIndex = '1000';
				menu.style.minWidth = '80px';

				let currentSelection = 'auto';

				const updateMenuHighlight = () => {
					const items = menu.querySelectorAll('[data-quality]');
					items.forEach((item) => {
						const isActive = item.getAttribute('data-quality') === currentSelection;
						item.setAttribute(
							'style',
							`
								padding: 6px 12px;
								cursor: pointer;
								background: ${isActive ? '#555' : 'transparent'};
								font-weight: ${isActive ? 'bold' : 'normal'};
								color: white;
							`,
						);
					});
					button.textContent = `${currentSelection === 'auto' ? 'Auto' : currentSelection + 'p'} ▾`;
				};

				button.onclick = (e) => {
					e.stopPropagation();
					menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
				};

				document.addEventListener('click', () => {
					menu.style.display = 'none';
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

					menu.innerHTML = '';

					// Auto
					const autoItem = document.createElement('div');
					autoItem.textContent = 'Auto';
					autoItem.setAttribute('data-quality', 'auto');
					autoItem.onclick = () => {
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
						menu.style.display = 'none';
					};
					menu.appendChild(autoItem);

					Array.from(added)
						.sort((a: any, b: any) => b - a)
						.forEach((height) => {
							const item = document.createElement('div');
							item.textContent = `${height}p`;
							item.setAttribute('data-quality', `${height}`);
							item.onclick = () => {
								currentSelection = `${height}`;
								for (let j = 0; j < qualityLevels.length; j++) {
									qualityLevels[j].enabled = qualityLevels[j].height === height;
								}
								if (vhs && vhs.autoLevelEnabled !== undefined) {
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
								menu.style.display = 'none';
							};
							menu.appendChild(item);
						});

					updateMenuHighlight();
				});

				wrapper.appendChild(button);
				wrapper.appendChild(menu);
				controlBar?.el().appendChild(wrapper);

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
		} catch (error) {
			console.error('Error loading video:', error);
		}
	}
	ngOnDestroy(): void {
		this.subscription.unsubscribe();
	}

	navega(clase: Clase) {
		this.router.navigate(['repro/' + this.id_curso + '/' + clase.id_clase]);
	}

	cambiaVista(vista: number) {
		const vistaContenido: HTMLDivElement = document.getElementById('contenido') as HTMLDivElement;
		const vistaChat: HTMLDivElement = document.getElementById('chat') as HTMLDivElement;
		switch (vista) {
			case 0: {
				vistaContenido.hidden = false;
				vistaChat.hidden = true;
				break;
			}
			case 1: {
				vistaContenido.hidden = true;
				vistaChat.hidden = false;
				break;
			}
		}
	}

	// Función para mostrar la cortina en la posición del clic
	private muestraCortina(x: number, y: number, timeInSeconds: number) {
		const curtain = document.createElement('div');
		curtain.style.position = 'absolute';
		curtain.style.top = `${y + window.scrollY}px`;
		curtain.style.left = `${x}px`;
		curtain.style.width = '200px';
		curtain.style.height = 'auto';
		curtain.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
		curtain.style.color = 'white';
		curtain.style.padding = '10px';
		curtain.style.borderRadius = '5px';
		curtain.style.zIndex = '1000';

		// Botón para hacer una pregunta
		const pregunta: HTMLDivElement = document.createElement('div');
		pregunta.innerText = 'Haz una pregunta';
		pregunta.style.cursor = 'pointer';
		pregunta.addEventListener('click', () => {
			this.cambiaVista(1);
			this.chatComponent.creaPregunta(this.id_clase, timeInSeconds);
			curtain.remove();
		});

		curtain.appendChild(pregunta);
		document.body.appendChild(curtain);

		// Cierra la cortina al hacer clic fuera de ella
		globalThis.window.addEventListener(
			'click',
			(event) => {
				if (!curtain.contains(event.target as Node)) {
					curtain.remove();
				}
			},
			{ once: true },
		);
	}

	async esperarChatComponent(player: Player) {
		// Si `chat` no está cargado, espera un segundo antes de continuar
		while (!this.chatComponent.chat) {
			await new Promise((resolve) => setTimeout(resolve, 300)); // Espera 0.3 segundos
		}

		// Ejecutar el código después de verificar que `chat` está definido
		const claseChat: ClaseChat | undefined = this.chatComponent.chat?.clases.find((clase) => clase.id_clase == this.id_clase);
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
				const marcador = document.createElement('div');
				Object.assign(marcador.style, {
					position: 'absolute',
					left: `${preguntaPosX}px`,
					top: '0',
					width: '4px',
					height: '100%',
					zIndex: '10',
					backgroundColor: '#eab308',
				});

				let overCortina = false;

				marcador.addEventListener('mouseover', (event: MouseEvent) => {
					const cortina = document.createElement('div');
					cortina.className = 'cortina-info';
					cortina.innerText = preg.mensaje ?? '';
					Object.assign(cortina.style, {
						position: 'absolute',
						left: `${event.clientX}px`,
						top: `${event.clientY + 20}px`,
						padding: '5px',
						backgroundColor: 'rgba(0,0,0,0.8)',
						color: 'white',
						borderRadius: '3px',
						zIndex: '1000',
						cursor: 'pointer',
					});

					cortina.addEventListener('mouseout', () => {
						cortina.remove();
						overCortina = false;
					});

					cortina.addEventListener('mouseover', () => (overCortina = true));

					cortina.addEventListener('click', () => {
						this.cambiaVista(1);
						this.chatComponent.abreChatClase(this.id_clase);
						cortina.remove();
						const mensajeElement = document.getElementById('mex-' + preg.id_mensaje);
						if (mensajeElement) {
							mensajeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
						} else {
							console.error('No se encontró el mensaje');
						}
					});

					document.body.appendChild(cortina);

					marcador.addEventListener(
						'mouseout',
						() => {
							setTimeout(() => {
								if (!overCortina) cortina.remove();
							}, 1000);
						},
						{ once: true },
					);
				});

				// Añadir marcador al SeekBar
				seekBar.el().appendChild(marcador);
			}
		}
		this.cdr.detectChanges();
	}
}
