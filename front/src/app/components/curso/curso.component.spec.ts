import { PLATFORM_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { Curso } from '../../models/Curso';
import { Plan } from '../../models/Plan';
import { CursosService } from '../../services/cursos.service';
import { LoginService } from '../../services/login.service';
import { UsuariosService } from '../../services/usuarios.service';
import { CursoComponent } from './curso.component';

describe('CursoComponent', () => {
	let component: CursoComponent;
	let fixture: ComponentFixture<CursoComponent>;
	let mockCursoService: jasmine.SpyObj<CursosService>;
	let mockUsuarioService: jasmine.SpyObj<UsuariosService>;
	let mockLoginService: any;
	let mockRouter: jasmine.SpyObj<Router>;
	let paramsSubject: BehaviorSubject<any>;

	const dummyCurso: Curso = {
		idCurso: 1,
		nombreCurso: 'Curso Angular',
		descripcionCorta: 'Aprende Angular',
		descripcionLarga: 'Aprende Angular paso a paso',
		profesoresCurso: [10],
		precioCurso: 99,
		imagenCurso: 'portada.png',
		clasesCurso: [],
		planesCurso: [1, 2],
	};

	beforeEach(async () => {
		paramsSubject = new BehaviorSubject<any>({ idCurso: '1' });
		mockCursoService = jasmine.createSpyObj('CursosService', ['getCurso']);
		mockCursoService.getCurso.and.resolveTo(dummyCurso);

		mockUsuarioService = jasmine.createSpyObj('UsuariosService', ['cursoComprado', 'getNombreProfe']);
		mockUsuarioService.getNombreProfe.and.callFake((id) => (id === 10 ? 'Profesor John' : 'Profesor Jane'));

		mockLoginService = {
			usuario: null,
		};
		mockRouter = jasmine.createSpyObj('Router', ['navigate']);

		await TestBed.configureTestingModule({
			imports: [CursoComponent],
			providers: [
				{
					provide: ActivatedRoute,
					useValue: { params: paramsSubject.asObservable() },
				},
				{ provide: CursosService, useValue: mockCursoService },
				{ provide: UsuariosService, useValue: mockUsuarioService },
				{ provide: LoginService, useValue: mockLoginService },
				{ provide: Router, useValue: mockRouter },
				{ provide: PLATFORM_ID, useValue: 'browser' },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(CursoComponent);
		component = fixture.componentInstance;
	});

	it('should create and load initial course from route params', async () => {
		// Arrange & Act
		await fixture.whenStable();

		// Assert
		expect(component).toBeTruthy();
		expect(mockCursoService.getCurso).toHaveBeenCalledWith(1);
	});

	describe('compruebaPlan', () => {
		const planUser: Plan = {
			idPlan: 2,
			nombrePlan: 'Plan Premium',
			precioPlan: 20,
		} as unknown as Plan;

		it('should return null when planUsuario is null or undefined', () => {
			// Arrange & Act & Assert
			expect(component.compruebaPlan(undefined)).toBeNull();
			expect(component.compruebaPlan(null as any)).toBeNull();
		});

		it('should return user plan when user plan id matches one of course planes', () => {
			// Arrange
			component.curso.set(dummyCurso);
			(mockLoginService as any).usuario = {
				idUsuario: 1,
				nombreUsuario: 'test',
				planUsuario: planUser,
			};

			// Act
			const result = component.compruebaPlan(planUser);

			// Assert
			expect(result).toEqual(planUser);
		});

		it('should return null when user plan is found but user has no planUsuario set', () => {
			// Arrange
			component.curso.set(dummyCurso);
			(mockLoginService as any).usuario = {
				idUsuario: 1,
				nombreUsuario: 'test',
				planUsuario: null,
			};

			// Act
			const result = component.compruebaPlan(planUser);

			// Assert
			expect(result).toBeNull();
		});

		it('should return null when user plan id is not included in course planes', () => {
			// Arrange
			component.curso.set(dummyCurso);
			const otherPlan: Plan = { ...planUser, idPlan: 99 };

			// Act
			const result = component.compruebaPlan(otherPlan);

			// Assert
			expect(result).toBeNull();
		});
	});

	describe('onKeydownHandler', () => {
		it('should close modal when escape key is pressed and modalCourse is present', () => {
			// Arrange
			component.modalCourse.set(dummyCurso);
			spyOn(component, 'closeModal');

			// Act
			component.onKeydownHandler(new KeyboardEvent('keydown', { key: 'Escape' }));

			// Assert
			expect(component.closeModal).toHaveBeenCalled();
		});

		it('should do nothing on escape key if modalCourse is null', () => {
			// Arrange
			component.modalCourse.set(null);
			spyOn(component, 'closeModal');

			// Act
			component.onKeydownHandler(new KeyboardEvent('keydown', { key: 'Escape' }));

			// Assert
			expect(component.closeModal).not.toHaveBeenCalled();
		});
	});

	describe('compraCurso and closeModal', () => {
		it('should set modalCourse and add overflow-hidden in browser mode', () => {
			// Arrange
			const renderer = (component as any).renderer;
			spyOn(renderer, 'addClass');

			// Act
			component.compraCurso(dummyCurso);

			// Assert
			expect(component.modalCourse()).toEqual(dummyCurso);
			expect(renderer.addClass).toHaveBeenCalledWith((component as any).document.body, 'overflow-hidden');
		});

		it('should remove overflow-hidden and reset modalCourse in closeModal', () => {
			// Arrange
			component.modalCourse.set(dummyCurso);
			const renderer = (component as any).renderer;
			spyOn(renderer, 'removeClass');

			// Act
			component.closeModal();

			// Assert
			expect(component.modalCourse()).toBeNull();
			expect(renderer.removeClass).toHaveBeenCalledWith((component as any).document.body, 'overflow-hidden');
		});
	});

	describe('cursoComprado', () => {
		beforeEach(() => {
			spyOn(window, 'alert');
		});

		it('should alert success and closeModal when service returns true', () => {
			// Arrange
			mockUsuarioService.cursoComprado.and.returnValue(of(true));
			spyOn(component, 'closeModal');

			// Act
			component.cursoComprado(dummyCurso);

			// Assert
			expect(window.alert).toHaveBeenCalledWith('¡Curso comprado con éxito!');
			expect(component.closeModal).toHaveBeenCalled();
		});

		it('should alert error when service returns false', () => {
			// Arrange
			mockUsuarioService.cursoComprado.and.returnValue(of(false));

			// Act
			component.cursoComprado(dummyCurso);

			// Assert
			expect(window.alert).toHaveBeenCalledWith('Error al comprar el curso');
		});

		it('should alert error and log when service fails', () => {
			// Arrange
			mockUsuarioService.cursoComprado.and.returnValue(throwError(() => new Error('Error de red')));
			spyOn(console, 'error');

			// Act
			component.cursoComprado(dummyCurso);

			// Assert
			expect(console.error).toHaveBeenCalled();
			expect(window.alert).toHaveBeenCalledWith('Error al comprar el curso');
		});
	});

	describe('hasCurso', () => {
		it('should return false if usuario or cursosUsuario is null/undefined', () => {
			// Arrange
			(mockLoginService as any).usuario = null;

			// Act & Assert
			expect(component.hasCurso(dummyCurso)).toBeFalse();

			(mockLoginService as any).usuario = { idUsuario: 1 };
			expect(component.hasCurso(dummyCurso)).toBeFalse();
		});

		it('should return true if user already owns course', () => {
			// Arrange
			(mockLoginService as any).usuario = {
				idUsuario: 1,
				cursosUsuario: [{ idCurso: 1 }],
			};

			// Act & Assert
			expect(component.hasCurso(dummyCurso)).toBeTrue();
		});

		it('should return false if user has courses but not this one', () => {
			// Arrange
			(mockLoginService as any).usuario = {
				idUsuario: 1,
				cursosUsuario: [{ idCurso: 999 }],
			};

			// Act & Assert
			expect(component.hasCurso(dummyCurso)).toBeFalse();
		});
	});

	describe('getCurso', () => {
		it('should format single professor name when only one professor', async () => {
			// Arrange
			mockCursoService.getCurso.and.resolveTo(dummyCurso);

			// Act
			component.getCurso();
			await fixture.whenStable();

			// Assert
			expect(component.curso()).toEqual(dummyCurso);
			expect(component.nombresProfesores).toBe('Profesor John');
		});

		it('should join multiple professor names with " y "', async () => {
			// Arrange
			const cursoMulti: Curso = {
				...dummyCurso,
				profesoresCurso: [10, 20],
			};
			mockCursoService.getCurso.and.resolveTo(cursoMulti);

			// Act
			component.getCurso();
			await fixture.whenStable();

			// Assert
			expect(component.nombresProfesores).toBe('Profesor John y Profesor Jane');
		});

		it('should handle null course response', async () => {
			// Arrange
			mockCursoService.getCurso.and.resolveTo(null as any);

			// Act
			component.getCurso();
			await fixture.whenStable();

			// Assert
			expect(component.curso()).toBeNull();
		});
	});

	describe('addCursoAUsuario', () => {
		it('should call cursoComprado and navigate to repro route', () => {
			// Arrange
			spyOn(component, 'cursoComprado');

			// Act
			component.addCursoAUsuario(dummyCurso);

			// Assert
			expect(component.cursoComprado).toHaveBeenCalledWith(dummyCurso);
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/repro/1/0']);
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
