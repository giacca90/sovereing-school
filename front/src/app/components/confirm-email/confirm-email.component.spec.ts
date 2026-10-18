import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Auth } from '../../models/Auth';
import { LoginService } from '../../services/login.service';
import { ConfirmEmailComponent } from './confirm-email.component';

describe('ConfirmEmailComponent', () => {
	let component: ConfirmEmailComponent;
	let fixture: ComponentFixture<ConfirmEmailComponent>;
	let mockRouter: jasmine.SpyObj<Router>;
	let mockHttp: jasmine.SpyObj<HttpClient>;
	let mockLoginService: any;
	let mockActivatedRoute: any;

	beforeEach(async () => {
		mockRouter = jasmine.createSpyObj('Router', ['navigate']);
		mockRouter.navigate.and.returnValue(new Promise(() => {}));
		mockHttp = jasmine.createSpyObj('HttpClient', ['post']);
		mockLoginService = {
			usuario: null,
		};

		mockActivatedRoute = {
			snapshot: {
				queryParams: {},
			},
		};

		await TestBed.configureTestingModule({
			imports: [ConfirmEmailComponent],
			providers: [
				{ provide: Router, useValue: mockRouter },
				{ provide: HttpClient, useValue: mockHttp },
				{ provide: LoginService, useValue: mockLoginService },
				{ provide: ActivatedRoute, useValue: mockActivatedRoute },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(ConfirmEmailComponent);
		component = fixture.componentInstance;
	});

	it('should create', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
	});

	describe('backURL', () => {
		it('should return __env.BACK_BASE when defined on window', () => {
			// Arrange
			(window as any).__env = { BACK_BASE: 'https://api.test.com' };

			// Act
			const url = component.backURL;

			// Assert
			expect(url).toBe('https://api.test.com');
			delete (window as any).__env;
		});

		it('should return empty string when __env.BACK_BASE is nullish', () => {
			// Arrange
			(window as any).__env = {};

			// Act
			const url = component.backURL;

			// Assert
			expect(url).toBe('');
			delete (window as any).__env;
		});

		it('should return empty string when __env is not defined', () => {
			// Arrange
			delete (window as any).__env;

			// Act
			const url = component.backURL;

			// Assert
			expect(url).toBe('');
		});
	});

	describe('ngOnInit', () => {
		it('should navigate to root if token query param is missing', () => {
			// Arrange
			mockActivatedRoute.snapshot.queryParams = {};

			// Act
			component.ngOnInit();

			// Assert
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
			expect(mockHttp.post).not.toHaveBeenCalled();
		});

		it('should handle successful confirmation response', fakeAsync(() => {
			// Arrange
			mockActivatedRoute.snapshot.queryParams = { token: 'valid-token-123' };
			const dummyAuth = {
				accessToken: 'fake-jwt',
				usuario: {
					idUsuario: 1,
					nombreUsuario: 'testuser',
					rollUsuario: 'USER',
					fotoUsuario: ['#000'],
				},
			} as unknown as Auth;
			const fakeResponse = new HttpResponse({
				body: dummyAuth,
				status: 200,
			});
			mockHttp.post.and.returnValue(of(fakeResponse as any));
			spyOn(localStorage, 'setItem');

			// Act
			component.ngOnInit();

			// Assert
			expect(mockHttp.post).toHaveBeenCalled();
			expect(mockLoginService.usuario).toEqual(dummyAuth.usuario);
			expect(localStorage.setItem).toHaveBeenCalledWith('Token', 'fake-jwt');
			expect(component.mensajeText).toBe('Tu correo electrónico ha sido confirmado!!!');
			expect(component.isError).toBeFalse();

			// Test timer
			tick(1500);
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
		}));

		it('should not update user if response ok is true but body is null', () => {
			// Arrange
			mockActivatedRoute.snapshot.queryParams = { token: 'token-no-body' };
			const fakeResponse = new HttpResponse<Auth>({
				body: null,
				status: 200,
			});
			mockHttp.post.and.returnValue(of(fakeResponse as any));

			// Act
			component.ngOnInit();

			// Assert
			expect(mockHttp.post).toHaveBeenCalled();
			expect(component.mensajeText).toBe('Confirmando tu correo electrónico...');
		});

		it('should handle error when http request fails', fakeAsync(() => {
			// Arrange
			mockActivatedRoute.snapshot.queryParams = { token: 'invalid-token' };
			const errorResponse = new HttpErrorResponse({
				error: 'Token inválido o expirado',
				status: 400,
			});
			mockHttp.post.and.returnValue(throwError(() => errorResponse));

			// Act
			component.ngOnInit();

			// Assert
			expect(component.isError).toBeTrue();
			expect(component.mensajeText).toBe('Ha habido un error al confirmar tu correo electrónico:');
			expect(component.mensaje2Text).toBe('Token inválido o expirado');

			// Test timer
			tick(3000);
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
		}));
	});

	describe('ngOnDestroy', () => {
		it('should unsubscribe from sub if present', () => {
			// Arrange
			mockActivatedRoute.snapshot.queryParams = { token: 'test-token' };
			mockHttp.post.and.returnValue(of(new HttpResponse({ body: null })));
			component.ngOnInit();
			const sub = (component as any).sub;
			spyOn(sub, 'unsubscribe');

			// Act
			component.ngOnDestroy();

			// Assert
			expect(sub.unsubscribe).toHaveBeenCalled();
		});

		it('should not throw if sub is undefined', () => {
			// Arrange
			(component as any).sub = undefined;

			// Act & Assert
			expect(() => component.ngOnDestroy()).not.toThrow();
		});
	});
});
