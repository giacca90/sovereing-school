import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NavigationStart, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { Clase } from '../../../models/Clase';
import { Curso } from '../../../models/Curso';
import { CursosService } from '../../../services/cursos.service';
import { InitService } from '../../../services/init.service';
import { LoginService } from '../../../services/login.service';
import { StreamingService } from '../../../services/streaming.service';
import { EditorClaseComponent } from './editor-clase.component';

describe('EditorClaseComponent', () => {
	let component: EditorClaseComponent;
	let fixture: ComponentFixture<EditorClaseComponent>;
	let mockCursosService: jasmine.SpyObj<CursosService>;
	let mockStreamingService: jasmine.SpyObj<StreamingService>;
	let mockLoginService: any;
	let mockInitService: jasmine.SpyObj<InitService>;
	let mockRouter: any;
	let routerEventsSubject: Subject<any>;

	const createDummyClase = (): Clase => new Clase(1, 'Clase Test', 'Descripcion Test', 'http://video.mp4', 10, 'Contenido Test', 1, 1);

	const createDummyCurso = (): Curso => new Curso(1, 'Curso Test', [1], 'Corta', 'Larga', new Date(), [createDummyClase()], [], 'img.png', 50);

	beforeEach(async () => {
		routerEventsSubject = new Subject<any>();
		mockRouter = {
			events: routerEventsSubject.asObservable(),
		};

		mockCursosService = jasmine.createSpyObj('CursosService', ['updateCurso', 'getCurso']);
		mockCursosService.updateCurso.and.returnValue(of(createDummyCurso()));
		mockCursosService.getCurso.and.resolveTo(createDummyCurso());

		mockStreamingService = jasmine.createSpyObj('StreamingService', ['stopMediaStreaming', 'startWebOBS', 'getPresets', 'subeVideo', 'detenerWebOBS', 'emitirWebOBS', 'startOBS', 'emitirOBS', 'savePresets']);
		(mockStreamingService as any).emitiendo = false;
		(mockStreamingService as any).streamId = 'stream-123';
		(mockStreamingService as any).rtmpUrl = 'rtmp://test/live/stream-123';
		(mockStreamingService as any).status = '';
		mockStreamingService.getPresets.and.returnValue(of({ default: {} as any }));
		mockStreamingService.emitirWebOBS.and.resolveTo();

		mockLoginService = {
			usuario: { idUsuario: 1, fotoUsuario: [] },
		};

		mockInitService = jasmine.createSpyObj('InitService', ['carga']);

		await TestBed.configureTestingModule({
			imports: [EditorClaseComponent],
			providers: [
				{ provide: CursosService, useValue: mockCursosService },
				{ provide: StreamingService, useValue: mockStreamingService },
				{ provide: LoginService, useValue: mockLoginService },
				{ provide: InitService, useValue: mockInitService },
				{ provide: Router, useValue: mockRouter },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(EditorClaseComponent);
		component = fixture.componentInstance;
		component.clase.set(createDummyClase());
		component.curso.set(createDummyCurso());
	});

	it('should create the component', () => {
		// Arrange & Act
		fixture.detectChanges();

		// Assert
		expect(component).toBeTruthy();
	});

	describe('ngOnInit navigation guard', () => {
		it('should prompt user if changes were made upon NavigationStart', () => {
			// Arrange
			(component as any).claseOriginal = createDummyClase();
			component.clase.set(createDummyClase());
			component.clase.update((c) => ({ ...c!, nombreClase: 'Nombre Alterado' }));
			spyOn(window, 'confirm').and.returnValue(false);

			// Act
			component.ngOnInit();
			routerEventsSubject.next(new NavigationStart(1, '/other-route'));

			// Assert
			expect(window.confirm).toHaveBeenCalledWith('Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?');
		});
	});

	describe('ngAfterViewInit', () => {
		it('should scroll to top and set document body overflow hidden', () => {
			// Arrange
			spyOn(window, 'scrollTo');
			const renderer = (component as any).renderer;
			spyOn(renderer, 'setStyle');

			// Act
			component.ngAfterViewInit();

			// Assert
			expect(window.scrollTo).toHaveBeenCalledWith(0, 0);
			expect(renderer.setStyle).toHaveBeenCalledWith(document.body, 'overflow', 'hidden');
		});
	});

	describe('confirmacion', () => {
		beforeEach(() => {
			spyOn(window, 'alert');
		});

		it('should alert and return false when nombreClase is null or empty', () => {
			// Arrange
			component.clase.update((c) => ({ ...c!, nombreClase: '' }));

			// Act
			const result = component.confirmacion();

			// Assert
			expect(window.alert).toHaveBeenCalledWith('Debes poner un nombre para la clase');
			expect(result).toBeFalse();
		});

		it('should alert and return false when descripcionClase is null or empty', () => {
			// Arrange
			component.clase.update((c) => ({ ...c!, nombreClase: 'Valido', descripcionClase: '' }));

			// Act
			const result = component.confirmacion();

			// Assert
			expect(window.alert).toHaveBeenCalledWith('Debes poner una descripción para la clase');
			expect(result).toBeFalse();
		});

		it('should alert and return false when contenidoClase is null or empty', () => {
			// Arrange
			component.clase.update((c) => ({ ...c!, nombreClase: 'Valido', descripcionClase: 'Valida', contenidoClase: '' }));

			// Act
			const result = component.confirmacion();

			// Assert
			expect(window.alert).toHaveBeenCalledWith('Debes poner contenido para la clase');
			expect(result).toBeFalse();
		});

		it('should return true when all fields are valid', () => {
			// Arrange
			component.clase.update((c) => ({ ...c!, nombreClase: 'Valido', descripcionClase: 'Valida', contenidoClase: 'Valido' }));

			// Act
			const result = component.confirmacion();

			// Assert
			expect(result).toBeTrue();
		});
	});

	describe('validarVideo', () => {
		it('should alert and return false for new static video class without uploaded video', () => {
			// Arrange
			spyOn(window, 'alert');
			component.clase.update((c) => ({ ...c!, idClase: 0, tipoClase: 0 }));
			component.readyComponent = false;

			// Act
			const result = component.validarVideo();

			// Assert
			expect(window.alert).toHaveBeenCalledWith('Debes primero subir un video');
			expect(result).toBeFalse();
		});

		it('should return true if video is already ready or idClase > 0', () => {
			// Arrange
			component.clase.update((c) => ({ ...c!, idClase: 1, tipoClase: 0 }));

			// Act
			const result = component.validarVideo();

			// Assert
			expect(result).toBeTrue();
		});
	});

	describe('guardarCambiosClase and procesarClasePorTipo', () => {
		it('should abort if confirmacion fails', async () => {
			// Arrange
			spyOn(component, 'confirmacion').and.returnValue(false);
			spyOn(component, 'procesarClasePorTipo');

			// Act
			await component.guardarCambiosClase();

			// Assert
			expect(component.procesarClasePorTipo).not.toHaveBeenCalled();
		});

		it('should abort if validarVideo fails', async () => {
			// Arrange
			spyOn(component, 'confirmacion').and.returnValue(true);
			spyOn(component, 'validarVideo').and.returnValue(false);
			spyOn(component, 'procesarClasePorTipo');

			// Act
			await component.guardarCambiosClase();

			// Assert
			expect(component.procesarClasePorTipo).not.toHaveBeenCalled();
		});

		it('should prepare new class when idClase is 0 and process', async () => {
			// Arrange
			component.clase.update((c) => ({ ...c!, idClase: 0, tipoClase: 0, cursoClase: 0 }));
			spyOn(component, 'confirmacion').and.returnValue(true);
			spyOn(component, 'validarVideo').and.returnValue(true);
			spyOn(component, 'close');

			// Act
			await component.guardarCambiosClase();

			// Assert
			expect(component.close).toHaveBeenCalled();
		});

		it('should prepare new class when clasesCurso is undefined', async () => {
			component.clase.update((c) => ({ ...c!, idClase: 0, tipoClase: 0, cursoClase: 0 }));
			component.curso.update((cu) => ({ ...cu!, clasesCurso: undefined as any }));
			spyOn(component, 'confirmacion').and.returnValue(true);
			spyOn(component, 'validarVideo').and.returnValue(true);
			spyOn(component, 'close');

			await component.guardarCambiosClase();

			expect(component.curso()?.clasesCurso).toBeDefined();
		});

		it('should handle actualizarClaseExistente when clasesCurso is undefined', () => {
			component.curso.update((cu) => ({ ...cu!, clasesCurso: undefined as any }));
			component.clase.update((c) => ({ ...c!, idClase: 1 }));
			expect(() => component.actualizarClaseExistente()).not.toThrow();
		});

		it('should update existing class when idClase > 0 for static video class', async () => {
			// Arrange
			component.clase.update((c) => ({ ...c!, idClase: 1, tipoClase: 0, cursoClase: 1 }));
			spyOn(component, 'confirmacion').and.returnValue(true);
			spyOn(component, 'validarVideo').and.returnValue(true);
			spyOn(component, 'close');

			// Act
			await component.guardarCambiosClase();

			// Assert
			expect(mockCursosService.updateCurso).toHaveBeenCalled();
			expect(component.close).toHaveBeenCalled();
		});

		it('should process non-zero type class when readyService is false', async () => {
			// Arrange
			component.clase.update((c) => ({ ...c!, tipoClase: 1 }));
			component.readyService = false;
			spyOn(component, 'close');

			// Act
			await component.procesarClasePorTipo();

			// Assert
			expect(mockCursosService.getCurso).toHaveBeenCalledWith(component.curso()?.idCurso!, true);
			expect(component.close).toHaveBeenCalled();
		});
	});

	describe('close', () => {
		it('should emit claseGuardada with true', () => {
			// Arrange
			spyOn(component.claseGuardada, 'emit');

			// Act
			component.close();

			// Assert
			expect(component.claseGuardada.emit).toHaveBeenCalledWith(true);
		});
	});

	describe('eliminaClase', () => {
		it('should delete class and call updateCurso on confirmation', () => {
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(of(createDummyCurso()));
			component.eliminaClase(component.clase()!);
			expect(mockCursosService.updateCurso).toHaveBeenCalled();
		});

		it('should handle falsy success in eliminaClase', () => {
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(of(null as any));
			spyOn(console, 'error');
			component.eliminaClase(component.clase()!);
			expect(console.error).toHaveBeenCalledWith('Falló la actualización del curso en editor-clase');
		});

		it('should handle error in eliminaClase', () => {
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(throwError(() => new Error('Error')));
			spyOn(console, 'error');
			component.eliminaClase(component.clase()!);
			expect(console.error).toHaveBeenCalled();
		});
	});

	describe('actualizarClaseExistente and procesarClasePorTipo edge cases', () => {
		it('should do nothing in actualizarClaseExistente if idClase not found', () => {
			component.clase.update((c) => ({ ...c!, idClase: 999 }));
			expect(() => component.actualizarClaseExistente()).not.toThrow();
		});

		it('should handle falsy success in procesarClasePorTipo', async () => {
			component.clase.update((c) => ({ ...c!, idClase: 1, tipoClase: 0, cursoClase: 1 }));
			mockCursosService.updateCurso.and.returnValue(of(null as any));
			spyOn(component, 'close');
			spyOn(console, 'error');

			await component.procesarClasePorTipo();
			expect(console.error).toHaveBeenCalledWith('Falló la actualización del curso en editor-clase');
		});

		it('should handle error in procesarClasePorTipo', async () => {
			component.clase.update((c) => ({ ...c!, idClase: 1, tipoClase: 0, cursoClase: 1 }));
			mockCursosService.updateCurso.and.returnValue(throwError(() => new Error('API Error')));
			spyOn(component, 'close');
			spyOn(console, 'error');

			await component.procesarClasePorTipo();
			expect(console.error).toHaveBeenCalled();
		});
	});

	describe('cambiaTipoClase', () => {
		it('should return early if clase is null', () => {
			component.clase.set(null);
			expect(() => component.cambiaTipoClase(0)).not.toThrow();
		});

		it('should handle type 0, 1, 2', () => {
			component.clase.set(createDummyClase());
			component.cambiaTipoClase(0);
			expect(component.clase()?.tipoClase).toBe(0);

			component.cambiaTipoClase(1);
			expect(component.clase()?.tipoClase).toBe(1);

			component.cambiaTipoClase(2);
			expect(component.clase()?.tipoClase).toBe(2);
		});
	});

	describe('eliminaClase', () => {
		it('should remove class and update course when confirmed', () => {
			// Arrange
			spyOn(window, 'confirm').and.returnValue(true);
			const dummyClase = createDummyClase();

			// Act
			component.eliminaClase(dummyClase);

			// Assert
			expect(mockCursosService.updateCurso).toHaveBeenCalled();
			expect(mockInitService.carga).toHaveBeenCalled();
		});

		it('should do nothing if confirm is cancelled', () => {
			// Arrange
			spyOn(window, 'confirm').and.returnValue(false);

			// Act
			component.eliminaClase(createDummyClase());

			// Assert
			expect(mockCursosService.updateCurso).not.toHaveBeenCalled();
		});

		it('should handle falsy success and error in eliminaClase', () => {
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(of(null as any));
			spyOn(console, 'error');
			component.eliminaClase(createDummyClase());
			expect(console.error).toHaveBeenCalled();

			mockCursosService.updateCurso.and.returnValue(throwError(() => new Error('Delete error')));
			component.eliminaClase(createDummyClase());
			expect(console.error).toHaveBeenCalled();
		});
	});

	describe('cambiaTipoClase advanced', () => {
		it('should handle type 2 getPresets error and success with invalid JSON / valid JSON', () => {
			component.savedPresets = null;
			mockStreamingService.getPresets.and.returnValue(of('invalid-json' as any));
			spyOn(console, 'error');
			component.cambiaTipoClase(2);
			expect(component.clase()?.tipoClase).toBe(2);

			component.savedPresets = null;
			mockStreamingService.getPresets.and.returnValue(throwError(() => new Error('Preset error')));
			component.cambiaTipoClase(2);
			expect(component.clase()?.tipoClase).toBe(2);
		});

		it('should use cached savedPresets if not null', () => {
			component.savedPresets = new Map<string, any>([['test', 1]]);
			component.cambiaTipoClase(2);
			expect(component.clase()?.tipoClase).toBe(2);
		});
	});

	describe('subeVideo and preparaWebcam', () => {
		it('should upload video successfully', () => {
			const file = new File([''], 'video.mp4');
			mockStreamingService.subeVideo.and.returnValue(of('http://video-url.com/vid.mp4'));
			component.subeVideo(file);
			expect(component.clase()?.direccionClase).toBe('http://video-url.com/vid.mp4');
		});

		it('should prepare webcam with course image', async () => {
			component.curso.update((c) => ({ ...c!, imagenCurso: 'http://img.com/image.png' }));
			spyOn(window, 'fetch').and.returnValue(Promise.resolve(new Response(new Blob(['img']))));
			await component.preparaWebcam();
			expect(window.fetch).toHaveBeenCalledWith('http://img.com/image.png', { credentials: 'include' });
		});
	});

	describe('cambiaTipoClase', () => {
		it('should set tipoClase to 0 for static video', () => {
			// Arrange & Act
			component.cambiaTipoClase(0);

			// Assert
			expect(mockStreamingService.stopMediaStreaming).toHaveBeenCalled();
			expect(component.clase()?.tipoClase).toBe(0);
		});

		it('should set tipoClase to 1 for OBS', () => {
			// Arrange & Act
			component.cambiaTipoClase(1);

			// Assert
			expect(component.clase()?.tipoClase).toBe(1);
		});

		it('should start WebOBS and fetch presets when switching to type 2', () => {
			// Arrange
			component.savedPresets = null;

			// Act
			component.cambiaTipoClase(2);

			// Assert
			expect(mockStreamingService.startWebOBS).toHaveBeenCalled();
			expect(mockStreamingService.getPresets).toHaveBeenCalled();
			expect(component.clase()?.tipoClase).toBe(2);
		});
	});

	describe('subeVideo', () => {
		it('should upload video and update clase.direccionClase', () => {
			// Arrange
			const mockFile = new File(['data'], 'test.mp4', { type: 'video/mp4' });
			mockStreamingService.subeVideo.and.returnValue(of('http://server.com/video.mp4'));

			// Act
			component.subeVideo(mockFile);

			// Assert
			expect(component.clase()?.direccionClase).toBe('http://server.com/video.mp4');
			expect(component.isUploadingVideo).toBeFalse();
			expect(component.readyComponent).toBeTrue();
		});
	});

	describe('emiteWebOBS', () => {
		it('should alert if mediaStream is null and not streaming', () => {
			// Arrange
			spyOn(window, 'alert');
			(mockStreamingService as any).emitiendo = false;

			// Act
			component.emiteWebOBS(null);

			// Assert
			expect(window.alert).toHaveBeenCalledWith('Debes conectarte primero con la webcam');
			expect(component.readyComponent).toBeFalse();
		});

		it('should stop WebOBS if mediaStream is null and currently streaming', () => {
			// Arrange
			(mockStreamingService as any).emitiendo = true;

			// Act
			component.emiteWebOBS(null);

			// Assert
			expect(mockStreamingService.detenerWebOBS).toHaveBeenCalled();
			expect(component.readyComponent).toBeFalse();
		});

		it('should update course and emit WebOBS when valid stream provided', () => {
			// Arrange
			spyOn(component, 'confirmacion').and.returnValue(true);
			const mockStream = {} as MediaStream;

			// Act
			component.emiteWebOBS(mockStream);

			// Assert
			expect(mockCursosService.updateCurso).toHaveBeenCalled();
			expect(mockStreamingService.emitirWebOBS).toHaveBeenCalledWith(mockStream);
		});
	});

	describe('obsEvent and emiteOBS', () => {
		it('should handle startOBS', () => {
			// Arrange & Act
			component.obsEvent({ type: 'startOBS', message: '' });

			// Assert
			expect(mockStreamingService.startOBS).toHaveBeenCalled();
		});

		it('should handle stopOBS', () => {
			// Arrange & Act
			component.obsEvent({ type: 'stopOBS', message: '' });

			// Assert
			expect(mockStreamingService.stopMediaStreaming).toHaveBeenCalled();
			expect(component.readyComponent).toBeFalse();
		});

		it('should handle status event', () => {
			// Arrange & Act
			component.obsEvent({ type: 'status', message: 'connected' });

			// Assert
			expect(mockStreamingService.status).toBe('connected');
		});

		it('should handle emiteOBS event', () => {
			// Arrange
			spyOn(component, 'emiteOBS');

			// Act
			component.obsEvent({ type: 'emiteOBS', message: 'rtmp://url' });

			// Assert
			expect(component.emiteOBS).toHaveBeenCalledWith('rtmp://url');
		});

		it('should warn on unknown event type', () => {
			// Arrange
			spyOn(console, 'warn');

			// Act
			component.obsEvent({ type: 'unknownAction', message: '' });

			// Assert
			expect(console.warn).toHaveBeenCalled();
		});

		it('should log error when streamUrl is null in emiteOBS', async () => {
			// Arrange
			spyOn(console, 'error');

			// Act
			await component.emiteOBS(null);

			// Assert
			expect(console.error).toHaveBeenCalledWith('No se pudo obtener la URL del servidor');
		});

		it('should update course and emit OBS when valid streamUrl provided', async () => {
			// Arrange
			spyOn(component, 'confirmacion').and.returnValue(true);

			// Act
			await component.emiteOBS('rtmp://server/live/stream-123');

			// Assert
			expect(mockCursosService.updateCurso).toHaveBeenCalled();
			expect(mockStreamingService.emitirOBS).toHaveBeenCalled();
		});
	});

	describe('savePresets', () => {
		it('should forward presets to streamingService', () => {
			// Arrange
			const presets = new Map();

			// Act
			component.savePresets(presets);

			// Assert
			expect(mockStreamingService.savePresets).toHaveBeenCalledWith(presets);
		});
	});

	describe('ngOnDestroy', () => {
		it('should clean up subscriptions and set body overflow to auto', () => {
			// Arrange
			const renderer = (component as any).renderer;
			spyOn(renderer, 'setStyle');

			// Act
			component.ngOnDestroy();

			// Assert
			expect(renderer.setStyle).toHaveBeenCalledWith(document.body, 'overflow', 'auto');
		});
	});
});
