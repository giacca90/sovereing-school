import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, EventEmitter, Inject, Input, Output, PLATFORM_ID } from '@angular/core';
import videojs from 'video.js';
import Player from 'video.js/dist/types/player';
import { Clase } from '../../../../models/Clase';
import { ClaseService } from '../../../../services/clase.service';
@Component({
	selector: 'app-editor-video',
	imports: [],
	templateUrl: './editor-video.component.html',
	styleUrl: './editor-video.component.css',
})
export class EditorVideoComponent implements AfterViewInit {
	@Input() clase!: Clase;
	@Output() readyEvent: EventEmitter<boolean> = new EventEmitter();
	isBrowser: boolean;
	player: Player | null = null;
	backStream: string = '';

	constructor(
		private claseService: ClaseService,
		@Inject(PLATFORM_ID) private platformId: Object,
	) {
		this.isBrowser = isPlatformBrowser(platformId);
	}

	ngAfterViewInit(): void {
		if (this.isBrowser && this.clase.direccion_clase) {
			this.backStream = (window as any).__env?.BACK_STREAM ?? '';
			const videoPlayer = document.getElementById('videoPlayer') as HTMLVideoElement;
			if (!videoPlayer) {
				console.error('No se pudo obtener el elemento videoPlayer');
				return;
			}
			const player = videojs(videoPlayer, {
				aspectRatio: '16:9',
				controls: true,
				autoplay: false,
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
			});

			console.log('cursoUrl:', this.clase.curso_clase);

			player.src({
				src: `${this.backStream}/${this.clase.curso_clase}/${this.clase.id_clase}/master.m3u8`,
				type: 'application/x-mpegURL',
				withCredentials: true,
			});

			player.on('loadeddata', () => {
				console.log('Archivo .m3u8 cargado correctamente');
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
					this.claseService.subeVideo(input.files[0], this.clase.curso_clase, this.clase?.id_clase).subscribe((result) => {
						if (result && this.clase) {
							this.clase.direccion_clase = result;
							this.clase.curso_clase = this.clase.curso_clase;
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
