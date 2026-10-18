import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { LoginModalService } from '../../../services/login-modal.service';
import { LoginService } from '../../../services/login.service';
import { RegisterService } from '../../../services/register.service';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
	let component: RegisterComponent;
	let fixture: ComponentFixture<RegisterComponent>;
	let loginModalServiceSpy: jasmine.SpyObj<LoginModalService>;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;
	let registerServiceSpy: jasmine.SpyObj<RegisterService>;

	beforeEach(async () => {
		const modalSpy = jasmine.createSpyObj('LoginModalService', ['show', 'hide']);
		const logSpy = jasmine.createSpyObj('LoginService', ['compruebaCorreo']);
		const regSpy = jasmine.createSpyObj('RegisterService', ['registrarNuevoUsuario']);

		await TestBed.configureTestingModule({
			imports: [RegisterComponent, FormsModule],
			providers: [
				{ provide: LoginModalService, useValue: modalSpy },
				{ provide: LoginService, useValue: logSpy },
				{ provide: RegisterService, useValue: regSpy },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(RegisterComponent);
		component = fixture.componentInstance;
		loginModalServiceSpy = TestBed.inject(LoginModalService) as jasmine.SpyObj<LoginModalService>;
		loginServiceSpy = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
		registerServiceSpy = TestBed.inject(RegisterService) as jasmine.SpyObj<RegisterService>;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	describe('ngOnInit & KeyEvents', () => {
		it('should subscribe to keyEvents and handle Enter in fase 0', async () => {
			const keyEvents$ = new Subject<KeyboardEvent>();
			component.keyEvents = keyEvents$;
			component.fase = 0;
			spyOn(component, 'compruebaCorreo').and.returnValue(Promise.resolve());

			component.ngOnInit();

			keyEvents$.next(new KeyboardEvent('keydown', { key: 'Enter' }));
			expect(component.compruebaCorreo).toHaveBeenCalled();
		});

		it('should subscribe to keyEvents and handle Enter in fase 1', async () => {
			const keyEvents$ = new Subject<KeyboardEvent>();
			component.keyEvents = keyEvents$;
			component.fase = 1;
			spyOn(component, 'compruebaPassword');

			component.ngOnInit();

			keyEvents$.next(new KeyboardEvent('keydown', { key: 'Enter' }));
			expect(component.compruebaPassword).toHaveBeenCalled();
		});

		it('should subscribe to keyEvents and handle Enter in fase 2 (close)', async () => {
			const keyEvents$ = new Subject<KeyboardEvent>();
			component.keyEvents = keyEvents$;
			component.fase = 2;
			spyOn(component, 'close');

			component.ngOnInit();

			keyEvents$.next(new KeyboardEvent('keydown', { key: 'Enter' }));
			expect(component.close).toHaveBeenCalled();
		});

		it('should handle Escape, Esc and Delete keys to close', () => {
			const keyEvents$ = new Subject<KeyboardEvent>();
			component.keyEvents = keyEvents$;
			spyOn(component, 'close');

			component.ngOnInit();

			keyEvents$.next(new KeyboardEvent('keydown', { key: 'Escape' }));
			keyEvents$.next(new KeyboardEvent('keydown', { key: 'Esc' }));
			keyEvents$.next(new KeyboardEvent('keydown', { key: 'Delete' }));
			expect(component.close).toHaveBeenCalledTimes(3);
		});
	});

	describe('ngOnDestroy & close', () => {
		it('should unsubscribe and reset on ngOnDestroy', () => {
			component.nuevoUsuario.nombreUsuario = 'Test';
			component.ngOnDestroy();
			expect(component.nuevoUsuario.nombreUsuario).toBeNull();
		});

		it('should call modalService.hide() on close', () => {
			component.close();
			expect(loginModalServiceSpy.hide).toHaveBeenCalled();
		});
	});

	describe('compruebaEmail', () => {
		it('should validate email format correctly', () => {
			expect(component.compruebaEmail('test@example.com')).toBeTrue();
			expect(component.compruebaEmail('invalid-email')).toBeFalse();
			expect(component.compruebaEmail('test@.com')).toBeFalse();
		});
	});

	describe('compruebaCorreo', () => {
		[
			{ nombre: '', correo: 'test@example.com', err: 'El nombre no puede estar vacío.' },
			{ nombre: 'User', correo: '', err: 'No has puesto el correo!!!' },
			{ nombre: 'User', correo: 'bademail', err: 'El correo electrónico no tiene un formato válido!!!' },
		].forEach(({ nombre, correo, err }) => {
			it(`should validate correo [nombre: "${nombre}", correo: "${correo}"] -> "${err}"`, async () => {
				component.nuevoUsuario.nombreUsuario = nombre;
				component.nuevoUsuario.correoElectronico = correo;
				await component.compruebaCorreo();
				expect(component.mensajeError).toBe(err);
			});
		});

		it('should set error if correoElectronico is already registered', async () => {
			component.nuevoUsuario.nombreUsuario = 'User';
			component.nuevoUsuario.correoElectronico = 'test@example.com';
			loginServiceSpy.compruebaCorreo.and.returnValue(Promise.resolve(true));

			await component.compruebaCorreo();

			expect(component.mensajeError).toBe('Este correo ya está registrado!!!');
			expect(component.fase).toBe(0);
		});

		it('should advance to fase 1 if correoElectronico is available', async () => {
			component.nuevoUsuario.nombreUsuario = 'User';
			component.nuevoUsuario.correoElectronico = 'test@example.com';
			loginServiceSpy.compruebaCorreo.and.returnValue(Promise.resolve(false));

			await component.compruebaCorreo();

			expect(component.mensajeError).toBe('');
			expect(component.fase).toBe(1);
		});
	});

	describe('compruebaPassword', () => {
		[
			{ pass: '', conf: '123456', err: 'La contraseña no puede estar vacía' },
			{ pass: '123456', conf: '', err: 'La contraseña no puede estar vacía' },
			{ pass: '123456', conf: '654321', err: 'Las contraseñas no coinciden' },
		].forEach(({ pass, conf, err }) => {
			it(`should validate password [pass: "${pass}", conf: "${conf}"] -> "${err}"`, () => {
				component.nuevoUsuario.password = pass;
				component.passwordConfirm = conf;
				component.compruebaPassword();
				expect(component.mensajeError).toBe(err);
			});
		});

		it('should set fase to 2 if registration succeeds', async () => {
			component.nuevoUsuario.password = '123456';
			component.passwordConfirm = '123456';
			registerServiceSpy.registrarNuevoUsuario.and.returnValue(Promise.resolve(true));

			component.compruebaPassword();
			await fixture.whenStable();

			expect(component.fase).toBe(2);
		});

		it('should not change fase if registration returns false', async () => {
			component.nuevoUsuario.password = '123456';
			component.passwordConfirm = '123456';
			component.fase = 1;
			registerServiceSpy.registrarNuevoUsuario.and.returnValue(Promise.resolve(false));

			component.compruebaPassword();
			await fixture.whenStable();

			expect(component.fase).toBe(1);
		});
	});
});
