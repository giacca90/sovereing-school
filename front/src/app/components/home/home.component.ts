import { afterNextRender, ChangeDetectionStrategy, ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA, ElementRef, ViewChild } from '@angular/core';
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
	changeDetection: ChangeDetectionStrategy.Eager,
	templateUrl: './home.component.html',
	styleUrls: ['./home.component.css'], // corregido de styleUrl
})
export class HomeComponent {
	swiperInstance?: Swiper;
	@ViewChild('swiperContainer') swiperContainer!: ElementRef<HTMLElement>;

	currentProfe: Usuario | null = null;
	isReverse: boolean = false;
	profeOpacity: number = 0;

	isBrowser = globalThis.window !== undefined;

	/**
	 * Constructor del componente.
	 * @param {CursosService} cursoService - Servicio de cursos.
	 * @param {UsuariosService} usuarioService - Servicio de usuarios.
	 * @param {InitService} initService - Servicio de inicialización.
	 * @param {ChangeDetectorRef} cdr - Detección de cambios.
	 * @param {Router} router - Router de Angular.
	 */
	constructor(
		public cursoService: CursosService,
		public usuarioService: UsuariosService,
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

	/**
	 * Inicializa el swiper.
	 */
	async initSwiper() {
		// 0) sólo en cliente
		if (!this.isBrowser) return;

		// 1) obtener el contenedor
		const container = this.swiperContainer?.nativeElement;
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
		if (!this.isBrowser) return;

		let index = 0;
		// eslint-disable-next-line no-constant-condition
		while (true) {
			if (this.usuarioService.profes.length > 0) {
				const profe = this.usuarioService.profes[index];
				this.currentProfe = profe;
				this.isReverse = index % 2 !== 0;
				this.cdr.detectChanges();

				// Fade in
				await this.delay(100);
				this.profeOpacity = 1;
				this.cdr.detectChanges();

				await this.delay(5000);

				// Fade out
				this.profeOpacity = 0;
				this.cdr.detectChanges();
				await this.delay(1000);

				index = (index + 1) % this.usuarioService.profes.length;
			} else {
				await this.delay(1000);
			}
		}
	}

	delay(ms: number): Promise<void> {
		return new Promise((resolve) => setTimeout(resolve, ms));
	}

	getProfessorPhoto(idProfe: number) {
		return this.usuarioService.profes.find((profe) => profe.idUsuario === idProfe)?.fotoUsuario[0];
	}
	getProfessorName(idProfe: number) {
		return this.usuarioService.profes.find((profe) => profe.idUsuario === idProfe)?.nombreUsuario;
	}
}
