import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CursosService } from './cursos.service';
import { InitService } from './init.service';
import { LoginService } from './login.service';
import { UsuariosService } from './usuarios.service';

describe('InitService', () => {
	let service: InitService;
	let httpMock: HttpTestingController;
	let cursosService: CursosService;
	let usuariosService: UsuariosService;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;

	beforeEach(() => {
		loginServiceSpy = jasmine.createSpyObj('LoginService', ['refreshToken', 'logout']);

		(window as any).__env = { BACK_BASE: 'http://localhost:8080' };

		TestBed.configureTestingModule({
			imports: [HttpClientTestingModule],
			providers: [InitService, CursosService, UsuariosService, { provide: LoginService, useValue: loginServiceSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		service = TestBed.inject(InitService);
		httpMock = TestBed.inject(HttpTestingController);
		cursosService = TestBed.inject(CursosService);
		usuariosService = TestBed.inject(UsuariosService);
	});

	afterEach(() => {
		httpMock.verify();
		delete (window as any).__env;
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('apiUrl getter should return correct browser URL', () => {
		expect(service.apiUrl).toBe('http://localhost:8080/init');
	});

	it('apiUrl getter should fallback when __env is missing', () => {
		delete (window as any).__env;
		(globalThis as any).process = { env: {} };
		expect(service.apiUrl).toBe('https://localhost:8080/init');
		delete (globalThis as any).process;
	});

	it('apiUrl getter should use process.env.BACK_BASE when window.__env is missing', () => {
		delete (window as any).__env;
		(globalThis as any).process = { env: { BACK_BASE: 'http://env-base' } };
		expect(service.apiUrl).toBe('http://env-base/init');
		delete (globalThis as any).process;
	});

	it('apiUrl getter should return server URL when platform is server', () => {
		TestBed.resetTestingModule();
		(globalThis as any).process = { env: { BACK_BASE_DOCKER: 'http://docker-backend' } };
		TestBed.configureTestingModule({
			imports: [HttpClientTestingModule],
			providers: [InitService, CursosService, UsuariosService, { provide: LoginService, useValue: loginServiceSpy }, { provide: PLATFORM_ID, useValue: 'server' }],
		});
		const srv = TestBed.inject(InitService);
		expect(srv.apiUrl).toBe('http://docker-backend/init');
		delete (globalThis as any).process;
	});

	it('preloadFromGlobalCache should set TransferState when global cache exists', () => {
		const transferState = (service as any).transferState;
		spyOn(transferState, 'set');
		const { setGlobalInitCache } = require('../../init-cache');
		setGlobalInitCache({ profesInit: [], cursosInit: [], estadistica: null });
		service.preloadFromGlobalCache();
		expect(transferState.set).toHaveBeenCalled();
		setGlobalInitCache(null as any);
	});

	it('carga should fetch from backend when cache is empty', async () => {
		const initData = { profesInit: [], cursosInit: [], estadistica: null } as any;
		const promise = service.carga();

		const req = httpMock.expectOne('http://localhost:8080/init');
		expect(req.request.method).toBe('GET');
		req.flush(initData);

		const result = await promise;
		expect(result).toBeTrue();
		expect(cursosService.cursos()).toEqual([]);
	});

	it('carga should use memory cache on subsequent calls', async () => {
		const initData = { profesInit: [], cursosInit: [], estadistica: null } as any;

		// First call
		const p1 = service.carga();
		httpMock.expectOne('http://localhost:8080/init').flush(initData);
		await p1;

		// Second call should use cache
		const result = await service.carga();
		expect(result).toBeTrue();
		httpMock.expectNone('http://localhost:8080/init');
	});

	it('carga should handle HTTP error gracefully', async () => {
		const promise = service.carga();

		const req = httpMock.expectOne('http://localhost:8080/init');
		req.flush('Error', { status: 500, statusText: 'Server Error' });

		const result = await promise;
		expect(result).toBeFalse();
	});

	it('cargarUsuario should set loginService.usuario on success', async () => {
		const user = { idUsuario: 1, nombre: 'Test' } as any;
		const promise = service.cargarUsuario();

		const req = httpMock.expectOne('http://localhost:8080/init/auth');
		expect(req.request.method).toBe('GET');
		req.flush(user);

		await promise;
		expect(loginServiceSpy.usuario).toEqual(user);
	});

	it('cargarUsuario should set loginService.usuario to null on error', async () => {
		const promise = service.cargarUsuario();

		const req = httpMock.expectOne('http://localhost:8080/init/auth');
		req.flush('Error', { status: 500, statusText: 'Server Error' });

		await promise;
		expect(loginServiceSpy.usuario).toBeNull();
	});

	it('carga should use TransferState when key exists', async () => {
		const transferState = (service as any).transferState;
		spyOn(transferState, 'hasKey').and.returnValue(true);
		spyOn(transferState, 'get').and.returnValue({ profesInit: [], cursosInit: [], estadistica: null });

		const result = await service.carga();
		expect(result).toBeTrue();
	});

	it('carga should use global init cache on server platform', async () => {
		TestBed.resetTestingModule();
		(globalThis as any).process = { env: { BACK_BASE_DOCKER: 'http://docker-backend' } };
		TestBed.configureTestingModule({
			imports: [HttpClientTestingModule],
			providers: [InitService, CursosService, UsuariosService, { provide: LoginService, useValue: loginServiceSpy }, { provide: PLATFORM_ID, useValue: 'server' }],
		});
		const srv = TestBed.inject(InitService);
		const initData = { profesInit: [], cursosInit: [], estadistica: null } as any;
		const { setGlobalInitCache } = require('../../init-cache');
		setGlobalInitCache(initData);

		const result = await srv.carga();
		expect(result).toBeTrue();
		setGlobalInitCache(null as any);
		delete (globalThis as any).process;
	});

	it('carga should fetch and set global cache and transferState on server platform', async () => {
		TestBed.resetTestingModule();
		(globalThis as any).process = { env: { BACK_BASE_DOCKER: 'http://docker-backend' } };
		TestBed.configureTestingModule({
			imports: [HttpClientTestingModule],
			providers: [InitService, CursosService, UsuariosService, { provide: LoginService, useValue: loginServiceSpy }, { provide: PLATFORM_ID, useValue: 'server' }],
		});
		const srv = TestBed.inject(InitService);
		const http = TestBed.inject(HttpTestingController);
		const { setGlobalInitCache } = require('../../init-cache');
		setGlobalInitCache(null as any);

		const initData = { profesInit: [], cursosInit: [], estadistica: null } as any;
		const promise = srv.carga();

		const req = http.expectOne('http://docker-backend/init');
		req.flush(initData);

		const result = await promise;
		expect(result).toBeTrue();
		setGlobalInitCache(null as any);
		delete (globalThis as any).process;
		http.verify();
	});
});
