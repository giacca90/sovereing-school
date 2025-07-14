import { afterNextRender, ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { SwiperContainer } from 'swiper/element';
import { register } from 'swiper/element/bundle';
import { Usuario } from '../../models/Usuario';
import { CursosService } from '../../services/cursos.service';
import { InitService } from '../../services/init.service';
import { UsuariosService } from '../../services/usuarios.service';

register(); // Registrar Swiper al inicio

@Component({
	selector: 'app-home',
	standalone: true,
	imports: [],
	schemas: [CUSTOM_ELEMENTS_SCHEMA],
	templateUrl: './home.component.html',
	styleUrl: './home.component.css',
})
export class HomeComponent {
	@ViewChild('swiper') swiper?: ElementRef<SwiperContainer>;

	vistaCursos: HTMLDivElement[] = [];
	vista: HTMLDivElement | null = null;
	responsiveOptions = {
		0: {
			slidesPerView: 1,
		},
		320: {
			slidesPerView: 2,
		},
		640: {
			slidesPerView: 3,
		},
		960: {
			slidesPerView: 4,
		},
		1280: {
			slidesPerView: 5,
		},
	};

	autoplayOptions = {
		delay: 3000, // 3 segundos entre slides
		disableOnInteraction: false, // sigue reproduciendo aunque el usuario interactúe
		pauseOnMouseEnter: true, // pausa cuando el mouse está encima (ideal en desktop)
	};

	isBrowser = typeof window !== 'undefined';

	constructor(
		public cursoService: CursosService,
		private usuarioService: UsuariosService,
		public initService: InitService,
		private cdr: ChangeDetectorRef,
		public router: Router,
	) {
		afterNextRender(() => {
			this.carouselProfes();
			this.initSwiper();
		});
	}

	private initSwiper() {
		if (this.swiper?.nativeElement && this.cursoService.cursos.length > 0) {
			const swiperEl = this.swiper.nativeElement;
			const swiperParams = {
				...this.responsiveOptions,
				autoplay: this.autoplayOptions,
				navigation: {
					enabled: false,
				},
				pagination: {
					enabled: false,
				},
				loop: true,
			};

			Object.assign(swiperEl, swiperParams);
			swiperEl.initialize();
		} else {
			setTimeout(() => {
				this.initSwiper();
			}, 10);
		}
	}

	async carouselProfes() {
		setTimeout(async () => {
			const carouselProfes: HTMLDivElement[] = [];
			this.usuarioService.profes.forEach((profe: Usuario) => {
				const div: HTMLDivElement = document.createElement('div') as HTMLDivElement;
				const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
				if (isDarkMode) {
					div.classList.add('border', 'border-gray-400', 'rounded-lg', 'flex', 'h-full', 'p-2', 'flex-1', 'opacity-0', 'transition-opacity', 'duration-1000', 'items-center');
				} else {
					div.classList.add('border', 'border-black', 'rounded-lg', 'flex', 'h-full', 'p-2', 'flex-1', 'opacity-0', 'transition-opacity', 'duration-1000', 'items-center');
				}

				const img: HTMLImageElement = document.createElement('img') as HTMLImageElement;
				img.classList.add('h-1/2', 'sm:h-full', 'w-auto', 'object-contain', 'mr-4');
				img.src = profe.foto_usuario[0];
				img.alt = 'profe';
				div.appendChild(img);

				const desc: HTMLDivElement = document.createElement('div') as HTMLDivElement;
				desc.classList.add('flex', 'flex-col', 'flex-1', 'items-center', 'justify-center');

				const nombre: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
				nombre.classList.add('text-blond', 'text-green-700', 'text-center');
				nombre.textContent = profe.nombre_usuario.toString();
				desc.appendChild(nombre);

				const pres: HTMLParagraphElement = document.createElement('p') as HTMLParagraphElement;
				pres.classList.add('text-center');
				pres.textContent = profe.presentacion.toString();
				desc.appendChild(pres);

				div.appendChild(desc);
				carouselProfes.push(div);
			});

			const profes: HTMLDivElement = document.getElementById('profes') as HTMLDivElement;
			let reverse: boolean = false;
			while (carouselProfes.length > 0) {
				const profe: HTMLDivElement | undefined = carouselProfes.shift();
				if (profe) {
					profes.innerHTML = '';
					carouselProfes.push(profe);
					if (reverse) {
						profe.classList.add('flex-row-reverse');
					}
					profes.appendChild(profe);
					this.cdr.detectChanges();
					profe.classList.remove('opacity-0');
					reverse = !reverse;
					await this.delay(5000);
					profe.classList.add('opacity-0');
					await this.delay(1000);
				}
			}
		}, 200);
	}

	delay(ms: number): Promise<void> {
		return new Promise((resolve) => setTimeout(resolve, ms));
	}
}
