import { signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Curso } from '../../models/Curso';
import { CursosService } from '../../services/cursos.service';
import { UsuariosService } from '../../services/usuarios.service';
import { SearchComponent } from './search.component';

describe('SearchComponent', () => {
	let component: SearchComponent;
	let fixture: ComponentFixture<SearchComponent>;
	let mockCursosService: { cursos: WritableSignal<Curso[]> };
	let mockRouter: { navigate: jasmine.Spy };
	let mockUsuariosService: { profes: any[] };

	const dummyCursos: Partial<Curso>[] = [
		{
			idCurso: 1,
			nombreCurso: 'Curso de Angular Avanzado',
			descripcionCorta: 'Aprende arquitectura y componentes reactivos',
			profesoresCurso: [10],
		},
		{
			idCurso: 2,
			nombreCurso: 'Programación en Python & IA',
			descripcionCorta: 'Modelos de machine learning con Python',
			profesoresCurso: undefined,
		},
		{
			idCurso: 3,
			nombreCurso: 'Diseño UX/UI Gráfico',
			descripcionCorta: 'Interfaces accesibles e inclusivas',
			profesoresCurso: [999],
		},
	];

	beforeEach(async () => {
		mockCursosService = {
			cursos: signal([...dummyCursos] as Curso[]),
		};

		mockRouter = {
			navigate: jasmine.createSpy('navigate'),
		};

		mockUsuariosService = {
			profes: [{ idUsuario: 10, nombreUsuario: 'Profesor Carlos' }],
		};

		await TestBed.configureTestingModule({
			imports: [SearchComponent],
			providers: [
				{ provide: CursosService, useValue: mockCursosService },
				{ provide: Router, useValue: mockRouter },
				{ provide: UsuariosService, useValue: mockUsuariosService },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(SearchComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create the component', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
		expect(component.result()).toBeFalse();
		expect(component.porNombre()).toEqual([]);
		expect(component.porDescripcion()).toEqual([]);
		expect(component.busqueda).toBe('');
	});

	describe('normalize', () => {
		it('should convert text to lowercase and remove accents / diacritics', () => {
			// Arrange
			const input = 'Programación Árbol Álbum Canción Único';

			// Act
			const result = component.normalize(input);

			// Assert
			expect(result).toBe('programacion arbol album cancion unico');
		});

		it('should handle strings without accents or already lowercased', () => {
			// Arrange
			const input = 'angular testing';

			// Act
			const result = component.normalize(input);

			// Assert
			expect(result).toBe('angular testing');
		});
	});

	describe('busca', () => {
		it('should reset search results when input is empty', () => {
			// Arrange
			component.result.set(true);
			component.porNombre.set([dummyCursos[0] as Curso]);
			component.porDescripcion.set([dummyCursos[0] as Curso]);
			const mockEvent = {
				target: { value: '' },
			} as unknown as Event;

			// Act
			component.busca(mockEvent);

			// Assert
			expect(component.busqueda).toBe('');
			expect(component.result()).toBeFalse();
			expect(component.porNombre()).toEqual([]);
			expect(component.porDescripcion()).toEqual([]);
		});

		it('should filter courses by nombreCurso matching search string ignoring case and accents', () => {
			// Arrange
			const mockEvent = {
				target: { value: 'áNguLAr' },
			} as unknown as Event;

			// Act
			component.busca(mockEvent);

			// Assert
			expect(component.result()).toBeTrue();
			expect(component.busqueda).toBe('áNguLAr');
			expect(component.porNombre().length).toBe(1);
			expect(component.porNombre()[0].idCurso).toBe(1);
		});

		it('should filter courses by descripcionCorta matching search string', () => {
			// Arrange
			const mockEvent = {
				target: { value: 'machine learning' },
			} as unknown as Event;

			// Act
			component.busca(mockEvent);

			// Assert
			expect(component.result()).toBeTrue();
			expect(component.porDescripcion().length).toBe(1);
			expect(component.porDescripcion()[0].idCurso).toBe(2);
		});

		it('should return empty results when no courses match search query', () => {
			// Arrange
			const mockEvent = {
				target: { value: 'NoExisteNingunCursoConEsteTermino' },
			} as unknown as Event;

			// Act
			component.busca(mockEvent);

			// Assert
			expect(component.result()).toBeTrue();
			expect(component.porNombre().length).toBe(0);
			expect(component.porDescripcion().length).toBe(0);
		});

		it('should filter courses by profesor name matching search string', () => {
			// Arrange
			const mockEvent = {
				target: { value: 'Carlos' },
			} as unknown as Event;

			// Act
			component.busca(mockEvent);

			// Assert
			expect(component.result()).toBeTrue();
			expect(component.porProfesor().length).toBe(1);
			expect(component.porProfesor()[0].idCurso).toBe(1);
		});
	});

	describe('navega', () => {
		it('should set result to false and navigate to course route', () => {
			// Arrange
			component.result.set(true);

			// Act
			component.navega(1);

			// Assert
			expect(component.result()).toBeFalse();
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/curso/1']);
		});
	});

	describe('navega', () => {
		it('should set result to false and navigate to course url inside ngZone', () => {
			// Arrange
			component.result.set(true);
			const idCurso = 42;

			// Act
			component.navega(idCurso);

			// Assert
			expect(component.result()).toBeFalse();
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/curso/42']);
		});
	});
});
