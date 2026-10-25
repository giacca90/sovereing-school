import { Component, ElementRef, Input, PLATFORM_ID, Renderer2, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import videojs from 'video.js';
import { Clase } from '../../models/Clase';
import { Curso } from '../../models/Curso';
import { CursosService } from '../../services/cursos.service';
import { StreamingService } from '../../services/streaming.service';
import { ChatComponent } from '../chat/chat/chat.component';
import { ReproductionComponent } from './reproduction.component';

@Component({
	selector: 'app-chat',
	template: '<div>Mock Chat</div>',
})
class MockChatComponent {
	@Input() idCurso: number | null = null;
	creaPregunta = jasmine.createSpy('creaPregunta');
	chat = signal<any>({
		clases: [
			{
				idClase: 2,
				mensajes: [
					{ pregunta: 10, mensaje: '¿Qué es esto?', idMensaje: '123' },
					{ pregunta: null, mensaje: 'No pregunta', idMensaje: '456' },
					{ pregunta: 500, mensaje: 'Fuera de rango', idMensaje: '789' },
				],
			},
		],
	});
}

describe('ReproductionComponent', () => {
	let component: ReproductionComponent;
	let fixture: ComponentFixture<ReproductionComponent>;
	let mockCursosService: any;
	let mockStreamingService: any;
	let mockRouter: any;
	let paramsSubject: BehaviorSubject<any>;
	let queryParamsSubject: BehaviorSubject<any>;
	let renderer: Renderer2;

	const dummyCurso: Partial<Curso> = {
		idCurso: 1,
		nombreCurso: 'Curso Angular',
		imagenCurso: 'angular.jpg',
		clasesCurso: [
			{
				idClase: 2,
				nombreClase: 'Clase de Mocks',
				contenidoClase: 'Contenido detallado',
			} as Clase,
		],
	};

	beforeEach(async () => {
		paramsSubject = new BehaviorSubject<any>({ idCurso: '1', idClase: '2' });
		queryParamsSubject = new BehaviorSubject<any>({ momento: '45' });

		mockCursosService = {
			getStatusCurso: jasmine.createSpy('getStatusCurso').and.returnValue(of(2)),
			getCurso: jasmine.createSpy('getCurso').and.returnValue(Promise.resolve(dummyCurso as Curso)),
		};

		mockStreamingService = {
			registrarProgreso: jasmine.createSpy('registrarProgreso').and.returnValue(of({})),
		};

		mockRouter = {
			navigate: jasmine.createSpy('navigate'),
		};

		await TestBed.configureTestingModule({
			imports: [ReproductionComponent],
			providers: [
				{
					provide: ActivatedRoute,
					useValue: {
						params: paramsSubject.asObservable(),
						queryParams: queryParamsSubject.asObservable(),
					},
				},
				{ provide: CursosService, useValue: mockCursosService },
				{ provide: StreamingService, useValue: mockStreamingService },
				{ provide: Router, useValue: mockRouter },
				{ provide: PLATFORM_ID, useValue: 'browser' },
			],
		})
			.overrideComponent(ReproductionComponent, {
				remove: { imports: [ChatComponent] },
				add: { imports: [MockChatComponent] },
			})
			.compileComponents();

		fixture = TestBed.createComponent(ReproductionComponent);
		component = fixture.componentInstance;
		renderer = fixture.debugElement.injector.get(Renderer2);

		// Evitar la ejecución automática de getVideo durante los tests base
		spyOn<any>(component, 'getVideo').and.stub();
	});

	it('should create', () => {
		fixture.detectChanges();
		expect(component).toBeTruthy();
	});

	describe('ngOnInit y suscripción a rutas', () => {
		it('should load course and class data if idClase is not 0', () => {
			// Arrange
			spyOn(component, 'loadData');

			// Act
			fixture.detectChanges();

			// Assert
			expect(component.idCurso).toBe(1);
			expect(component.idClase).toBe(2);
			expect(component.loadData).toHaveBeenCalled();
		});

		it('should handle idClase === 0 and redirect to current class status', async () => {
			// Arrange
			paramsSubject.next({ idCurso: '1', idClase: '0' });

			// Act
			fixture.detectChanges();
			await new Promise((resolve) => setTimeout(resolve, 0));

			// Assert
			expect(mockCursosService.getStatusCurso).toHaveBeenCalledWith(1);
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/repro/1/2']);
		});

		it('should handle idClase === 0 and redirect to root if course status is not available', async () => {
			// Arrange
			spyOn(window, 'alert');
			mockCursosService.getStatusCurso.and.returnValue(of(null));
			paramsSubject.next({ idCurso: '1', idClase: '0' });

			// Act
			fixture.detectChanges();
			await new Promise((resolve) => setTimeout(resolve, 0));

			// Assert
			expect(window.alert).toHaveBeenCalledWith('Este curso no está disponible');
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
		});

		it('should log error when getStatusCurso fails', async () => {
			// Arrange
			spyOn(console, 'error');
			mockCursosService.getStatusCurso.and.returnValue(throwError(() => new Error('API Error')));
			paramsSubject.next({ idCurso: '1', idClase: '0' });

			// Act
			fixture.detectChanges();
			await new Promise((resolve) => setTimeout(resolve, 0));

			// Assert
			expect(console.error).toHaveBeenCalledWith('Error en recibir el estado del curso: API Error');
		});
	});

	describe('loadData', () => {
		it('should retrieve course and set active class', async () => {
			// Arrange
			component.idCurso = 1;
			component.idClase = 2;

			// Act
			await component.loadData();

			// Assert
			expect(mockCursosService.getCurso).toHaveBeenCalledWith(1);
			expect(component.curso()).toEqual(dummyCurso as Curso);
			expect(component.clase()).toEqual(dummyCurso.clasesCurso![0]);
			expect(component['getVideo']).toHaveBeenCalled();
		});

		it('should not set active class if class is not found in course classes', async () => {
			// Arrange
			component.idCurso = 1;
			component.idClase = 999; // Clase inexistente

			// Act
			await component.loadData();

			// Assert
			expect(component.clase()).toBeNull();
		});
	});

	describe('host listener e interacciones de usuario', () => {
		it('should close context menu on document click if open', () => {
			// Arrange
			component.contextMenu.set({ x: 100, y: 200, time: 30 });

			// Act
			component.onDocumentClick(new MouseEvent('click'));

			// Assert
			expect(component.contextMenu()).toBeNull();
		});

		it('should remain null on document click if context menu is already closed', () => {
			// Arrange
			component.contextMenu.set(null);

			// Act
			component.onDocumentClick(new MouseEvent('click'));

			// Assert
			expect(component.contextMenu()).toBeNull();
		});

		it('should change views', () => {
			// Arrange & Act
			component.cambiaVista(1);

			// Assert
			expect(component.vista()).toBe(1);
		});

		it('should set contextMenu coordinates when muestraCortina is called', () => {
			// Arrange & Act
			component['muestraCortina'](150, 250, 12.5);

			// Assert
			expect(component.contextMenu()).toEqual({
				x: 150,
				y: 250 + (globalThis.window?.scrollY || 0),
				time: 12.5,
			});
		});

		it('should open chat view, create question and reset context menu when clickPregunta is called', () => {
			// Arrange
			component.contextMenu.set({ x: 100, y: 200, time: 35 });
			component.idClase = 2;
			const mockChat = new MockChatComponent() as any;
			(component as any).chatComponent = signal(mockChat);

			// Act
			component.clickPregunta();

			// Assert
			expect(component.vista()).toBe(1);
			expect(mockChat.creaPregunta).toHaveBeenCalledWith(2, 35);
			expect(component.contextMenu()).toBeNull();
		});
	});

	describe('navega', () => {
		it('should navigate to class reproduction url', () => {
			// Arrange
			component.idCurso = 1;
			const targetClase = { idClase: 5 } as Clase;

			// Act
			component.navega(targetClase);

			// Assert
			expect(mockRouter.navigate).toHaveBeenCalledWith(['repro/1/5']);
		});
	});

	describe('esperarChatComponent y marcadores de preguntas', () => {
		it('should wait for chat and add markers to seekBar', async () => {
			// Arrange
			component.idClase = 2;
			const mockChat = new MockChatComponent() as any;
			(component as any).chatComponent = signal(mockChat);

			const mockSeekBarElement = document.createElement('div');
			spyOn(mockSeekBarElement, 'getBoundingClientRect').and.returnValue({
				width: 1000,
				left: 0,
			} as DOMRect);

			const mockPlayer = {
				duration: jasmine.createSpy('duration').and.returnValue(100),
				getChild: jasmine.createSpy('getChild').and.returnValue({
					getChild: jasmine.createSpy('getChild').and.returnValue({
						getChild: jasmine.createSpy('getChild').and.returnValue({
							el: () => mockSeekBarElement,
						}),
					}),
				}),
			} as any;

			// Act
			component.esperarChatComponent(mockPlayer);
			await new Promise((resolve) => setTimeout(resolve, 0));

			// Assert
			expect(mockPlayer.duration).toHaveBeenCalled();
			const marker = mockSeekBarElement.querySelector('div');
			expect(marker).toBeTruthy();
			expect(marker?.style.left).toBe('100px'); // 10% of 1000px (10s / 100s)

			// Simular mouseover en el marcador
			const mouseOverEvent = new MouseEvent('mouseover', { clientX: 200, clientY: 300 });
			marker?.dispatchEvent(mouseOverEvent);
			expect(component.hoveredMarker()).toEqual({
				message: '¿Qué es esto?',
				x: 200,
				y: 320,
				idMensaje: '123',
			});

			// Simular mouseout en el marcador
			marker?.dispatchEvent(new MouseEvent('mouseout'));
			await new Promise((resolve) => setTimeout(resolve, 1000));
			expect(component.hoveredMarker()).toBeNull();
		});

		it('should handle missing chatComponent chat without crashing and wait', async () => {
			// Arrange
			component.idClase = 2;
			const mockChat = new MockChatComponent() as any;
			mockChat.chat.set(null); // Inicialmente null
			(component as any).chatComponent = signal(mockChat);

			const mockPlayer = {
				duration: jasmine.createSpy('duration').and.returnValue(100),
				getChild: jasmine.createSpy('getChild').and.returnValue({
					getChild: jasmine.createSpy('getChild').and.returnValue({
						getChild: jasmine.createSpy('getChild').and.returnValue({
							el: () => document.createElement('div'),
						}),
					}),
				}),
			} as any;

			// Act
			component.esperarChatComponent(mockPlayer);

			// Simular que el chat carga después de 300ms
			await new Promise((resolve) => setTimeout(resolve, 300));
			mockChat.chat.set({
				clases: [],
			} as any);
			await new Promise((resolve) => setTimeout(resolve, 300));

			// Assert
			expect(mockPlayer.duration).not.toHaveBeenCalled(); // No clases match idClase
		});
	});

	describe('getVideo real execution coverage', () => {
		beforeEach(() => {
			((component as any).getVideo as jasmine.Spy).and.callThrough();
		});

		it('should return early if not in browser', async () => {
			// Arrange
			(component as any).isBrowser = false;
			spyOn(console, 'warn');

			// Act
			await (component as any).getVideo();

			// Assert
			expect(console.warn).not.toHaveBeenCalled();
		});

		it('should log warning if videoElement is not found', async () => {
			// Arrange
			(component as any).isBrowser = true;
			(component as any).videoElement = signal({ nativeElement: null } as any);
			spyOn(console, 'warn');

			// Act
			await (component as any).getVideo();

			// Assert
			expect(console.warn).toHaveBeenCalledWith('Elemento de video no encontrado');
		});

		it('should initialize player, quality selector, and listeners via Video.js getPlayer hook', async () => {
			(component as any).isBrowser = true;
			const videoEl = document.createElement('video');
			(component as any).videoElement = signal(new ElementRef(videoEl));
			queryParamsSubject.next({ momento: '5' });
			component.backStream = 'http://stream';
			component.idCurso = 1;
			component.idClase = 2;
			component.momento = 5;

			const qualityListeners: Record<string, Function> = {};
			const mockQualityLevels: any = [
				{ height: 720, enabled: false },
				{ height: 1080, enabled: false },
			];
			mockQualityLevels.on = (evt: string, cb: Function) => {
				qualityListeners[evt] = cb;
			};

			const playerEvents: Record<string, Function> = {};
			const seekbarEvents: Record<string, Function> = {};
			const mockSeekBar = {
				el: () => {
					const el = document.createElement('div');
					spyOn(el, 'getBoundingClientRect').and.returnValue({ left: 10, width: 200 } as DOMRect);
					return el;
				},
				on: (evt: string, cb: Function) => {
					seekbarEvents[evt] = cb;
				},
			};

			const controlBarEl = document.createElement('div');
			const mockPlayer: any = {
				src: jasmine.createSpy('src'),
				ready: jasmine.createSpy('ready').and.callFake((cb: Function) => cb()),
				tech: () => ({
					vhs: {
						autoLevelEnabled: false,
						bandwidthEstimator: { sample: jasmine.createSpy('sample') },
					},
				}),
				qualityLevels: () => mockQualityLevels,
				getChild: jasmine.createSpy('getChild').and.callFake((name: string) => {
					if (name === 'ControlBar') {
						return {
							el: () => controlBarEl,
							getChild: (cName: string) => {
								if (cName === 'ProgressControl') {
									return {
										getChild: (sName: string) => (sName === 'SeekBar' ? mockSeekBar : null),
									};
								}
								return null;
							},
						};
					}
					return null;
				}),
				currentTime: jasmine.createSpy('currentTime').and.returnValues(10, 10, 10),
				duration: jasmine.createSpy('duration').and.returnValue(100),
				buffered: () => ({
					length: 1,
					start: () => 0,
					end: () => 50,
				}),
				one: jasmine.createSpy('one').and.callFake((evt: string, cb: Function) => cb()),
				on: (evt: string, cb: Function) => {
					playerEvents[evt] = cb;
				},
			};

			spyOn(videojs, 'getPlayer').and.returnValue(mockPlayer as any);

			await (component as any).getVideo();

			expect(mockPlayer.src).toHaveBeenCalled();
			expect(mockPlayer.ready).toHaveBeenCalled();
			expect(mockPlayer.currentTime).toHaveBeenCalledWith(2); // momento: 5 > 3 => 5 - 3 = 2
			expect(mockRouter.navigate).toHaveBeenCalledWith([], {
				queryParams: { momento: null },
				queryParamsHandling: 'merge',
				replaceUrl: true,
			});
			expect(component.loading()).toBeFalse();
		});

		it('should handle momento <= 3 when setting currentTime in getVideo', async () => {
			(component as any).isBrowser = true;
			const videoEl = document.createElement('video');
			(component as any).videoElement = signal(new ElementRef(videoEl));
			queryParamsSubject.next({ momento: '2' });
			component.backStream = 'http://stream/test.m3u8';
			component.momento = 2; // <= 3

			const mockPlayer = {
				src: jasmine.createSpy('src'),
				ready: jasmine.createSpy('ready').and.callFake((cb: Function) => cb()),
				tech: () => ({ vhs: {} }),
				qualityLevels: () => {
					const ql: any = [];
					ql.on = () => {};
					return ql;
				},
				getChild: () => ({ el: () => document.createElement('div'), getChild: () => null }),
				currentTime: jasmine.createSpy('currentTime'),
				duration: jasmine.createSpy('duration').and.returnValue(100),
				one: jasmine.createSpy('one'),
				on: jasmine.createSpy('on'),
			};

			spyOn(videojs, 'getPlayer').and.returnValue(mockPlayer as any);

			await (component as any).getVideo();

			expect(mockPlayer.currentTime).toHaveBeenCalledWith(2);
			expect(mockRouter.navigate).toHaveBeenCalledWith([], {
				queryParams: { momento: null },
				queryParamsHandling: 'merge',
				replaceUrl: true,
			});
		});

		it('should handle quality selector creation, item clicks (auto and specific height), and seek fallback', async () => {
			(component as any).isBrowser = true;
			const videoEl = document.createElement('video');
			(component as any).videoElement = signal(new ElementRef(videoEl));
			component.backStream = 'http://stream/test.m3u8';
			component.momento = null;

			const qualityLevels: any = [
				{ height: 720, enabled: true },
				{ height: 1080, enabled: true },
			];
			let addQualityLevelCb: Function = () => {};
			qualityLevels.on = (evt: string, cb: Function) => {
				if (evt === 'addqualitylevel') {
					addQualityLevelCb = cb;
				}
			};

			const controlBarEl = document.createElement('div');
			const mockControlBar = {
				el: () => controlBarEl,
				getChild: () => null,
			};

			const bandwidthEstimator = { sample: jasmine.createSpy('sample') };
			let loadedDataCb: Function = () => {};
			let timeupdateCb: Function = () => {};
			let playCb: Function = () => {};
			let seekedCb: Function = () => {};

			const mockPlayer = {
				src: jasmine.createSpy('src'),
				ready: jasmine.createSpy('ready').and.callFake((cb: Function) => cb()),
				tech: () => ({
					vhs: {
						autoLevelEnabled: true,
						bandwidthEstimator,
					},
				}),
				qualityLevels: () => qualityLevels,
				getChild: (name: string) => (name === 'ControlBar' ? mockControlBar : null),
				currentTime: jasmine.createSpy('currentTime').and.returnValues(50, 50, 50),
				duration: jasmine.createSpy('duration').and.returnValue(100),
				buffered: () => ({
					length: 1,
					start: () => 0,
					end: () => 10, // 50 is NOT buffered => seek fallback triggered
				}),
				one: (evt: string, cb: Function) => {
					if (evt === 'seeked') seekedCb = cb;
				},
				on: (evt: string, cb: Function) => {
					if (evt === 'timeupdate') timeupdateCb = cb;
					if (evt === 'play') playCb = cb;
				},
			};

			spyOn(videojs, 'getPlayer').and.returnValue(mockPlayer as any);

			await (component as any).getVideo();

			// Trigger addqualitylevel to build selector and highlight
			addQualityLevelCb();
			// Trigger a duplicate addqualitylevel to test early return when !changes
			addQualityLevelCb();

			const selectorEl = controlBarEl.querySelector('#vjs-quality-selector') as HTMLElement;
			expect(selectorEl).toBeTruthy();
			const btn = selectorEl.querySelector('button') as HTMLButtonElement;
			const menu = selectorEl.querySelector('div') as HTMLElement;

			// Click toggle button
			btn.click();
			expect(menu.style.display).toBe('block');

			// Click toggle button again to hide (branch !isHidden)
			btn.click();
			expect(menu.style.display).toBe('none');

			// Reopen and click document to hide via document listener
			btn.click();
			expect(menu.style.display).toBe('block');
			document.dispatchEvent(new MouseEvent('click'));
			expect(menu.style.display).toBe('none');

			// Reopen for item clicks
			btn.click();

			// Click auto item
			const autoItem = menu.querySelector('[data-quality="auto"]') as HTMLElement;
			autoItem.click();
			expect(qualityLevels[0].enabled).toBeTrue();
			expect(qualityLevels[1].enabled).toBeTrue();
			expect(bandwidthEstimator.sample).toHaveBeenCalled();

			// Click specific height item (720)
			jasmine.clock().install();
			try {
				const q720Item = menu.querySelector('[data-quality="720"]') as HTMLElement;
				q720Item.click();
				expect(qualityLevels[0].enabled).toBeTrue();
				expect(qualityLevels[1].enabled).toBeFalse();
				expect(mockPlayer.currentTime).toHaveBeenCalledWith(50.1);

				// Simulate seeked callback and tick 100ms
				seekedCb();
				jasmine.clock().tick(100);
				expect(mockPlayer.currentTime).toHaveBeenCalledWith(50);
			} finally {
				jasmine.clock().uninstall();
			}
		});

		it('should handle quality selector when bandwidthEstimator is null and buffer covers currentTime', async () => {
			(component as any).isBrowser = true;
			const videoEl = document.createElement('video');
			(component as any).videoElement = signal(new ElementRef(videoEl));
			component.backStream = 'http://stream/test.m3u8';

			const qualityLevels: any = [{ height: 480, enabled: true }];
			qualityLevels.on = (evt: string, cb: Function) => {
				if (evt === 'addqualitylevel') cb();
			};

			const controlBarEl = document.createElement('div');
			const mockControlBar = {
				el: () => controlBarEl,
				getChild: () => null,
			};

			let loadedDataCb: Function = () => {};
			const mockPlayer = {
				src: jasmine.createSpy('src'),
				ready: jasmine.createSpy('ready').and.callFake((cb: Function) => cb()),
				tech: () => ({
					vhs: {
						bandwidthEstimator: null,
					},
				}),
				qualityLevels: () => qualityLevels,
				getChild: (name: string) => (name === 'ControlBar' ? mockControlBar : null),
				currentTime: jasmine.createSpy('currentTime').and.returnValues(5, 5),
				duration: jasmine.createSpy('duration').and.returnValue(100),
				buffered: () => ({
					length: 1,
					start: () => 0,
					end: () => 10, // 5 IS buffered [0, 10] => inBuffer is true
				}),
				one: (evt: string, cb: Function) => {
					if (evt === 'loadeddata') loadedDataCb = cb;
				},
				on: jasmine.createSpy('on'),
			};

			spyOn(videojs, 'getPlayer').and.returnValue(mockPlayer as any);

			await (component as any).getVideo();

			const menu = controlBarEl.querySelector('#vjs-quality-selector div') as HTMLElement;
			const autoItem = menu.querySelector('[data-quality="auto"]') as HTMLElement;
			autoItem.click();

			const q480Item = menu.querySelector('[data-quality="480"]') as HTMLElement;
			q480Item.click();

			// loadeddata when time is already in buffer
			loadedDataCb();
			// When inBuffer is true, currentTime is NOT called with originalTime inside loadeddata
			expect(mockPlayer.currentTime).not.toHaveBeenCalledWith(5);
		});

		it('should reuse existing quality selector if already present in DOM', async () => {
			(component as any).isBrowser = true;
			const videoEl = document.createElement('video');
			(component as any).videoElement = signal(new ElementRef(videoEl));
			component.backStream = 'http://stream/test.m3u8';

			const qualityLevels: any = [{ height: 720, enabled: true }];
			qualityLevels.on = (evt: string, cb: Function) => {
				if (evt === 'addqualitylevel') cb();
			};

			const controlBarEl = document.createElement('div');
			const existingSelector = document.createElement('div');
			existingSelector.id = 'vjs-quality-selector';
			controlBarEl.appendChild(existingSelector);

			const mockControlBar = {
				el: () => controlBarEl,
				getChild: () => null,
			};

			const mockPlayer = {
				src: jasmine.createSpy('src'),
				ready: jasmine.createSpy('ready').and.callFake((cb: Function) => cb()),
				tech: () => ({ vhs: {} }),
				qualityLevels: () => qualityLevels,
				getChild: (name: string) => (name === 'ControlBar' ? mockControlBar : null),
				currentTime: jasmine.createSpy('currentTime').and.returnValue(10),
				duration: jasmine.createSpy('duration').and.returnValue(100),
				buffered: () => ({ length: 0, start: () => 0, end: () => 0 }),
				one: jasmine.createSpy('one'),
				on: jasmine.createSpy('on'),
				trigger: jasmine.createSpy('trigger'),
			};

			spyOn(videojs, 'getPlayer').and.returnValue(mockPlayer as any);

			await (component as any).getVideo();
			expect(controlBarEl.querySelectorAll('#vjs-quality-selector').length).toBe(1);
		});

		it('should jump before start buffer when jump exceeds duration', async () => {
			(component as any).isBrowser = true;
			const videoEl = document.createElement('video');
			(component as any).videoElement = signal(new ElementRef(videoEl));
			component.backStream = 'http://stream/test.m3u8';

			const qualityLevels: any = [{ height: 1080, enabled: true }];
			qualityLevels.on = (evt: string, cb: Function) => {
				if (evt === 'addqualitylevel') cb();
			};

			const controlBarEl = document.createElement('div');
			const mockControlBar = {
				el: () => controlBarEl,
				getChild: () => null,
			};

			const mockPlayer = {
				src: jasmine.createSpy('src'),
				ready: jasmine.createSpy('ready').and.callFake((cb: Function) => cb()),
				tech: () => ({ vhs: {} }),
				qualityLevels: () => qualityLevels,
				getChild: (name: string) => (name === 'ControlBar' ? mockControlBar : null),
				currentTime: jasmine.createSpy('currentTime').and.returnValue(99.98),
				duration: jasmine.createSpy('duration').and.returnValue(100),
				buffered: () => ({
					length: 1,
					start: () => 10,
					end: () => 99.98,
				}),
				one: jasmine.createSpy('one'),
				on: jasmine.createSpy('on'),
				trigger: jasmine.createSpy('trigger'),
			};

			spyOn(videojs, 'getPlayer').and.returnValue(mockPlayer as any);

			await (component as any).getVideo();

			const menu = controlBarEl.querySelector('#vjs-quality-selector div') as HTMLElement;
			const item = menu.querySelector('[data-quality="1080"]') as HTMLElement;
			item.click();

			expect(mockPlayer.currentTime).toHaveBeenCalledWith(9.95);
		});

		it('should catch and log error if video initialization throws', async () => {
			(component as any).isBrowser = true;
			const videoEl = document.createElement('video');
			(component as any).videoElement = signal(new ElementRef(videoEl));
			component.backStream = 'http://stream/test.m3u8';

			spyOn(videojs, 'getPlayer').and.throwError(new Error('Init fail'));
			spyOn(console, 'error');

			await (component as any).getVideo();

			expect(console.error).toHaveBeenCalledWith('Error loading video:', jasmine.any(Error));
		});

		it('should attach contextmenu event to seekBar and open context menu if duration > 0', async () => {
			(component as any).isBrowser = true;
			const videoEl = document.createElement('video');
			(component as any).videoElement = signal(new ElementRef(videoEl));
			component.backStream = 'http://stream/test.m3u8';

			const seekBarEl = document.createElement('div');
			spyOn(seekBarEl, 'getBoundingClientRect').and.returnValue({
				left: 10,
				width: 200,
			} as DOMRect);

			let contextMenuCb: Function = () => {};
			const mockSeekBar = {
				el: () => seekBarEl,
				on: (evt: string, cb: Function) => {
					if (evt === 'contextmenu') contextMenuCb = cb;
				},
			};

			const mockControlBar = {
				el: () => document.createElement('div'),
				getChild: (name: string) => (name === 'ProgressControl' ? { getChild: (s: string) => (s === 'SeekBar' ? mockSeekBar : null) } : null),
			};

			const mockPlayer = {
				src: jasmine.createSpy('src'),
				ready: jasmine.createSpy('ready').and.callFake((cb: Function) => cb()),
				tech: () => ({ vhs: {} }),
				qualityLevels: () => {
					const ql: any = [];
					ql.on = () => {};
					return ql;
				},
				getChild: (name: string) => (name === 'ControlBar' ? mockControlBar : null),
				currentTime: jasmine.createSpy('currentTime').and.returnValue(0),
				duration: jasmine.createSpy('duration').and.returnValues(100, 0),
				buffered: () => ({ length: 0, start: () => 0, end: () => 0 }),
				one: jasmine.createSpy('one'),
				on: jasmine.createSpy('on'),
			};

			spyOn(videojs, 'getPlayer').and.returnValue(mockPlayer as any);
			spyOn(component as any, 'muestraCortina');

			await (component as any).getVideo();

			// First trigger: duration = 100 > 0 -> calls muestraCortina
			const event1 = new MouseEvent('contextmenu', { clientX: 60, clientY: 40 });
			contextMenuCb(event1);
			expect((component as any).muestraCortina).toHaveBeenCalledWith(60, 40, (50 / 200) * 100);

			// Second trigger: duration = 0 -> does not call muestraCortina
			(component as any).muestraCortina.calls.reset();
			const event2 = new MouseEvent('contextmenu', { clientX: 60, clientY: 40 });
			contextMenuCb(event2);
			expect((component as any).muestraCortina).not.toHaveBeenCalled();
		});

		it('should trigger player.on play and timeupdate events, handling progress error and segment caching', async () => {
			(component as any).isBrowser = true;
			const videoEl = document.createElement('video');
			(component as any).videoElement = signal(new ElementRef(videoEl));
			component.backStream = 'http://stream/test.m3u8';
			component.idCurso = 1;
			component.idClase = 2;

			let timeupdateCb: Function = () => {};
			let playCb: Function = () => {};
			let mockCurrentTime: any = 25;

			const mockPlayer = {
				src: jasmine.createSpy('src'),
				ready: jasmine.createSpy('ready').and.callFake((cb: Function) => cb()),
				tech: () => ({ vhs: {} }),
				qualityLevels: () => {
					const ql: any = [];
					ql.on = () => {};
					return ql;
				},
				getChild: () => ({ el: () => document.createElement('div'), getChild: () => null }),
				currentTime: jasmine.createSpy('currentTime').and.callFake((val?: number) => {
					if (val !== undefined) mockCurrentTime = val;
					return mockCurrentTime;
				}),
				duration: jasmine.createSpy('duration').and.returnValue(100),
				buffered: () => ({ length: 0, start: () => 0, end: () => 0 }),
				one: jasmine.createSpy('one'),
				on: (evt: string, cb: Function) => {
					if (evt === 'timeupdate') timeupdateCb = cb;
					if (evt === 'play') playCb = cb;
				},
			};

			spyOn(videojs, 'getPlayer').and.returnValue(mockPlayer as any);
			spyOn(component, 'esperarChatComponent');
			spyOn(console, 'error');

			await (component as any).getVideo();

			// Play triggers esperarChatComponent
			playCb();
			expect(component.esperarChatComponent).toHaveBeenCalledWith(mockPlayer as any);

			// Timeupdate 1: currentTime = 25 -> segmentIndex = Math.floor(25 / 2) = 12
			// Calls registrarProgreso with success
			mockCurrentTime = 25;
			mockStreamingService.registrarProgreso.and.returnValue(of({}));
			timeupdateCb();
			expect(mockStreamingService.registrarProgreso).toHaveBeenCalledWith(1, 2, 12);

			// Timeupdate 2: currentTime = 25.5 -> segmentIndex = Math.floor(25.5 / 2) = 12 === lastSegmentReported -> skips call
			mockCurrentTime = 25.5;
			mockStreamingService.registrarProgreso.calls.reset();
			timeupdateCb();
			expect(mockStreamingService.registrarProgreso).not.toHaveBeenCalled();

			// Timeupdate 3: currentTime = undefined -> skips call
			mockCurrentTime = undefined;
			timeupdateCb();
			expect(mockStreamingService.registrarProgreso).not.toHaveBeenCalled();

			// Timeupdate 4: currentTime = 45 -> segmentIndex = Math.floor(45 / 2) = 22, with error in registrarProgreso
			mockCurrentTime = 45;
			(console.error as jasmine.Spy).calls.reset();
			mockStreamingService.registrarProgreso.and.returnValue(throwError(() => new Error('Progreso error')));
			timeupdateCb();
			expect(console.error).toHaveBeenCalledWith('Error al registrar progreso:', jasmine.any(Error));
		});
	});

	describe('esperarChatComponent', () => {
		it('should stop retrying after 30 attempts if chat is never ready', async () => {
			(component as any).chatComponent = signal(null);
			const mockPlayer = { duration: jasmine.createSpy('duration') };

			component.esperarChatComponent(mockPlayer as any);
			await new Promise((resolve) => setTimeout(resolve, 3500)); // 35 * 100ms > 30 retries

			expect(mockPlayer.duration).not.toHaveBeenCalled();
		});

		it('should handle when chat.clases does not contain current idClase', async () => {
			component.idClase = 999;
			const mockChat = new MockChatComponent() as any;
			mockChat.chat.set({
				clases: [
					{
						idClase: 1,
						mensajes: [{ pregunta: 10, mensaje: 'Test', idMensaje: '1' } as any],
					},
				],
			} as any);
			(component as any).chatComponent = signal(mockChat);

			const mockPlayer = { duration: jasmine.createSpy('duration') };

			component.esperarChatComponent(mockPlayer as any);
			await new Promise((resolve) => setTimeout(resolve, 150));

			expect(mockPlayer.duration).not.toHaveBeenCalled();
		});

		it('should filter out invalid messages (pregunta null or > duration) and handle missing seekBar', async () => {
			component.idClase = 2;
			const mockChat = new MockChatComponent() as any;
			mockChat.chat.set({
				clases: [
					{
						idClase: 2,
						mensajes: [{ pregunta: null, mensaje: 'Sin tiempo' } as any, { pregunta: 150, mensaje: 'Fuera de duracion' } as any, { pregunta: 10, mensaje: 'Valido' } as any],
					},
				],
			} as any);
			(component as any).chatComponent = signal(mockChat);

			const mockPlayer = {
				duration: jasmine.createSpy('duration').and.returnValue(100),
				getChild: jasmine.createSpy('getChild').and.returnValue(null), // No ControlBar
			};

			component.esperarChatComponent(mockPlayer as any);
			await new Promise((resolve) => setTimeout(resolve, 150));

			expect(mockPlayer.duration).toHaveBeenCalled();
			expect(mockPlayer.getChild).toHaveBeenCalledWith('ControlBar');
		});

		it('should create question markers and handle mouseover/mouseout listeners', async () => {
			component.idClase = 2;
			const mockChat = new MockChatComponent() as any;
			mockChat.chat.set({
				clases: [
					{
						idClase: 2,
						mensajes: [{ pregunta: 25, mensaje: 'Pregunta en 25s', idMensaje: 'msg-1' } as any, { pregunta: 50, mensaje: null, idMensaje: null } as any],
					},
				],
			} as any);
			(component as any).chatComponent = signal(mockChat);

			const seekBarEl = document.createElement('div');
			spyOn(seekBarEl, 'getBoundingClientRect').and.returnValue({ width: 400 } as DOMRect);

			const mockSeekBar = {
				el: () => seekBarEl,
			};
			const mockControlBar = {
				getChild: (n: string) => (n === 'ProgressControl' ? { getChild: (s: string) => (s === 'SeekBar' ? mockSeekBar : null) } : null),
			};
			const mockPlayer = {
				duration: jasmine.createSpy('duration').and.returnValue(100),
				getChild: (name: string) => (name === 'ControlBar' ? mockControlBar : null),
			};

			component.esperarChatComponent(mockPlayer as any);
			await new Promise((resolve) => setTimeout(resolve, 150));

			const markers = seekBarEl.querySelectorAll('div');
			expect(markers.length).toBe(2);

			const firstMarker = markers[0];
			// Mouseover on first marker
			firstMarker.dispatchEvent(new MouseEvent('mouseover', { clientX: 100, clientY: 200 }));
			expect(component.hoveredMarker()).toEqual({
				message: 'Pregunta en 25s',
				x: 100,
				y: 220,
				idMensaje: 'msg-1',
			});

			// Mouseout triggers 1000ms timeout to clear hoveredMarker
			firstMarker.dispatchEvent(new MouseEvent('mouseout'));
			expect(component.hoveredMarker()).not.toBeNull();
			await new Promise((resolve) => setTimeout(resolve, 1000));
			expect(component.hoveredMarker()).toBeNull();

			// Mouseover on second marker (mensaje is null -> fallback '')
			const secondMarker = markers[1];
			secondMarker.dispatchEvent(new MouseEvent('mouseover', { clientX: 150, clientY: 220 }));
			expect(component.hoveredMarker()?.message).toBe('');
			expect(component.hoveredMarker()?.idMensaje).toBe('');
			expect(component.hoveredMarker()?.y).toBe(240);

			secondMarker.dispatchEvent(new MouseEvent('mouseout'));
			await new Promise((resolve) => setTimeout(resolve, 1000));
			expect(component.hoveredMarker()).toBeNull();
		});
	});

	describe('loadData edge cases', () => {
		it('should handle null curso', async () => {
			mockCursosService.getCurso.and.returnValue(Promise.resolve(null));
			component.idCurso = 1;
			component.idClase = 2;

			await component.loadData();

			expect(component.curso()).toBeNull();
			expect(component.clase()).toBeNull();
		});

		it('should handle null clasesCurso', async () => {
			mockCursosService.getCurso.and.returnValue(Promise.resolve({ idCurso: 1, clasesCurso: null } as any));
			component.idCurso = 1;
			component.idClase = 2;

			await component.loadData();

			expect(component.clase()).toBeNull();
		});

		it('should handle clase not found in clasesCurso', async () => {
			mockCursosService.getCurso.and.returnValue(
				Promise.resolve({
					idCurso: 1,
					clasesCurso: [{ idClase: 99 }],
				} as any),
			);
			component.idCurso = 1;
			component.idClase = 2;

			await component.loadData();

			expect(component.clase()).toBeNull();
		});

		it('should catch and log error when getCurso rejects', async () => {
			mockCursosService.getCurso.and.returnValue(Promise.reject('Network Error'));
			spyOn(console, 'error');
			component.idCurso = 1;
			component.idClase = 2;

			await component.loadData();

			expect(console.error).toHaveBeenCalledWith('Error loading curso:', 'Network Error');
		});
	});

	describe('ngOnInit y router params edge cases', () => {
		it('should navigate to root when idClase is 0 and resp is 0', async () => {
			mockCursosService.getStatusCurso.and.returnValue(of(0));
			spyOn(window, 'alert');
			(component as any).route.params = of({ idCurso: '1', idClase: '0' });

			component.ngOnInit();
			await new Promise((resolve) => setTimeout(resolve, 0));

			expect(window.alert).toHaveBeenCalledWith('Este curso no está disponible');
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
		});

		it('should navigate to valid class when idClase is 0 and resp > 0', async () => {
			mockCursosService.getStatusCurso.and.returnValue(of(4));
			(component as any).route.params = of({ idCurso: '1', idClase: '0' });

			component.ngOnInit();
			await new Promise((resolve) => setTimeout(resolve, 0));

			expect(mockRouter.navigate).toHaveBeenCalledWith(['/repro/1/4']);
		});

		it('should log error when getStatusCurso fails', async () => {
			mockCursosService.getStatusCurso.and.returnValue(throwError(() => new Error('Status service fail')));
			spyOn(console, 'error');
			(component as any).route.params = of({ idCurso: '1', idClase: '0' });

			component.ngOnInit();
			await new Promise((resolve) => setTimeout(resolve, 0));

			expect(console.error).toHaveBeenCalledWith('Error en recibir el estado del curso: Status service fail');
		});

		it('should set momento to null if queryParam momento is invalid NaN', () => {
			(component as any).route.queryParams = of({ momento: 'invalid-number' });
			component.ngOnInit();
			expect(component.momento).toBeNull();
		});

		it('should set momento correctly if queryParam momento is valid number', () => {
			(component as any).route.queryParams = of({ momento: '45' });
			component.ngOnInit();
			expect(component.momento).toBe(45);
		});
	});

	describe('UI interactions and helper methods', () => {
		it('should unsubscribe on ngOnDestroy', () => {
			fixture.detectChanges();
			const subSpy = spyOn((component as any).subscription, 'unsubscribe');
			component.ngOnDestroy();
			expect(subSpy).toHaveBeenCalled();
		});

		it('should change view via cambiaVista', () => {
			component.cambiaVista(1);
			expect(component.vista()).toBe(1);
			component.cambiaVista(0);
			expect(component.vista()).toBe(0);
		});

		it('should navigate to class via navega', () => {
			component.idCurso = 2;
			component.navega({ idClase: 8 } as any);
			expect(mockRouter.navigate).toHaveBeenCalledWith(['repro/2/8']);
		});

		it('should close contextMenu on document click', () => {
			component.contextMenu.set({ x: 10, y: 20, time: 30 });
			component.onDocumentClick(new MouseEvent('click'));
			expect(component.contextMenu()).toBeNull();
		});

		it('should not call creaPregunta if contextMenu is null', () => {
			component.contextMenu.set(null);
			const mockChat = new MockChatComponent() as any;
			(component as any).chatComponent = signal(mockChat);

			component.clickPregunta();

			expect(mockChat.creaPregunta).not.toHaveBeenCalled();
		});

		it('should call creaPregunta and reset contextMenu when contextMenu is present', () => {
			component.contextMenu.set({ x: 50, y: 60, time: 120 });
			component.idClase = 3;
			const mockChat = new MockChatComponent() as any;
			(component as any).chatComponent = signal(mockChat);

			component.clickPregunta();

			expect(mockChat.creaPregunta).toHaveBeenCalledWith(3, 120);
			expect(component.contextMenu()).toBeNull();
		});
	});
});
