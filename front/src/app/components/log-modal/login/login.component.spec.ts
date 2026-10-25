import { ElementRef, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { LoginModalService } from '../../../services/login-modal.service';
import { LoginService } from '../../../services/login.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
	let component: LoginComponent;
	let fixture: ComponentFixture<LoginComponent>;
	let mockModalService: { hide: jasmine.Spy };
	let mockLoginService: {
		compruebaCorreo: jasmine.Spy;
		compruebaPassword: jasmine.Spy;
	};

	beforeEach(async () => {
		mockModalService = {
			hide: jasmine.createSpy('hide'),
		};

		mockLoginService = {
			compruebaCorreo: jasmine.createSpy('compruebaCorreo').and.returnValue(Promise.resolve(true)),
			compruebaPassword: jasmine.createSpy('compruebaPassword').and.returnValue(Promise.resolve(true)),
		};

		await TestBed.configureTestingModule({
			imports: [LoginComponent],
			providers: [
				{ provide: LoginModalService, useValue: mockModalService },
				{ provide: LoginService, useValue: mockLoginService },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(LoginComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		// Arrange & Act
		fixture.detectChanges();

		// Assert
		expect(component).toBeTruthy();
		expect(component.fase()).toBe(0);
		expect(component.correo).toBe('');
		expect(component.password).toBe('');
		expect(component.mensajeError()).toBe('');
	});

	describe('ngOnInit y manejo de teclado', () => {
		it('should subscribe to keyEvents and handle Enter key in fase 0', async () => {
			// Arrange
			const keyEventsSubject = new Subject<KeyboardEvent>();
			fixture.componentRef.setInput('keyEvents', keyEventsSubject);
			spyOn(component, 'compruebaCorreo');
			fixture.detectChanges();

			// Act
			keyEventsSubject.next(new KeyboardEvent('keydown', { key: 'Enter' }));
			await new Promise((resolve) => setTimeout(resolve, 10));

			// Assert
			expect(component.compruebaCorreo).toHaveBeenCalled();
		});

		it('should handle Enter key in fase 1', async () => {
			// Arrange
			const keyEventsSubject = new Subject<KeyboardEvent>();
			fixture.componentRef.setInput('keyEvents', keyEventsSubject);
			component.fase.set(1);
			spyOn(component, 'compruebaPassword');
			fixture.detectChanges();

			// Act
			keyEventsSubject.next(new KeyboardEvent('keydown', { key: 'Enter' }));
			await new Promise((resolve) => setTimeout(resolve, 10));

			// Assert
			expect(component.compruebaPassword).toHaveBeenCalled();
		});

		it('should close modal on Escape, Esc or Delete keys', async () => {
			// Arrange
			const keyEventsSubject = new Subject<KeyboardEvent>();
			fixture.componentRef.setInput('keyEvents', keyEventsSubject);
			spyOn(component, 'close');
			fixture.detectChanges();

			// Act & Assert
			keyEventsSubject.next(new KeyboardEvent('keydown', { key: 'Escape' }));
			expect(component.close).toHaveBeenCalledTimes(1);

			keyEventsSubject.next(new KeyboardEvent('keydown', { key: 'Esc' }));
			expect(component.close).toHaveBeenCalledTimes(2);

			keyEventsSubject.next(new KeyboardEvent('keydown', { key: 'Delete' }));
			expect(component.close).toHaveBeenCalledTimes(3);
		});

		it('should focus correoInput on initialization', async () => {
			// Arrange
			const inputEl = document.createElement('input');
			spyOn(inputEl, 'focus');
			(component as any).correoInput = signal(new ElementRef(inputEl));
			spyOn(window, 'requestAnimationFrame').and.callFake((cb: FrameRequestCallback) => {
				setTimeout(() => cb(0), 0);
				return 0;
			});

			// Act
			component.ngOnInit();
			await new Promise((resolve) => setTimeout(resolve, 10));

			// Assert
			expect(inputEl.focus).toHaveBeenCalled();
		});
	});

	describe('close and ngOnDestroy', () => {
		it('should unsubscribe and call modalService.hide', () => {
			// Arrange
			const keyEventsSubject = new Subject<KeyboardEvent>();
			fixture.componentRef.setInput('keyEvents', keyEventsSubject);
			fixture.detectChanges();

			// Act
			component.close();

			// Assert
			expect(mockModalService.hide).toHaveBeenCalled();
		});

		it('should safely unsubscribe on ngOnDestroy', () => {
			// Arrange
			const keyEventsSubject = new Subject<KeyboardEvent>();
			fixture.componentRef.setInput('keyEvents', keyEventsSubject);
			fixture.detectChanges();

			// Act & Assert
			expect(() => component.ngOnDestroy()).not.toThrow();
		});
	});

	describe('compruebaEmail', () => {
		it('should return true for valid emails', () => {
			// Arrange & Act & Assert
			expect(component.compruebaEmail('user@example.com')).toBeTrue();
			expect(component.compruebaEmail('name.surname@domain.co.uk')).toBeTrue();
		});

		it('should return false for invalid emails', () => {
			// Arrange & Act & Assert
			expect(component.compruebaEmail('plainaddress')).toBeFalse();
			expect(component.compruebaEmail('user@domain')).toBeFalse();
			expect(component.compruebaEmail('@domain.com')).toBeFalse();
			expect(component.compruebaEmail('')).toBeFalse();
		});
	});

	describe('compruebaCorreo', () => {
		it('should set error message if email is empty', async () => {
			// Arrange
			component.correo = '';

			// Act
			await component.compruebaCorreo();

			// Assert
			expect(component.mensajeError()).toBe('El correo no puede estar vacío!!!');
			expect(mockLoginService.compruebaCorreo).not.toHaveBeenCalled();
		});

		it('should set error message if email format is invalid', async () => {
			// Arrange
			component.correo = 'invalid-email-address';

			// Act
			await component.compruebaCorreo();

			// Assert
			expect(component.mensajeError()).toBe('El formato del correo no es válido!!!');
			expect(mockLoginService.compruebaCorreo).not.toHaveBeenCalled();
		});

		it('should transition to fase 1 and focus password input if email is verified', async () => {
			// Arrange
			component.correo = 'valid@example.com';
			mockLoginService.compruebaCorreo.and.returnValue(Promise.resolve(true));
			const inputEl = document.createElement('input');
			spyOn(inputEl, 'focus');
			(component as any).passwordInput = signal(new ElementRef(inputEl));
			spyOn(window, 'requestAnimationFrame').and.callFake((cb: FrameRequestCallback) => {
				setTimeout(() => cb(0), 0);
				return 0;
			});

			// Act
			await component.compruebaCorreo();
			await new Promise((resolve) => setTimeout(resolve, 10));

			// Assert
			expect(component.fase()).toBe(1);
			expect(component.mensajeError()).toBe('');
			expect(inputEl.focus).toHaveBeenCalled();
		});

		it('should set error message if email is not registered', async () => {
			// Arrange
			component.correo = 'unregistered@example.com';
			mockLoginService.compruebaCorreo.and.returnValue(Promise.resolve(false));

			// Act
			await component.compruebaCorreo();

			// Assert
			expect(component.fase()).toBe(0);
			expect(component.mensajeError()).toBe('El correo no existe!!!');
		});
	});

	describe('compruebaPassword', () => {
		it('should set error message if password is empty', async () => {
			// Arrange
			component.password = '';

			// Act
			await component.compruebaPassword();

			// Assert
			expect(component.mensajeError()).toBe('La contraseña no puede estar vacía!!!');
			expect(mockLoginService.compruebaPassword).not.toHaveBeenCalled();
		});

		it('should close modal when password is correct', async () => {
			// Arrange
			component.password = 'correct-password';
			mockLoginService.compruebaPassword.and.returnValue(Promise.resolve(true));
			spyOn(component, 'close');

			// Act
			await component.compruebaPassword();

			// Assert
			expect(component.close).toHaveBeenCalled();
			expect(component.mensajeError()).toBe('');
		});

		it('should set error message when password is incorrect', async () => {
			// Arrange
			component.password = 'wrong-password';
			mockLoginService.compruebaPassword.and.returnValue(Promise.resolve(false));

			// Act
			await component.compruebaPassword();

			// Assert
			expect(component.mensajeError()).toBe('La contraseña no es correcta!!!');
		});
	});
});
