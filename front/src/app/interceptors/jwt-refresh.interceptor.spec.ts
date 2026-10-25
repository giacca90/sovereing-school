import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { LoginService } from '../services/login.service';
import { jwtRefreshInterceptor } from './jwt-refresh.interceptor';

describe('jwtRefreshInterceptor', () => {
	let loginServiceSpy: jasmine.SpyObj<LoginService>;
	const executeInterceptor: HttpInterceptorFn = (req, next) => TestBed.runInInjectionContext(() => jwtRefreshInterceptor(req, next));

	beforeEach(() => {
		loginServiceSpy = jasmine.createSpyObj('LoginService', ['refreshToken', 'logout']);
		TestBed.configureTestingModule({
			providers: [{ provide: LoginService, useValue: loginServiceSpy }],
		});
		localStorage.clear();
	});

	it('should be created', () => {
		expect(executeInterceptor).toBeTruthy();
	});

	it('should pass through successful requests', (done) => {
		const req = new HttpRequest('GET', '/api/test');
		const next: HttpHandlerFn = () => of(new HttpResponse({ status: 200 }));

		executeInterceptor(req, next).subscribe((res: any) => {
			expect(res.status).toBe(200);
			done();
		});
	});

	it('should refresh token on 401 error and retry request successfully with loginWithToken body update', (done) => {
		const req = new HttpRequest('POST', '/api/loginWithToken', { token: 'old' });
		let attempts = 0;
		const next: HttpHandlerFn = (request) => {
			attempts++;
			if (attempts === 1) {
				return throwError(() => new HttpErrorResponse({ status: 401 }));
			}
			expect(request.headers.get('Authorization')).toBe('Bearer new-token');
			expect(request.body).toBe('new-token');
			return of(new HttpResponse({ status: 200 }));
		};

		loginServiceSpy.refreshToken.and.returnValue(of('new-token'));

		executeInterceptor(req, next).subscribe((res: any) => {
			expect(res.status).toBe(200);
			expect(localStorage.getItem('Token')).toBe('new-token');
			done();
		});
	});

	it('should logout and throw error when refresh token returns empty', (done) => {
		const req = new HttpRequest('GET', '/api/test');
		let attempts = 0;
		const next: HttpHandlerFn = () => {
			attempts++;
			if (attempts === 1) {
				return throwError(() => new HttpErrorResponse({ status: 401 }));
			}
			return of(new HttpResponse({ status: 200 }));
		};

		loginServiceSpy.refreshToken.and.returnValue(of(''));

		executeInterceptor(req, next).subscribe({
			error: (err) => {
				expect(err.message).toBe('No se pudo refrescar el token');
				expect(loginServiceSpy.logout).toHaveBeenCalled();
				done();
			},
		});
	});

	it('should logout and throw error when refresh token fails', (done) => {
		const req = new HttpRequest('GET', '/api/test');
		let attempts = 0;
		const next: HttpHandlerFn = () => {
			attempts++;
			if (attempts === 1) {
				return throwError(() => new HttpErrorResponse({ status: 401 }));
			}
			return of(new HttpResponse({ status: 200 }));
		};

		loginServiceSpy.refreshToken.and.returnValue(throwError(() => new Error('Refresh failed')));

		executeInterceptor(req, next).subscribe({
			error: (err) => {
				expect(loginServiceSpy.logout).toHaveBeenCalled();
				done();
			},
		});
	});
});
