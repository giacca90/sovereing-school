import { PLATFORM_ID } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, flush, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { Clase } from '../../models/Clase';
import { Curso } from '../../models/Curso';
import { CursosService } from '../../services/cursos.service';
import { InitService } from '../../services/init.service';
import { LoginService } from '../../services/login.service';
import { StreamingService } from '../../services/streaming.service';
import { EditorCursoComponent } from './editor-curso.component';

describe('EditorCursoComponent', () => {
	let component: EditorCursoComponent;
	let fixture: ComponentFixture<EditorCursoComponent>;
	let mockRouter: jasmine.SpyObj<Router>;
	let mockCursosService: jasmine.SpyObj<CursosService>;
	let mockLoginService: any;
	let mockStreamingService: any;
	let mockInitService: jasmine.SpyObj<InitService>;
	let paramsSubject: BehaviorSubject<any>;

	const dummyClase1: Clase = new Clase(1, 'Clase 1', 'Desc 1', 'video.mp4', 10, 'notas', 1, 1);
	const dummyClase2: Clase = new Clase(2, 'Clase 2', 'Desc 2', 'video2.mp4', 15, 'notas 2', 2, 1);

	const dummyCurso: Curso = new Curso(1, 'Curso TypeScript', [10], 'desc corta', 'desc larga', new Date(), [dummyClase1, dummyClase2], [1], 'portada.jpg', 20);

	beforeEach(async () => {
		paramsSubject = new BehaviorSubject<any>({ idCurso: '1' });
		mockRouter = jasmine.createSpyObj('Router', ['navigate']);
		mockRouter.navigate.and.resolveTo(true);

		mockCursosService = jasmine.createSpyObj('CursosService', ['getCurso', 'updateCurso', 'deleteCurso', 'addImagenCurso'], {
			cursos: [],
		});
		mockCursosService.getCurso.and.resolveTo(dummyCurso);

		mockLoginService = {
			usuario: { idUsuario: 10, nombreUsuario: 'Profesor' },
		};

		mockStreamingService = {
			emitiendo: false,
		};

		mockInitService = jasmine.createSpyObj('InitService', ['carga']);

		await TestBed.configureTestingModule({
			imports: [EditorCursoComponent],
			providers: [
				{ provide: ActivatedRoute, useValue: { params: paramsSubject.asObservable() } },
				{ provide: Router, useValue: mockRouter },
				{ provide: CursosService, useValue: mockCursosService },
				{ provide: LoginService, useValue: mockLoginService },
				{ provide: StreamingService, useValue: mockStreamingService },
				{ provide: InitService, useValue: mockInitService },
				{ provide: PLATFORM_ID, useValue: 'browser' },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(EditorCursoComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
	});

	describe('canDeactivate', () => {
		it('should prompt confirm when streaming is active', () => {
			// Arrange
			(mockStreamingService as any).emitiendo = true;
			spyOn(window, 'confirm').and.returnValue(false);

			// Act
			const result = component.canDeactivate();

			// Assert
			expect(window.confirm).toHaveBeenCalledWith('Estás emitiendo. ¿Seguro que quieres salir?');
			expect(result).toBeFalse();
		});

		it('should prompt confirm when editado is true and not streaming', () => {
			// Arrange
			(mockStreamingService as any).emitiendo = false;
			component.editado = true;
			spyOn(window, 'confirm').and.returnValue(true);

			// Act
			const result = component.canDeactivate();

			// Assert
			expect(window.confirm).toHaveBeenCalledWith('Tienes cambios sin guardar. ¿Seguro que quieres salir?');
			expect(result).toBeTrue();
		});

		it('should return true without prompt when neither streaming nor edited', () => {
			// Arrange
			(mockStreamingService as any).emitiendo = false;
			component.editado = false;
			spyOn(window, 'confirm');

			// Act
			const result = component.canDeactivate();

			// Assert
			expect(window.confirm).not.toHaveBeenCalled();
			expect(result).toBeTrue();
		});
	});

	describe('unloadNotification', () => {
		it('should prevent default when streaming or edited', () => {
			// Arrange
			(mockStreamingService as any).emitiendo = true;
			const event = jasmine.createSpyObj('BeforeUnloadEvent', ['preventDefault']);

			// Act
			component.unloadNotification(event);

			// Assert
			expect(event.preventDefault).toHaveBeenCalled();
		});

		it('should not prevent default when not streaming and not edited', () => {
			// Arrange
			(mockStreamingService as any).emitiendo = false;
			component.editado = false;
			const event = jasmine.createSpyObj('BeforeUnloadEvent', ['preventDefault']);

			// Act
			component.unloadNotification(event);

			// Assert
			expect(event.preventDefault).not.toHaveBeenCalled();
		});
	});

	describe('ngOnInit', () => {
		it('should create a new Curso instance when idCurso is 0 and user is logged in', () => {
			// Arrange
			component.idCurso = 0;

			// Act
			component.ngOnInit();

			// Assert
			expect(component.curso).toBeDefined();
			expect(component.curso.idCurso).toBe(0);
			expect(component.curso.profesoresCurso).toEqual([10]);
		});

		it('should navigate to root when idCurso is 0 and no user is logged in', () => {
			// Arrange
			component.idCurso = 0;
			(mockLoginService as any).usuario = null;
			spyOn(console, 'error');

			// Act
			component.ngOnInit();

			// Assert
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
			expect(console.error).toHaveBeenCalled();
		});

		it('should load existing course when idCurso > 0', async () => {
			// Arrange
			component.idCurso = 1;
			mockCursosService.getCurso.and.resolveTo(dummyCurso);

			// Act
			component.ngOnInit();
			await fixture.whenStable();

			// Assert
			expect(component.curso).toBeDefined();
			expect(component.curso.idCurso).toBe(1);
		});

		it('should navigate to root when course is not found', async () => {
			// Arrange
			component.idCurso = 999;
			mockCursosService.getCurso.and.resolveTo(null);
			spyOn(console, 'error');

			// Act
			component.ngOnInit();
			await fixture.whenStable();

			// Assert
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
		});

		it('should navigate to root when getting course throws error', fakeAsync(() => {
			// Arrange
			component.idCurso = 1;
			mockCursosService.getCurso.and.returnValue(Promise.reject(new Error('Network error')));
			spyOn(console, 'error');

			// Act
			component.ngOnInit();
			flush();

			// Assert
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
		}));
	});

	describe('drag and drop', () => {
		describe('onDragStart', () => {
			it('should setup drag image and set draggedElementId', fakeAsync(() => {
				// Arrange
				const mockTarget = document.createElement('div');
				mockTarget.className = 'test-class';
				const mockDataTransfer = {
					setData: jasmine.createSpy('setData'),
					setDragImage: jasmine.createSpy('setDragImage'),
				};
				const event = {
					target: mockTarget,
					dataTransfer: mockDataTransfer,
				} as unknown as DragEvent;

				// Act
				component.onDragStart(event, 1);

				// Assert
				expect(component.draggedElementId).toBe(1);
				expect(mockDataTransfer.setData).toHaveBeenCalledWith('text/plain', '1');
				tick(1);
			}));
		});

		describe('onDragOver', () => {
			it('should do nothing if draggedElementId is null or identical to target id', () => {
				// Arrange
				component.draggedElementId = null;
				const event = jasmine.createSpyObj('Event', ['preventDefault']);

				// Act
				component.onDragOver(event, 1);

				// Assert
				expect(event.preventDefault).toHaveBeenCalled();

				component.draggedElementId = 1;
				component.onDragOver(event, 1);
			});

			it('should swap class positions when dragging over another item', () => {
				// Arrange
				component.curso = structuredClone(dummyCurso);
				component.draggedElementId = 1;
				spyOn(component, 'compruebaCambios');
				const event = jasmine.createSpyObj('Event', ['preventDefault']);

				// Act
				component.onDragOver(event, 2);

				// Assert
				expect(component.compruebaCambios).toHaveBeenCalled();
				expect(component.curso!.clasesCurso![0].idClase).toBe(2);
			});
		});

		describe('onDrop and onDragEnd', () => {
			it('should reset draggedElementId on drop', () => {
				// Arrange
				component.draggedElementId = 5;
				const event = jasmine.createSpyObj('Event', ['preventDefault']);

				// Act
				component.onDrop(event);

				// Assert
				expect(component.draggedElementId).toBeNull();
			});

			it('should remove opacity-0 on drag end and reset draggedElementId', () => {
				// Arrange
				component.draggedElementId = 5;
				const div = document.createElement('div');
				div.classList.add('opacity-0');
				const event = { target: div } as unknown as DragEvent;

				// Act
				component.onDragEnd(event);

				// Assert
				expect(div.classList.contains('opacity-0')).toBeFalse();
				expect(component.draggedElementId).toBeNull();
			});
		});
	});

	describe('compruebaCambios', () => {
		it('should set editado to true when course differs from remote', async () => {
			// Arrange
			component.curso = { ...dummyCurso, nombreCurso: 'Modificado' };
			mockCursosService.getCurso.and.resolveTo(dummyCurso);

			// Act
			component.compruebaCambios();
			await fixture.whenStable();

			// Assert
			expect(component.editado).toBeTrue();
		});
	});

	describe('updateCurso', () => {
		it('should call updateCurso, update state and navigate on success', () => {
			// Arrange
			component.idCurso = 0;
			component.curso = dummyCurso;
			mockCursosService.updateCurso.and.returnValue(of(dummyCurso));

			// Act
			component.updateCurso();

			// Assert
			expect(mockInitService.carga).toHaveBeenCalled();
			expect(component.editado).toBeFalse();
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/cursosUsuario']);
		});

		it('should log error when update returns falsy or throws error', () => {
			// Arrange
			component.curso = dummyCurso;
			mockCursosService.updateCurso.and.returnValue(throwError(() => new Error('Error al actualizar')));
			spyOn(console, 'error');

			// Act
			component.updateCurso();

			// Assert
			expect(console.error).toHaveBeenCalled();
		});

		it('should log error when update returns falsy response', () => {
			// Arrange
			component.curso = dummyCurso;
			mockCursosService.updateCurso.and.returnValue(of(null as any));
			spyOn(console, 'error');

			// Act
			component.updateCurso();

			// Assert
			expect(console.error).toHaveBeenCalledWith('Falló la actualización del curso en editor-curso');
		});
	});

	describe('editarClase and nuevaClase', () => {
		it('should populate claseEditar when class is found in course', async () => {
			// Arrange
			mockCursosService.getCurso.and.resolveTo(dummyCurso);
			component.curso = structuredClone(dummyCurso);

			// Act
			await component.editarClase(dummyClase1);

			// Assert
			expect(component.claseEditar).toEqual({ ...dummyClase1 });
		});

		it('should log error when course or class is missing in editarClase', async () => {
			// Arrange
			spyOn(console, 'error');
			component.curso = {} as any;
			mockCursosService.getCurso.and.resolveTo(null);

			// Act
			await component.editarClase(dummyClase1);

			// Assert
			expect(console.error).toHaveBeenCalledWith('No se pudo obtener el curso');
		});

		it('should log error when class is not found in course classes', async () => {
			// Arrange
			const cursoMock = structuredClone(dummyCurso);
			cursoMock.clasesCurso = [];
			mockCursosService.getCurso.and.resolveTo(cursoMock);
			component.curso = structuredClone(dummyCurso);
			spyOn(console, 'error');

			// Act
			await component.editarClase(dummyClase1);

			// Assert
			expect(console.error).toHaveBeenCalledWith('No se pudo obtener la clase');
		});

		it('should create new class in nuevaClase', () => {
			// Arrange
			component.idCurso = 1;
			component.curso = structuredClone(dummyCurso);

			// Act
			component.nuevaClase();

			// Assert
			expect(component.claseEditar).toBeDefined();
			expect(component.claseEditar?.posicionClase).toBe(3);
		});
	});

	describe('cargaImagenCurso', () => {
		it('should do nothing if input has no files', () => {
			// Arrange
			const event = { target: { files: null } } as unknown as Event;

			// Act
			component.cargaImagenCurso(event);

			// Assert
			expect(mockCursosService.addImagenCurso).not.toHaveBeenCalled();
		});

		it('should upload image when files are present', () => {
			const file = new File(['dummy content'], 'test.png', { type: 'image/png' });
			const event = { target: { files: [file] } } as unknown as Event;
			component.curso = structuredClone(dummyCurso);
			mockCursosService.addImagenCurso.and.returnValue(of('new-image.png'));
			spyOn(component, 'compruebaCambios');

			class MockFileReader {
				onload: any = null;
				readAsDataURL(blob: Blob) {
					if (this.onload) {
						this.onload({ target: { result: 'data:image/png;base64,...' } });
					}
				}
			}
			spyOn(window as any, 'FileReader').and.returnValue(new MockFileReader() as any);

			component.cargaImagenCurso(event);

			expect(mockCursosService.addImagenCurso).toHaveBeenCalled();
			expect(component.curso.imagenCurso).toBe('new-image.png');
			expect(component.compruebaCambios).toHaveBeenCalled();
		});

		it('should log error when addImagenCurso fails', () => {
			const file = new File(['dummy content'], 'test.png', { type: 'image/png' });
			const event = { target: { files: [file] } } as unknown as Event;
			component.curso = structuredClone(dummyCurso);
			mockCursosService.addImagenCurso.and.returnValue(throwError(() => new Error('Upload error')));
			spyOn(console, 'error');

			class MockFileReader {
				onload: any = null;
				readAsDataURL(blob: Blob) {
					if (this.onload) {
						this.onload({ target: { result: 'data:image/png;base64,...' } });
					}
				}
			}
			spyOn(window as any, 'FileReader').and.returnValue(new MockFileReader() as any);

			component.cargaImagenCurso(event);

			expect(console.error).toHaveBeenCalledWith('Error en añadir la imagen al curso: Upload error');
		});
	});

	describe('eliminaClase', () => {
		it('should remove class and update course on confirmation', () => {
			component.curso = structuredClone(dummyCurso);
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(of(dummyCurso));

			component.eliminaClase(dummyClase1);

			expect(mockCursosService.updateCurso).toHaveBeenCalled();
			expect(mockInitService.carga).toHaveBeenCalled();
		});

		it('should log error when updateCurso returns falsy in eliminaClase', () => {
			component.curso = structuredClone(dummyCurso);
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(of(null as any));
			spyOn(console, 'error');

			component.eliminaClase(dummyClase1);

			expect(console.error).toHaveBeenCalledWith('Falló la actualización del curso en editor-clase');
		});

		it('should log error when updateCurso throws error in eliminaClase', () => {
			component.curso = structuredClone(dummyCurso);
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(throwError(() => new Error('Error')));
			spyOn(console, 'error');

			component.eliminaClase(dummyClase1);

			expect(console.error).toHaveBeenCalled();
		});
	});

	describe('claseGuardada', () => {
		it('should handle claseGuardada when idCurso is 0', async () => {
			component.idCurso = 0;
			spyOn(component, 'compruebaCambios');

			await component.claseGuardada(true);

			expect(component.compruebaCambios).toHaveBeenCalled();
			expect(component.claseEditar).toBeNull();
		});

		it('should reload course when idCurso is not 0', async () => {
			component.idCurso = 1;
			component.curso = structuredClone(dummyCurso);
			mockCursosService.getCurso.and.resolveTo(dummyCurso);
			spyOn(component, 'compruebaCambios');

			await component.claseGuardada(true);

			expect(component.compruebaCambios).toHaveBeenCalled();
			expect(component.claseEditar).toBeNull();
		});

		it('should log error when getCurso returns null in claseGuardada', async () => {
			component.idCurso = 1;
			component.curso = structuredClone(dummyCurso);
			mockCursosService.getCurso.and.resolveTo(null);
			spyOn(console, 'error');

			await component.claseGuardada(true);

			expect(console.error).toHaveBeenCalledWith('No se pudo obtener el curso');
		});

		it('should log error when getCurso throws in claseGuardada', async () => {
			component.idCurso = 1;
			component.curso = structuredClone(dummyCurso);
			mockCursosService.getCurso.and.returnValue(Promise.reject(new Error('Fetch error')));
			spyOn(console, 'error');

			await component.claseGuardada(true);
			await new Promise((resolve) => setTimeout(resolve, 20));

			expect(console.error).toHaveBeenCalled();
		});

		it('should do nothing if event is false in claseGuardada', async () => {
			await component.claseGuardada(false);
			expect(mockCursosService.getCurso).not.toHaveBeenCalled();
		});
	});

	describe('nuevaClase additional branch', () => {
		it('should not throw if clasesCurso is undefined in nuevaClase', () => {
			component.curso = { ...dummyCurso, clasesCurso: undefined };
			expect(() => component.nuevaClase()).not.toThrow();
		});
	});

	describe('deleteCurso', () => {
		it('should delete course and navigate when user confirms twice', () => {
			// Arrange
			component.curso = dummyCurso;
			spyOn(window, 'confirm').and.returnValues(true, true);
			mockCursosService.deleteCurso.and.returnValue(of(true));

			// Act
			component.deleteCurso();

			// Assert
			expect(mockCursosService.deleteCurso).toHaveBeenCalledWith(dummyCurso);
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/cursosUsuario']);
		});

		it('should abort if first or second confirm is false', () => {
			// Arrange
			component.curso = dummyCurso;
			spyOn(window, 'confirm').and.returnValue(false);

			// Act
			component.deleteCurso();

			// Assert
			expect(mockCursosService.deleteCurso).not.toHaveBeenCalled();
		});

		it('should handle deleteCurso returning false', () => {
			// Arrange
			component.curso = dummyCurso;
			spyOn(window, 'confirm').and.returnValues(true, true);
			mockCursosService.deleteCurso.and.returnValue(of(false));

			// Act
			component.deleteCurso();

			// Assert
			expect(mockCursosService.deleteCurso).toHaveBeenCalled();
		});

		it('should handle deleteCurso throwing error', () => {
			// Arrange
			component.curso = dummyCurso;
			spyOn(window, 'confirm').and.returnValues(true, true);
			mockCursosService.deleteCurso.and.returnValue(throwError(() => new Error('Delete error')));
			spyOn(console, 'error');

			// Act
			component.deleteCurso();

			// Assert
			expect(console.error).toHaveBeenCalled();
		});
	});

	describe('eliminaClase', () => {
		it('should remove class and update course when confirmed', () => {
			// Arrange
			component.curso = structuredClone(dummyCurso);
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(of(component.curso));

			// Act
			component.eliminaClase(dummyClase1);

			// Assert
			expect(component.curso!.clasesCurso!.length).toBe(1);
			expect(mockCursosService.updateCurso).toHaveBeenCalled();
			expect(mockInitService.carga).toHaveBeenCalled();
		});

		it('should not delete class when confirm is false', () => {
			// Arrange
			component.curso = structuredClone(dummyCurso);
			spyOn(window, 'confirm').and.returnValue(false);

			// Act
			component.eliminaClase(dummyClase1);

			// Assert
			expect(mockCursosService.updateCurso).not.toHaveBeenCalled();
		});
	});

	describe('claseGuardada', () => {
		it('should reset claseEditar and overflow when idCurso is 0 and event is true', async () => {
			// Arrange
			component.idCurso = 0;
			component.claseEditar = dummyClase1;
			spyOn(component, 'compruebaCambios');

			// Act
			await component.claseGuardada(true);

			// Assert
			expect(component.claseEditar).toBeNull();
			expect(component.compruebaCambios).toHaveBeenCalled();
		});

		it('should reload course when idCurso > 0 and event is true', async () => {
			// Arrange
			component.idCurso = 1;
			component.curso = dummyCurso;
			component.claseEditar = dummyClase1;
			mockCursosService.getCurso.and.resolveTo(dummyCurso);
			spyOn(component, 'compruebaCambios');

			// Act
			await component.claseGuardada(true);

			// Assert
			expect(component.claseEditar).toBeNull();
			expect(component.compruebaCambios).toHaveBeenCalled();
		});

		it('should do nothing when event is false', async () => {
			// Arrange
			component.claseEditar = dummyClase1;

			// Act
			await component.claseGuardada(false);

			// Assert
			expect(component.claseEditar).toEqual(dummyClase1);
		});

		it('should log error when course is not found on claseGuardada with idCurso > 0', async () => {
			// Arrange
			component.idCurso = 1;
			component.curso = dummyCurso;
			mockCursosService.getCurso.and.resolveTo(null);
			spyOn(console, 'error');

			// Act
			await component.claseGuardada(true);
			await fixture.whenStable();

			// Assert
			expect(console.error).toHaveBeenCalledWith('No se pudo obtener el curso');
		});

		it('should log error when getCurso throws error on claseGuardada', async () => {
			// Arrange
			component.idCurso = 1;
			component.curso = dummyCurso;
			mockCursosService.getCurso.and.returnValue(Promise.reject(new Error('Fetch error')));
			spyOn(console, 'error');

			// Act
			await component.claseGuardada(true);
			await fixture.whenStable();

			// Assert
			expect(console.error).toHaveBeenCalledWith('Error al obtener el curso:', jasmine.any(Error));
		});
	});

	describe('additional branch tests for editarClase, deleteCurso, eliminaClase, cargaImagenCurso', () => {
		it('should log error when class is missing in course during editarClase', async () => {
			// Arrange
			spyOn(console, 'error');
			mockCursosService.getCurso.and.resolveTo(dummyCurso);
			component.curso = structuredClone(dummyCurso);
			const invalidClase = new Clase(999, 'Invalid', '', '', 0, '', 1, 1);

			// Act
			await component.editarClase(invalidClase);

			// Assert
			expect(console.error).toHaveBeenCalledWith('No se pudo obtener la clase');
		});

		it('should log error when deleteCurso throws error', () => {
			// Arrange
			component.curso = dummyCurso;
			spyOn(window, 'confirm').and.returnValues(true, true);
			mockCursosService.deleteCurso.and.returnValue(throwError(() => new Error('Delete error')));
			spyOn(console, 'error');

			// Act
			component.deleteCurso();

			// Assert
			expect(console.error).toHaveBeenCalled();
		});

		it('should log error when updateCurso returns falsy on eliminaClase', () => {
			// Arrange
			component.curso = structuredClone(dummyCurso);
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(of(null as any));
			spyOn(console, 'error');

			// Act
			component.eliminaClase(dummyClase1);

			// Assert
			expect(console.error).toHaveBeenCalledWith('Falló la actualización del curso en editor-clase');
		});

		it('should log error when updateCurso throws error on eliminaClase', () => {
			// Arrange
			component.curso = structuredClone(dummyCurso);
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.updateCurso.and.returnValue(throwError(() => new Error('Update error')));
			spyOn(console, 'error');

			// Act
			component.eliminaClase(dummyClase1);

			// Assert
			expect(console.error).toHaveBeenCalled();
		});

		it('should read file when file is selected in cargaImagenCurso', () => {
			// Arrange
			const file = new File(['dummy'], 'test.png', { type: 'image/png' });
			const event = { target: { files: [file] } } as unknown as Event;
			const readerSpyObj = jasmine.createSpyObj('FileReader', ['readAsDataURL']);
			spyOn(window, 'FileReader').and.returnValue(readerSpyObj as any);

			// Act
			component.cargaImagenCurso(event);

			// Assert
			expect(readerSpyObj.readAsDataURL).toHaveBeenCalledWith(file);
		});
	});

	describe('ngOnDestroy', () => {
		it('should unsubscribe from subscription', () => {
			// Arrange
			const unsubSpy = spyOn((component as any).subscription, 'unsubscribe');

			// Act
			component.ngOnDestroy();

			// Assert
			expect(unsubSpy).toHaveBeenCalled();
		});
	});
});
