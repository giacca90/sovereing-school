import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { LoginService } from './login.service';

describe('LoginService', () => {
	let service: LoginService;
	let httpMock: HttpTestingController;

	beforeEach(() => {
		(window as any).__env = { BACK_BASE: 'http://localhost:8080', FRONTURL: 'http://localhost:4200' };

		TestBed.configureTestingModule({
			providers: [LoginService, { provide: PLATFORM_ID, useValue: 'browser' }, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
		});
		service = TestBed.inject(LoginService);
		httpMock = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		delete (window as any).__env;
		httpMock.verify();
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('apiUrl, loginSSRUrl, logoutSSRUrl should return correct urls', () => {
		expect(service.apiUrl).toBe('http://localhost:8080/login/');
		expect(service.loginSSRUrl).toBe('http://localhost:4200/ssr-login');
		expect(service.logoutSSRUrl).toBe('http://localhost:4200/ssr-logout');
	});

	it('urls should return empty string when __env is missing', () => {
		delete (window as any).__env;
		expect(service.apiUrl).toBe('');
		expect(service.loginSSRUrl).toBe('');
		expect(service.logoutSSRUrl).toBe('');
	});

	it('compruebaCorreo should resolve true when body != 0', async () => {
		const promise = service.compruebaCorreo('test@test.com');
		const req = httpMock.expectOne('http://localhost:8080/login/test@test.com');
		expect(req.request.method).toBe('GET');
		req.flush(123, { status: 200, statusText: 'OK' });
		const result = await promise;
		expect(result).toBe(true);
	});

	it('compruebaCorreo should resolve false when body == 0', async () => {
		const promise = service.compruebaCorreo('test@test.com');
		const req = httpMock.expectOne('http://localhost:8080/login/test@test.com');
		req.flush(0, { status: 200, statusText: 'OK' });
		const result = await promise;
		expect(result).toBe(false);
	});

	it('compruebaPassword should authenticate and set user and token', async () => {
		(service as any).idUsuario = 1;
		const promise = service.compruebaPassword('secret');
		const req = httpMock.expectOne('http://localhost:8080/login/1/secret');
		expect(req.request.method).toBe('GET');
		req.flush({ status: true, usuario: { idUsuario: 1 }, accessToken: 'token123' }, { status: 200, statusText: 'OK' });

		const reqSsr = httpMock.expectOne('http://localhost:4200/ssr-login');
		expect(reqSsr.request.method).toBe('POST');
		reqSsr.flush('OK', { status: 200, statusText: 'OK' });

		const result = await promise;
		expect(result).toBe(true);
		expect(service.usuario).toBeTruthy();
	});

	it('refreshToken should return new access token', (done) => {
		service.refreshToken().subscribe((token) => {
			expect(token).toBe('new-token');
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/login/refresh');
		expect(req.request.method).toBe('POST');
		req.flush({ accessToken: 'new-token' }, { status: 200, statusText: 'OK' });
	});

	it('loginWithToken should set user on success', () => {
		service.loginWithToken('token123');
		const req = httpMock.expectOne('http://localhost:8080/login/loginWithToken');
		expect(req.request.method).toBe('POST');
		req.flush({ idUsuario: 1, nombreUsuario: 'User' }, { status: 200, statusText: 'OK' });
		expect(service.usuario).toBeTruthy();
	});

	it('logout should clear user and perform logout requests', () => {
		service.usuario = { idUsuario: 1 } as any;
		service.logout();

		const reqLogout = httpMock.expectOne('http://localhost:8080/login/logout');
		expect(reqLogout.request.method).toBe('GET');
		reqLogout.flush('OK', { status: 200, statusText: 'OK' });

		const reqSsrLogout = httpMock.expectOne('http://localhost:4200/ssr-logout');
		expect(reqSsrLogout.request.method).toBe('GET');
		reqSsrLogout.flush('OK', { status: 200, statusText: 'OK' });

		expect(service.usuario).toBeNull();
	});

	it('cargarUsuarioDesdeTransferState should load user in browser', async () => {
		(window as any)['TRANSFER_STATE'] = { usuario: { idUsuario: 5 } };
		await service.cargarUsuarioDesdeTransferState();
		expect(service.usuario?.idUsuario).toBe(5);
	});

	it('cargarUsuarioDesdeTransferState should set user to null if not in TRANSFER_STATE', async () => {
		(window as any)['TRANSFER_STATE'] = {};
		service.usuario = { idUsuario: 1 } as any;
		await service.cargarUsuarioDesdeTransferState();
		expect(service.usuario).toBeNull();
	});

	it('compruebaCorreo should return false on HTTP error', async () => {
		const promise = service.compruebaCorreo('test@test.com');
		const req = httpMock.expectOne('http://localhost:8080/login/test@test.com');
		req.flush('Error', { status: 500, statusText: 'Server Error' });
		const result = await promise;
		expect(result).toBe(false);
	});

	it('compruebaPassword should return false when status is false', async () => {
		(service as any).idUsuario = 1;
		const promise = service.compruebaPassword('wrong');
		const req = httpMock.expectOne('http://localhost:8080/login/1/wrong');
		req.flush({ status: false, usuario: null, accessToken: '' }, { status: 200, statusText: 'OK' });
		const result = await promise;
		expect(result).toBe(false);
	});

	it('compruebaPassword should return false on HTTP error', async () => {
		(service as any).idUsuario = 1;
		const promise = service.compruebaPassword('secret');
		const req = httpMock.expectOne('http://localhost:8080/login/1/secret');
		req.flush('Error', { status: 400, statusText: 'Bad Request' });
		const result = await promise;
		expect(result).toBe(false);
	});

	it('refreshToken should return null on error', (done) => {
		service.refreshToken().subscribe((token) => {
			expect(token).toBeNull();
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/login/refresh');
		req.flush('Error', { status: 500, statusText: 'Error' });
	});

	it('loginWithToken should handle error response', () => {
		service.loginWithToken('invalid');
		const req = httpMock.expectOne('http://localhost:8080/login/loginWithToken');
		req.flush('Error', { status: 401, statusText: 'Unauthorized' });
		expect(service).toBeTruthy();
	});

	it('logout should handle non-200 or error responses', () => {
		service.logout();
		const reqLogout = httpMock.expectOne('http://localhost:8080/login/logout');
		reqLogout.flush('Error', { status: 500, statusText: 'Error' });

		const reqSsrLogout = httpMock.expectOne('http://localhost:4200/ssr-logout');
		reqSsrLogout.flush('Error', { status: 500, statusText: 'Error' });
		expect(service).toBeTruthy();
	});

	it('logout should log error when response status is not 200', () => {
		spyOn(console, 'error');
		service.logout();
		const reqLogout = httpMock.expectOne('http://localhost:8080/login/logout');
		reqLogout.flush('Bad', { status: 400, statusText: 'Bad Request' });

		const reqSsrLogout = httpMock.expectOne('http://localhost:4200/ssr-logout');
		reqSsrLogout.flush('Bad', { status: 400, statusText: 'Bad Request' });
		expect(console.error).toHaveBeenCalled();
	});

	it('loginSSR should handle non-200 or error responses', () => {
		service.loginSSR('token');
		const reqSsr = httpMock.expectOne('http://localhost:4200/ssr-login');
		reqSsr.flush({ ok: false }, { status: 200, statusText: 'OK' });

		service.loginSSR('token2');
		const reqSsr2 = httpMock.expectOne('http://localhost:4200/ssr-login');
		reqSsr2.flush('Error', { status: 400, statusText: 'Bad Request' });
		expect(service).toBeTruthy();
	});

	it('compruebaCorreo should return false on non-ok response', async () => {
		const promise = service.compruebaCorreo('test@test.com');
		const req = httpMock.expectOne('http://localhost:8080/login/test@test.com');
		req.flush('Fail', { status: 400, statusText: 'Bad Request' });
		const result = await promise;
		expect(result).toBe(false);
	});

	it('compruebaPassword should handle non-ok or null body response', async () => {
		(service as any).idUsuario = 1;
		const promise = service.compruebaPassword('secret');
		const req = httpMock.expectOne('http://localhost:8080/login/1/secret');
		req.flush(null, { status: 400, statusText: 'Bad Request' });
		const result = await promise;
		expect(result).toBe(false);
	});

	it('refreshToken should return null when body is missing', (done) => {
		service.refreshToken().subscribe((token) => {
			expect(token).toBeNull();
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/login/refresh');
		req.flush(null, { status: 200, statusText: 'OK' });
	});

	it('loginWithToken should handle null body response', () => {
		service.loginWithToken('token123');
		const req = httpMock.expectOne('http://localhost:8080/login/loginWithToken');
		req.flush(null, { status: 200, statusText: 'OK' });
		expect(service.usuario).toBeNull();
	});

	it('cargarUsuarioDesdeTransferState should load user on server platform', async () => {
		TestBed.resetTestingModule();
		TestBed.configureTestingModule({
			providers: [LoginService, { provide: PLATFORM_ID, useValue: 'server' }, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
		});
		const serverService = TestBed.inject(LoginService);
		(globalThis as any).ssrUsuario = { idUsuario: 10 };
		await serverService.cargarUsuarioDesdeTransferState();
		expect(serverService.usuario?.idUsuario).toBe(10);
		delete (globalThis as any).ssrUsuario;
	});
});
