import { afterNextRender, ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { Router } from '@angular/router';
import Swiper from 'swiper';
import { Usuario } from '../../models/Usuario';
import { CursosService } from '../../services/cursos.service';
import { InitService } from '../../services/init.service';
import { UsuariosService } from '../../services/usuarios.service';

@Component({
	selector: 'app-home',
	standalone: true,
	imports: [],
	schemas: [CUSTOM_ELEMENTS_SCHEMA],
	templateUrl: './home.component.html',
	styleUrls: ['./home.component.css'], // corregido de styleUrl
})
export class HomeComponent {
	swiperInstance?: Swiper;

	isBrowser = globalThis.window !== undefined;

	constructor(
		public cursoService: CursosService,
		private readonly usuarioService: UsuariosService,
		public initService: InitService,
		private readonly cdr: ChangeDetectorRef,
		public router: Router,
	) {
		if (this.isBrowser) {
			afterNextRender(() => {
				this.carouselProfes();
				this.initSwiper();
			});
		}
	}

	async initSwiper() {
		// 0) sólo en cliente
		if (!this.isBrowser) return;

		// 1) obtener el contenedor
		const container = document.getElementById('swiper');
		if (!container) {
			console.warn('Swiper: contenedor #swiper no encontrado');
			return;
		}

		// 2) esperar a que haya slides (intenta inmediato y si no, observar mutaciones)
		const hasSlidesNow = () => {
			const wrapper = container.querySelector('.swiper-wrapper');
			return wrapper && wrapper.querySelectorAll('.swiper-slide').length > 0;
		};

		if (!hasSlidesNow()) {
			// esperar a que aparezcan slides (timeout 3s fallback)
			await new Promise<void>((resolve) => {
				const wrapper = container.querySelector('.swiper-wrapper');
				if (!wrapper) {
					// si no hay wrapper aún, observa el container
					const observer = new MutationObserver(() => {
						if (hasSlidesNow()) {
							observer.disconnect();
							resolve();
						}
					});
					observer.observe(container, { childList: true, subtree: true });
					// fallback timeout
					setTimeout(() => {
						observer.disconnect();
						resolve();
					}, 3000);
				} else {
					// wrapper existe pero slides no; observa wrapper
					const obs2 = new MutationObserver(() => {
						if (hasSlidesNow()) {
							obs2.disconnect();
							resolve();
						}
					});
					obs2.observe(wrapper, { childList: true });
					setTimeout(() => {
						obs2.disconnect();
						resolve();
					}, 3000);
				}
			});
		}

		// re-check
		const wrapperEl = container.querySelector('.swiper-wrapper');
		const slides = wrapperEl ? Array.from(wrapperEl.querySelectorAll('.swiper-slide')) : [];
		if (slides.length === 0) {
			console.warn('Swiper: no hay .swiper-slide al inicializar (se aborta)');
			return;
		}

		// 3) Import dinámico de Swiper (evita problemas SSR / side-effects)
		const SwiperModule = (await import('swiper')).default;
		const modules = await import('swiper/modules');
		const Autoplay = modules.Autoplay;
		const Navigation = modules.Navigation;
		const Pagination = modules.Pagination;

		// registrar módulos (según versión)
		SwiperModule.use?.([Autoplay, Navigation, Pagination]);

		// 4) finalmente crear la instancia (pasa un HTMLElement real)
		try {
			this.swiperInstance = new SwiperModule(container as HTMLElement, {
				slidesPerView: 'auto',
				loop: true,
				autoplay: {
					delay: 3000,
					disableOnInteraction: false,
					pauseOnMouseEnter: true,
				},
				navigation: false,
				pagination: false,
			});
		} catch (err) {
			console.error('Error inicializando Swiper:', err);
		}
	}

	async carouselProfes() {
		setTimeout(async () => {
			const carouselProfes: HTMLDivElement[] = [];

			this.usuarioService.profes.forEach((profe: Usuario) => {
				const div = document.createElement('div');
				const isDarkMode = globalThis.window.matchMedia && globalThis.window.matchMedia('(prefers-color-scheme: dark)').matches;

				div.classList.add('border', isDarkMode ? 'border-gray-400' : 'border-black', 'rounded-lg', 'flex', 'h-full', 'p-2', 'flex-1', 'opacity-0', 'transition-opacity', 'duration-1000', 'items-center');

				const img = document.createElement('img');
				img.classList.add('h-1/2', 'sm:h-full', 'w-auto', 'object-contain', 'mr-4');
				img.src = profe.foto_usuario[0];
				img.alt = 'profe';
				div.appendChild(img);

				const desc = document.createElement('div');
				desc.classList.add('flex', 'flex-col', 'flex-1', 'items-center', 'justify-center');

				const nombre = document.createElement('p');
				nombre.classList.add('text-blond', 'text-green-700', 'text-center');
				nombre.textContent = profe.nombre_usuario.toString();

				const pres = document.createElement('p');
				pres.classList.add('text-center');
				pres.textContent = profe.presentacion.toString();

				desc.appendChild(nombre);
				desc.appendChild(pres);
				div.appendChild(desc);

				carouselProfes.push(div);
			});

			const profes = document.getElementById('profes');
			if (!profes) {
				console.warn('Elemento #profes no encontrado en el DOM');
				return;
			}

			let reverse = false;
			while (carouselProfes.length > 0) {
				const profe = carouselProfes.shift();
				if (profe) {
					profes.innerHTML = '';
					carouselProfes.push(profe);
					if (reverse) {
						profe.classList.add('flex-row-reverse');
					} else {
						profe.classList.remove('flex-row-reverse');
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
