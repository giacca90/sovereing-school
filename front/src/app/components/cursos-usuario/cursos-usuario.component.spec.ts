import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CursosService } from '../../services/cursos.service';
import { LoginService } from '../../services/login.service';
import { CursosUsuarioComponent } from './cursos-usuario.component';

describe('CursosUsuarioComponent', () => {
	let component: CursosUsuarioComponent;
	let fixture: ComponentFixture<CursosUsuarioComponent>;
	let mockLoginService: any;
	let mockCursosService: jasmine.SpyObj<CursosService>;
	let mockRouter: jasmine.SpyObj<Router>;

	beforeEach(async () => {
		mockLoginService = {
			usuario: null,
		};
		mockCursosService = jasmine.createSpyObj('CursosService', ['getCursosProfe']);
		mockCursosService.getCursosProfe.and.returnValue([]);
		mockRouter = jasmine.createSpyObj('Router', ['navigate']);

		await TestBed.configureTestingModule({
			imports: [CursosUsuarioComponent],
			providers: [
				{ provide: LoginService, useValue: mockLoginService },
				{ provide: CursosService, useValue: mockCursosService },
				{ provide: Router, useValue: mockRouter },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(CursosUsuarioComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
	});

	describe('template rendering by user role and courses', () => {
		it('should render only title when user is null', () => {
			// Arrange
			(mockLoginService as any).usuario = null;

			// Act
			fixture.detectChanges();
			const compiled = fixture.nativeElement as HTMLElement;

			// Assert
			expect(compiled.querySelector('h2')?.textContent).toContain('Tus Cursos');
			expect(compiled.querySelector('h3')).toBeNull();
		});

		it('should display "No tienes cursos!!!" when role is USER and cursosUsuario is empty', () => {
			// Arrange
			(mockLoginService as any).usuario = {
				idUsuario: 1,
				rollUsuario: 'USER',
				cursosUsuario: [],
			};

			// Act
			fixture.detectChanges();
			const compiled = fixture.nativeElement as HTMLElement;

			// Assert
			expect(compiled.querySelector('h3')?.textContent).toContain('No tienes cursos!!!');
		});

		it('should render course items and navigate when role is USER with courses', () => {
			// Arrange
			(mockLoginService as any).usuario = {
				idUsuario: 1,
				rollUsuario: 'USER',
				cursosUsuario: [
					{
						idCurso: 10,
						nombreCurso: 'Curso de Prueba',
						descripcionCorta: 'Desc corta',
						imagenCurso: 'img.jpg',
					},
				],
			};

			// Act
			fixture.detectChanges();
			const compiled = fixture.nativeElement as HTMLElement;
			const button = compiled.querySelector('button');
			button?.click();

			// Assert
			expect(compiled.textContent).toContain('Curso de Prueba');
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/repro/10/0']);
		});

		it('should display "No tienes cursos!!!" and "Crea un nuevo curso" when role is PROF and has no courses', () => {
			// Arrange
			const userProf = {
				idUsuario: 2,
				rollUsuario: 'PROF',
			};
			(mockLoginService as any).usuario = userProf;
			mockCursosService.getCursosProfe.and.returnValue([]);

			// Act
			fixture.detectChanges();
			const compiled = fixture.nativeElement as HTMLElement;
			const createButton = compiled.querySelector('button');
			createButton?.click();

			// Assert
			expect(compiled.querySelector('h3')?.textContent).toContain('No tienes cursos!!!');
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/editorCurso/0']);
		});

		it('should render teacher courses and allow editing when role is PROF', () => {
			// Arrange
			const userProf = {
				idUsuario: 2,
				rollUsuario: 'PROF',
			};
			(mockLoginService as any).usuario = userProf;
			mockCursosService.getCursosProfe.and.returnValue([
				{
					idCurso: 25,
					nombreCurso: 'Curso Profesor',
					descripcionCorta: 'Desc',
					imagenCurso: 'portada.png',
				} as any,
			]);

			// Act
			fixture.detectChanges();
			const compiled = fixture.nativeElement as HTMLElement;
			const buttons = compiled.querySelectorAll('button');
			// First button is "Editar este curso"
			buttons[0]?.click();

			// Assert
			expect(compiled.textContent).toContain('Curso Profesor');
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/editorCurso/25']);
		});
	});
});
