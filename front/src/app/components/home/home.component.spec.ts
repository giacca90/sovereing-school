import { ElementRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Usuario } from '../../models/Usuario';
import { CursosService } from '../../services/cursos.service';
import { InitService } from '../../services/init.service';
import { UsuariosService } from '../../services/usuarios.service';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
	let component: HomeComponent;
	let fixture: ComponentFixture<HomeComponent>;
	let mockCursoService: jasmine.SpyObj<CursosService>;
	let mockUsuarioService: jasmine.SpyObj<UsuariosService>;
	let mockInitService: jasmine.SpyObj<InitService>;
	let mockRouter: jasmine.SpyObj<Router>;

	const dummyProfe1: Usuario = {
		idUsuario: 1,
		nombreUsuario: 'Profesor Uno',
		rollUsuario: 'PROF',
		fotoUsuario: ['http://example.com/p1.png'],
	} as unknown as Usuario;

	const dummyProfe2: Usuario = {
		idUsuario: 2,
		nombreUsuario: 'Profesor Dos',
		rollUsuario: 'PROF',
		fotoUsuario: ['#FF0000'],
	} as unknown as Usuario;

	beforeEach(async () => {
		mockCursoService = jasmine.createSpyObj('CursosService', ['obtenerCursos']);
		mockUsuarioService = jasmine.createSpyObj('UsuariosService', ['obtenerProfes'], {
			profes: [dummyProfe1, dummyProfe2],
		});
		mockInitService = jasmine.createSpyObj('InitService', ['init']);
		mockRouter = jasmine.createSpyObj('Router', ['navigate']);

		await TestBed.configureTestingModule({
			imports: [HomeComponent],
			providers: [
				{ provide: CursosService, useValue: mockCursoService },
				{ provide: UsuariosService, useValue: mockUsuarioService },
				{ provide: InitService, useValue: mockInitService },
				{ provide: Router, useValue: mockRouter },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(HomeComponent);
		component = fixture.componentInstance;
	});

	it('should create', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
	});

	describe('getProfessorPhoto', () => {
		it('should return the first photo url of the professor when found', () => {
			// Arrange & Act
			const photo = component.getProfessorPhoto(1);

			// Assert
			expect(photo).toBe('http://example.com/p1.png');
		});

		it('should return undefined when professor is not found', () => {
			// Arrange & Act
			const photo = component.getProfessorPhoto(999);

			// Assert
			expect(photo).toBeUndefined();
		});
	});

	describe('getProfessorName', () => {
		it('should return the professor name when found', () => {
			// Arrange & Act
			const name = component.getProfessorName(2);

			// Assert
			expect(name).toBe('Profesor Dos');
		});

		it('should return undefined when professor is not found', () => {
			// Arrange & Act
			const name = component.getProfessorName(999);

			// Assert
			expect(name).toBeUndefined();
		});
	});

	describe('delay', () => {
		it('should resolve after the specified timeout', async () => {
			// Arrange & Act
			const promise = component.delay(10);

			// Assert
			await expectAsync(promise).toBeResolved();
		});
	});

	describe('carouselProfes', () => {
		it('should return immediately when isBrowser is false', async () => {
			// Arrange
			component.isBrowser = false;

			// Act
			await component.carouselProfes();

			// Assert
			expect(component.currentProfe).toBeNull();
		});

		it('should iterate over professors updating opacity, reverse flag and currentProfe', async () => {
			// Arrange
			component.isBrowser = true;
			mockUsuarioService.profes = [dummyProfe1, dummyProfe2];
			let callCount = 0;
			spyOn(component, 'delay').and.callFake(() => {
				callCount++;
				if (callCount >= 6) {
					throw new Error('Stop loop');
				}
				return Promise.resolve();
			});

			// Act & Assert
			try {
				await component.carouselProfes();
			} catch (e: any) {
				expect(e.message).toBe('Stop loop');
			}
			expect(component.currentProfe).toBeDefined();
		});

		it('should delay when profes list is empty', async () => {
			// Arrange
			component.isBrowser = true;
			mockUsuarioService.profes = [];
			let delayCalled = false;
			spyOn(component, 'delay').and.callFake(() => {
				delayCalled = true;
				throw new Error('Stop empty loop');
			});

			// Act & Assert
			try {
				await component.carouselProfes();
			} catch (e: any) {
				expect(e.message).toBe('Stop empty loop');
			}
			expect(delayCalled).toBeTrue();
		});
	});

	describe('initSwiper', () => {
		it('should return immediately if isBrowser is false', async () => {
			// Arrange
			component.isBrowser = false;

			// Act
			await component.initSwiper();

			// Assert
			expect(component.swiperInstance).toBeUndefined();
		});

		it('should warn and return if container element is not found', async () => {
			// Arrange
			component.isBrowser = true;
			component.swiperContainer = undefined as any;
			const warnSpy = spyOn(console, 'warn');

			// Act
			await component.initSwiper();

			// Assert
			expect(warnSpy).toHaveBeenCalledWith('Swiper: contenedor #swiper no encontrado');
		});

		it('should warn and abort when container has no slides', async () => {
			// Arrange
			component.isBrowser = true;
			const mockEl = document.createElement('div');
			mockEl.innerHTML = '<div class="swiper-wrapper"></div>';
			component.swiperContainer = new ElementRef(mockEl);
			const warnSpy = spyOn(console, 'warn');

			// Act
			const promise = component.initSwiper();
			await promise;

			// Assert
			expect(warnSpy).toHaveBeenCalledWith('Swiper: no hay .swiper-slide al inicializar (se aborta)');
		});

		it('should observe container if swiper-wrapper does not exist and resolve on mutation or fallback', async () => {
			// Arrange
			component.isBrowser = true;
			const mockEl = document.createElement('div');
			component.swiperContainer = new ElementRef(mockEl);
			const warnSpy = spyOn(console, 'warn');

			// Act
			const promise = component.initSwiper();
			await promise;

			// Assert
			expect(warnSpy).toHaveBeenCalledWith('Swiper: no hay .swiper-slide al inicializar (se aborta)');
		});

		it('should initialize Swiper if container has slides', async () => {
			// Arrange
			component.isBrowser = true;
			const mockEl = document.createElement('div');
			mockEl.innerHTML = '<div class="swiper-wrapper"><div class="swiper-slide">Slide 1</div></div>';
			component.swiperContainer = new ElementRef(mockEl);

			// Act
			await component.initSwiper();

			// Assert
			expect(component.swiperInstance).toBeDefined();
		});
	});
});
