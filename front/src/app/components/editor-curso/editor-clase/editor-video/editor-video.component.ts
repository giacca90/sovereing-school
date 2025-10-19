import { AfterViewInit, Component, EventEmitter, Input, Output } from '@angular/core';
import Player from 'video.js/dist/types/player';
import { Clase } from '../../../../models/Clase';
import { StreamingService } from '../../../../services/streaming.service';
@Component({
	selector: 'app-editor-video',
	imports: [],
	templateUrl: './editor-video.component.html',
	styleUrl: './editor-video.component.css',
})
export class EditorVideoComponent implements AfterViewInit {
	@Input() clase!: Clase;
	@Output() readyEvent: EventEmitter<boolean> = new EventEmitter();
	player: Player | null = null;
	backStream: string = '';

	constructor(private readonly streamingService: StreamingService) {}

	async ngAfterViewInit(): Promise<void> {
		if (this.clase.direccion_clase) {
			this.backStream = (window as any).__env?.BACK_STREAM ?? '';
			const videoPlayer = document.getElementById('videoPlayer') as HTMLVideoElement;
			if (!videoPlayer) {
				console.error('No se pudo obtener el elemento videoPlayer');
				return;
			}
			const videojsModule = await import('video.js');
			const videojs = videojsModule.default;

			// Importar quality-levels
			await import('videojs-contrib-quality-levels');

			// Destruye el player anterior si existe
			if (this.player) {
				this.player.dispose();
			}

			this.player = videojs(videoPlayer, {
				controls: true,
				autoplay: true,
				preload: 'auto',
				techOrder: ['html5'],
				html5: {
					vhs: {
						withCredentials: true,
					},
				},
			});

			//console.log('cursoUrl:', this.clase.curso_clase);

			this.player.src({
				src: `${this.backStream}/${this.clase.curso_clase}/${this.clase.id_clase}/master.m3u8`,
				type: 'application/x-mpegURL',
				withCredentials: true,
			});

			this.player.ready(() => {
				const qualityLevels = (this.player as any).qualityLevels();
				qualityLevels.on('addqualitylevel', () => {
					// ⚙️ Mostrar todas las resoluciones disponibles
					const availableQualities = [];
					for (let i = 0; i < qualityLevels.length; i++) {
						const q = qualityLevels[i];
						availableQualities.push({ index: i, height: q.height, width: q.width });
					}
				});

				const controlBar = this.player?.getChild('ControlBar');
				// ❗ Verificar que el botón no exista ya
				if (controlBar?.el().querySelector('#vjs-quality-selector')) return;

				// 👉 Crear contenedor del botón
				const wrapper = document.createElement('div');
				wrapper.id = 'vjs-quality-selector';
				wrapper.style.position = 'relative';
				wrapper.style.marginLeft = '10px';

				// 🎯 Botón principal
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

				// 📋 Menú
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

				// Estado actual
				let currentSelection = 'auto';

				// 👉 Función para actualizar botón y menú
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
					// Actualizar texto del botón
					button.textContent = `${currentSelection === 'auto' ? 'Auto' : currentSelection + 'p'} ▾`;
				};

				// 👉 Toggle del menú
				button.onclick = (e) => {
					e.stopPropagation();
					menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
				};

				// ❌ Cerrar al hacer clic fuera
				document.addEventListener('click', () => {
					menu.style.display = 'none';
				});

				// 🔁 Escuchar calidades disponibles
				qualityLevels.on('addqualitylevel', () => {
					menu.innerHTML = '';

					// Auto
					const autoItem = document.createElement('div');
					autoItem.textContent = 'Auto';
					autoItem.setAttribute('data-quality', 'auto');
					autoItem.onclick = () => {
						currentSelection = 'auto';
						for (let i = 0; i < qualityLevels.length; i++) {
							qualityLevels[i].enabled = true;
						}
						updateMenuHighlight();
						menu.style.display = 'none';
					};
					menu.appendChild(autoItem);

					// Resolutions específicas
					const added = new Set();
					for (let i = 0; i < qualityLevels.length; i++) {
						const q = qualityLevels[i];
						const height = q.height;
						if (added.has(height)) continue; // evitar duplicados
						added.add(height);

						const item = document.createElement('div');
						item.textContent = `${height}p`;
						item.setAttribute('data-quality', `${height}`);
						item.onclick = () => {
							currentSelection = `${height}`;
							for (let j = 0; j < qualityLevels.length; j++) {
								qualityLevels[j].enabled = qualityLevels[j].height === height;
							}
							updateMenuHighlight();
							menu.style.display = 'none';
						};

						menu.appendChild(item);
					}

					updateMenuHighlight(); // aplicar por primera vez
				});

				// 🧱 Insertar en la barra de controles
				wrapper.appendChild(button);
				wrapper.appendChild(menu);
				controlBar?.el().appendChild(wrapper);
			});

			this.player.on('loadeddata', () => {
				//console.log('Archivo .m3u8 cargado correctamente');
			});
		}
	}

	cargaVideo(event: Event) {
		const input = event.target as HTMLInputElement;
		if (!input.files) {
			alert('Sube un video valido!!!');
			return;
		}
		const button = document.getElementById('video-upload-button') as HTMLSpanElement;
		button.classList.remove('border-black');
		button.classList.add('border-gray-500', 'text-gray-500');
		const button_guardar_clase = document.getElementById('button-guardar-clase') as HTMLButtonElement;
		button_guardar_clase.classList.remove('border-black');
		button_guardar_clase.classList.add('border-gray-500', 'text-gray-500');
		button_guardar_clase.disabled = true;

		const reader = new FileReader();
		reader.onload = (e: ProgressEvent<FileReader>) => {
			if (e.target) {
				const vid: HTMLVideoElement = document.getElementById('videoPlayer') as HTMLVideoElement;
				vid.src = e.target.result as string;
				if (this.clase && !this.clase?.id_clase) {
					this.clase.id_clase = 0;
				}
				if (input.files && this.clase) {
					const formData = new FormData();
					formData.append('video', input.files[0], input.files[0].name);

					this.streamingService.subeVideo(input.files[0], this.clase.curso_clase, this.clase?.id_clase).subscribe((result) => {
						if (result && this.clase) {
							this.clase.direccion_clase = result;
							button.classList.remove('border-gray-500', 'text-gray-500');
							button.classList.add('border-black');
							button_guardar_clase.classList.remove('border-gray-500', 'text-gray-500');
							button_guardar_clase.classList.add('border-black');
							button_guardar_clase.disabled = false;
						}
					});
				}
			}
		};
		reader.readAsDataURL(input.files[0]);
		this.readyEvent.emit(true);
	}

	keyEvent(event: KeyboardEvent) {
		if (event.key === 'Enter') {
			document.getElementById('video-upload')?.click();
		}
	}
}
