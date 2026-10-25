import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Curso } from '../../models/Curso';
import { CursoChat } from '../../models/CursoChat';
import { Usuario } from '../../models/Usuario';
import { ChatService } from '../../services/chat.service';
import { CursosService } from '../../services/cursos.service';
import { InitService } from '../../services/init.service';
import { UsuariosService } from '../../services/usuarios.service';
import { AdministracionComponent } from './administracion.component';

describe('AdministracionComponent', () => {
	let component: AdministracionComponent;
	let fixture: ComponentFixture<AdministracionComponent>;
	let mockUsuariosService: jasmine.SpyObj<UsuariosService>;
	let mockCursosService: jasmine.SpyObj<CursosService>;
	let mockChatService: jasmine.SpyObj<ChatService>;
	let mockInitService: jasmine.SpyObj<InitService>;

	const dummyUsuarios: Usuario[] = [
		{
			idUsuario: 1,
			nombreUsuario: 'Alice',
			rollUsuario: 'ADMIN',
			fotoUsuario: ['http://pic.jpg'],
			presentacion: 'Test',
		} as unknown as Usuario,
		{
			idUsuario: 2,
			nombreUsuario: 'Bob',
			rollUsuario: 'PROF',
			fotoUsuario: ['#00FF00'],
			presentacion: 'Test',
		} as unknown as Usuario,
	];

	const dummyCursos: Curso[] = [
		{
			idCurso: 101,
			nombreCurso: 'Angular Avanzado',
			descripcionCorta: 'Curso de Angular',
			profesoresCurso: [2],
			precioCurso: 50,
			imagenCurso: 'portada.png',
			clasesCurso: [],
		} as unknown as Curso,
		{
			idCurso: 102,
			nombreCurso: 'TypeScript',
			descripcionCorta: 'Curso de TS',
			profesoresCurso: [2],
			precioCurso: 40,
			imagenCurso: 'ts.png',
			clasesCurso: [],
		} as unknown as Curso,
	];

	const dummyChats: CursoChat[] = [
		{
			idCurso: 101,
			nombreCurso: 'Angular Avanzado',
			mensajes: [],
			clases: [],
			fotoCurso: 'pic.png',
		} as unknown as CursoChat,
	];

	beforeEach(async () => {
		mockUsuariosService = jasmine.createSpyObj('UsuariosService', ['getAllUsuarios', 'eliminaUsuario'], {
			profes: [dummyUsuarios[1]],
		});
		mockCursosService = jasmine.createSpyObj('CursosService', ['getAllCursos', 'deleteCurso', 'getCurso', 'updateCurso']);
		mockChatService = jasmine.createSpyObj('ChatService', ['getAllChats', 'deleteChat']);
		mockInitService = jasmine.createSpyObj('InitService', ['init', 'carga']);

		await TestBed.configureTestingModule({
			imports: [AdministracionComponent],
			providers: [
				{ provide: UsuariosService, useValue: mockUsuariosService },
				{ provide: CursosService, useValue: mockCursosService },
				{ provide: ChatService, useValue: mockChatService },
				{ provide: InitService, useValue: mockInitService },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(AdministracionComponent);
		component = fixture.componentInstance;
	});

	it('should create', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
		expect(component.tipo()).toBe(0);
	});

	describe('cargaUsuarios', () => {
		it('should load users when list is empty and data is returned', () => {
			// Arrange
			mockUsuariosService.getAllUsuarios.and.returnValue(of(dummyUsuarios));

			// Act
			component.cargaUsuarios();

			// Assert
			expect(mockUsuariosService.getAllUsuarios).toHaveBeenCalled();
			expect(component.usuarios).toEqual(dummyUsuarios);
			expect(component.usuariosSel()).toEqual(dummyUsuarios);
			expect(component.tipo()).toBe(1);
		});

		it('should not overwrite users if null is returned', () => {
			// Arrange
			mockUsuariosService.getAllUsuarios.and.returnValue(of(null));

			// Act
			component.cargaUsuarios();

			// Assert
			expect(component.usuarios).toEqual([]);
			expect(component.tipo()).toBe(1);
		});

		it('should not call service if users are already loaded', () => {
			// Arrange
			component.usuarios = dummyUsuarios;

			// Act
			component.cargaUsuarios();

			// Assert
			expect(mockUsuariosService.getAllUsuarios).not.toHaveBeenCalled();
			expect(component.tipo()).toBe(1);
		});
	});

	describe('cargaCursos', () => {
		it('should load courses when list is empty and data is returned', () => {
			// Arrange
			mockCursosService.getAllCursos.and.returnValue(of(dummyCursos as any));

			// Act
			component.cargaCursos();

			// Assert
			expect(mockCursosService.getAllCursos).toHaveBeenCalled();
			expect(component.cursos).toEqual(dummyCursos);
			expect(component.cursosSel()).toEqual(dummyCursos);
			expect(component.tipo()).toBe(2);
		});

		it('should not overwrite courses if null is returned', () => {
			// Arrange
			mockCursosService.getAllCursos.and.returnValue(of(null as any));

			// Act
			component.cargaCursos();

			// Assert
			expect(component.cursos).toEqual([]);
			expect(component.tipo()).toBe(2);
		});

		it('should not call service if courses already loaded', () => {
			// Arrange
			component.cursos = dummyCursos;

			// Act
			component.cargaCursos();

			// Assert
			expect(mockCursosService.getAllCursos).not.toHaveBeenCalled();
			expect(component.tipo()).toBe(2);
		});
	});

	describe('cargaChats', () => {
		it('should load chats when list is empty and data is returned', () => {
			// Arrange
			mockChatService.getAllChats.and.returnValue(of(dummyChats));

			// Act
			component.cargaChats();

			// Assert
			expect(mockChatService.getAllChats).toHaveBeenCalled();
			expect(component.chats).toEqual(dummyChats);
			expect(component.chatsSel()).toEqual(dummyChats);
			expect(component.tipo()).toBe(3);
		});

		it('should not overwrite chats if null is returned', () => {
			// Arrange
			mockChatService.getAllChats.and.returnValue(of(null));

			// Act
			component.cargaChats();

			// Assert
			expect(component.chats).toEqual([]);
			expect(component.tipo()).toBe(3);
		});

		it('should not call service if chats already loaded', () => {
			// Arrange
			component.chats = dummyChats;

			// Act
			component.cargaChats();

			// Assert
			expect(mockChatService.getAllChats).not.toHaveBeenCalled();
			expect(component.tipo()).toBe(3);
		});
	});

	describe('buscaUsuarios', () => {
		beforeEach(() => {
			component.usuarios = dummyUsuarios;
		});

		it('should reset usuariosSel if search value is empty', () => {
			// Arrange
			const event = { target: { value: '' } } as unknown as Event;

			// Act
			component.buscaUsuarios(event);

			// Assert
			expect(component.usuariosSel()).toEqual(dummyUsuarios);
		});

		it('should filter usuarios by name', () => {
			// Arrange
			const event = { target: { value: 'alice' } } as unknown as Event;

			// Act
			component.buscaUsuarios(event);

			// Assert
			expect(component.usuariosSel().length).toBe(1);
			expect(component.usuariosSel()[0].nombreUsuario).toBe('Alice');
		});

		it('should filter usuarios by role', () => {
			// Arrange
			const event = { target: { value: 'prof' } } as unknown as Event;

			// Act
			component.buscaUsuarios(event);

			// Assert
			expect(component.usuariosSel().length).toBe(1);
			expect(component.usuariosSel()[0].nombreUsuario).toBe('Bob');
		});

		it('should filter usuarios by id', () => {
			// Arrange
			const event = { target: { value: '2' } } as unknown as Event;

			// Act
			component.buscaUsuarios(event);

			// Assert
			expect(component.usuariosSel().length).toBe(1);
			expect(component.usuariosSel()[0].idUsuario).toBe(2);
		});
	});

	describe('buscaCursos', () => {
		beforeEach(() => {
			component.cursos = dummyCursos;
		});

		it('should reset cursosSel if search value is empty', () => {
			// Arrange
			const event = { target: { value: '' } } as unknown as Event;

			// Act
			component.buscaCursos(event);

			// Assert
			expect(component.cursosSel()).toEqual(dummyCursos);
		});

		it('should filter cursos by name', () => {
			// Arrange
			const event = { target: { value: 'angular' } } as unknown as Event;

			// Act
			component.buscaCursos(event);

			// Assert
			expect(component.cursosSel().length).toBe(1);
			expect(component.cursosSel()[0].idCurso).toBe(101);
		});

		it('should filter cursos by professor id/string', () => {
			// Arrange
			const event = { target: { value: '2' } } as unknown as Event;

			// Act
			component.buscaCursos(event);

			// Assert
			expect(component.cursosSel().length).toBe(2);
		});

		it('should filter cursos by course id', () => {
			// Arrange
			const event = { target: { value: '102' } } as unknown as Event;

			// Act
			component.buscaCursos(event);

			// Assert
			expect(component.cursosSel().length).toBe(1);
			expect(component.cursosSel()[0].idCurso).toBe(102);
		});
	});

	describe('buscaChats', () => {
		beforeEach(() => {
			component.chats = dummyChats;
		});

		it('should reset chatsSel if search value is empty', () => {
			// Arrange
			const event = { target: { value: '' } } as unknown as Event;

			// Act
			component.buscaChats(event);

			// Assert
			expect(component.chatsSel()).toEqual(dummyChats);
		});

		it('should filter chats by name', () => {
			// Arrange
			const event = { target: { value: 'angular' } } as unknown as Event;

			// Act
			component.buscaChats(event);

			// Assert
			expect(component.chatsSel().length).toBe(1);
		});

		it('should filter chats by id', () => {
			// Arrange
			const event = { target: { value: '101' } } as unknown as Event;

			// Act
			component.buscaChats(event);

			// Assert
			expect(component.chatsSel().length).toBe(1);
		});
	});

	describe('eliminaUsuario', () => {
		it('should do nothing if confirmation is cancelled', () => {
			// Arrange
			spyOn(window, 'confirm').and.returnValue(false);

			// Act
			component.eliminaUsuario(dummyUsuarios[0]);

			// Assert
			expect(mockUsuariosService.eliminaUsuario).not.toHaveBeenCalled();
		});

		it('should call service and reload users when confirmed and deletion is successful', () => {
			// Arrange
			spyOn(window, 'confirm').and.returnValue(true);
			mockUsuariosService.eliminaUsuario.and.returnValue(of(true));
			mockUsuariosService.getAllUsuarios.and.returnValue(of([dummyUsuarios[1]]));

			// Act
			component.eliminaUsuario(dummyUsuarios[0]);

			// Assert
			expect(mockUsuariosService.eliminaUsuario).toHaveBeenCalledWith(dummyUsuarios[0]);
			expect(mockUsuariosService.getAllUsuarios).toHaveBeenCalled();
			expect(component.usuarios).toEqual([dummyUsuarios[1]]);
		});

		it('should not reload users if service returns false', () => {
			// Arrange
			spyOn(window, 'confirm').and.returnValue(true);
			mockUsuariosService.eliminaUsuario.and.returnValue(of(false));

			// Act
			component.eliminaUsuario(dummyUsuarios[0]);

			// Assert
			expect(mockUsuariosService.getAllUsuarios).not.toHaveBeenCalled();
		});
	});

	describe('eliminaCurso', () => {
		it('should do nothing if confirmation is cancelled', () => {
			// Arrange
			spyOn(window, 'confirm').and.returnValue(false);

			// Act
			component.eliminaCurso(dummyCursos[0]);

			// Assert
			expect(mockCursosService.deleteCurso).not.toHaveBeenCalled();
		});

		it('should call deleteCurso and reload courses when confirmed and deletion is successful', () => {
			// Arrange
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.deleteCurso.and.returnValue(of(true));
			mockCursosService.getAllCursos.and.returnValue(of([dummyCursos[1]] as any));

			// Act
			component.eliminaCurso(dummyCursos[0]);

			// Assert
			expect(mockCursosService.deleteCurso).toHaveBeenCalledWith(dummyCursos[0]);
			expect(mockCursosService.getAllCursos).toHaveBeenCalled();
			expect(component.cursos).toEqual([dummyCursos[1]]);
		});
	});

	describe('eliminaClase', () => {
		it('should do nothing if confirmation is cancelled', () => {
			spyOn(window, 'confirm').and.returnValue(false);
			component.eliminaClase({ idClase: 1, cursoClase: 101 } as any);
			expect(mockCursosService.getCurso).not.toHaveBeenCalled();
		});

		it('should handle curso not found', async () => {
			spyOn(window, 'confirm').and.returnValue(true);
			mockCursosService.getCurso.and.resolveTo(null);
			spyOn(console, 'error');

			await component.eliminaClase({ idClase: 1, cursoClase: 101 } as any);
			expect(console.error).toHaveBeenCalledWith('Curso no encontrado');
		});

		it('should remove class from course and update successfully', async () => {
			spyOn(window, 'confirm').and.returnValue(true);
			const cursoMock = { idCurso: 101, clasesCurso: [{ idClase: 1 }, { idClase: 2 }] };
			mockCursosService.getCurso.and.resolveTo(cursoMock as any);
			mockCursosService.updateCurso.and.returnValue(of(cursoMock as any));
			mockInitService.carga.and.resolveTo();

			await component.eliminaClase({ idClase: 1, cursoClase: 101 } as any);
			expect(mockCursosService.updateCurso).toHaveBeenCalled();
		});

		it('should handle error on updateCurso', async () => {
			spyOn(window, 'confirm').and.returnValue(true);
			const cursoMock = { idCurso: 101, clasesCurso: [{ idClase: 1 }] };
			mockCursosService.getCurso.and.resolveTo(cursoMock as any);
			mockCursosService.updateCurso.and.returnValue(throwError(() => new Error('Update error')));
			spyOn(console, 'error');

			await component.eliminaClase({ idClase: 1, cursoClase: 101 } as any);
			expect(console.error).toHaveBeenCalled();
		});
	});

	describe('eliminaChat', () => {
		it('should do nothing if confirmation is cancelled', () => {
			// Arrange
			spyOn(window, 'confirm').and.returnValue(false);

			// Act
			component.eliminaChat(dummyChats[0]);

			// Assert
			expect(mockChatService.deleteChat).not.toHaveBeenCalled();
		});

		it('should call deleteChat and reload chats when confirmed and deletion is successful', () => {
			// Arrange
			spyOn(window, 'confirm').and.returnValue(true);
			mockChatService.deleteChat.and.returnValue(of(true));
			mockChatService.getAllChats.and.returnValue(of([]));

			// Act
			component.eliminaChat(dummyChats[0]);

			// Assert
			expect(mockChatService.deleteChat).toHaveBeenCalledWith(dummyChats[0]);
			expect(mockChatService.getAllChats).toHaveBeenCalled();
			expect(component.chats).toEqual([]);
		});
	});

	describe('toggle visibility methods', () => {
		describe('mostrarUsuario', () => {
			it('should toggle usuarioVisible between id and null', () => {
				// Arrange
				component.usuarioVisible.set(null);

				// Act 1
				component.mostrarUsuario(1);
				// Assert 1
				expect(component.usuarioVisible()).toBe(1 as any);

				// Act 2
				component.mostrarUsuario(1);
				// Assert 2
				expect(component.usuarioVisible()).toBeNull();
			});
		});

		describe('mostrarCurso', () => {
			it('should toggle cursoVisible between id and null', () => {
				// Arrange
				component.cursoVisible.set(null);

				// Act 1
				component.mostrarCurso(101);
				// Assert 1
				expect(component.cursoVisible()).toBe(101 as any);

				// Act 2
				component.mostrarCurso(101);
				// Assert 2
				expect(component.cursoVisible()).toBeNull();
			});
		});

		describe('mostrarChat', () => {
			it('should toggle chatVisible between id and null', () => {
				// Arrange
				component.chatVisible.set(null);

				// Act 1
				component.mostrarChat(101);
				// Assert 1
				expect(component.chatVisible()).toBe(101 as any);

				// Act 2
				component.mostrarChat(101);
				// Assert 2
				expect(component.chatVisible()).toBeNull();
			});
		});

		describe('mostrarClaseChat', () => {
			it('should toggle claseChatVisible between id and null', () => {
				// Arrange
				component.claseChatVisible.set(null);

				// Act 1
				component.mostrarClaseChat(5);
				// Assert 1
				expect(component.claseChatVisible()).toBe(5 as any);

				// Act 2
				component.mostrarClaseChat(5);
				// Assert 2
				expect(component.claseChatVisible()).toBeNull();
			});
		});
	});

	describe('getProfessorPhoto and getProfessorName', () => {
		describe('getProfessorPhoto', () => {
			it('should return photo url when professor exists', () => {
				// Arrange & Act
				const photo = component.getProfessorPhoto(2);

				// Assert
				expect(photo).toBe('#00FF00');
			});

			it('should return undefined when professor does not exist', () => {
				// Arrange & Act
				const photo = component.getProfessorPhoto(999);

				// Assert
				expect(photo).toBeUndefined();
			});
		});

		describe('getProfessorName', () => {
			it('should return name when professor exists', () => {
				// Arrange & Act
				const name = component.getProfessorName(2);

				// Assert
				expect(name).toBe('Bob');
			});

			it('should return undefined when professor does not exist', () => {
				// Arrange & Act
				const name = component.getProfessorName(999);

				// Assert
				expect(name).toBeUndefined();
			});
		});
	});
});
