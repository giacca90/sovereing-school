import { Component, Input } from '@angular/core';
import { Clase } from '../../../../models/Clase';
import { Curso } from '../../../../models/Curso';
import { ClaseService } from '../../../../services/clase.service';

@Component({
	selector: 'app-editor-video',
	imports: [],
	templateUrl: './editor-video.component.html',
	styleUrl: './editor-video.component.css',
})
export class EditorVideoComponent {
	@Input() clase!: Clase;
	public curso!: Curso;

	constructor(private claseService: ClaseService) {}
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
						if (result && this.curso?.clases_curso && this.clase) {
							this.clase.direccion_clase = result;
							this.clase.curso_clase = this.curso.id_curso;
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
	}

	keyEvent(event: KeyboardEvent) {
		if (event.key === 'Enter') {
			document.getElementById('video-upload')?.click();
		}
	}
}
