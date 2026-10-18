import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { CursosService } from '../services/cursos.service';
import { LoginService } from '../services/login.service';
import { ProfGuard } from './prof.guard';

describe('ProfGuard', () => {
	let guard: ProfGuard;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;
	let cursosServiceSpy: jasmine.SpyObj<CursosService>;
	let routerSpy: jasmine.SpyObj<Router>;

	beforeEach(() => {
		loginServiceSpy = jasmine.createSpyObj('LoginService', ['usuario']);
		cursosServiceSpy = jasmine.createSpyObj('CursosService', ['getCurso']);
		routerSpy = jasmine.createSpyObj('Router', ['navigate']);
	});

	it('should be created', () => {
		TestBed.configureTestingModule({
			providers: [ProfGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: CursosService, useValue: cursosServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(ProfGuard);
		expect(guard).toBeTruthy();
	});

	it('should return false when platform is server', async () => {
		TestBed.configureTestingModule({
			providers: [ProfGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: CursosService, useValue: cursosServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'server' }],
		});
		guard = TestBed.inject(ProfGuard);
		const route = { params: { idCurso: '1' } } as unknown as ActivatedRouteSnapshot;
		const state = {} as RouterStateSnapshot;
		const result = await guard.canActivate(route, state);
		expect(result).toBeFalse();
	});

	it('should navigate to root and return false when no user is logged in', async () => {
		TestBed.configureTestingModule({
			providers: [ProfGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: CursosService, useValue: cursosServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(ProfGuard);
		(loginServiceSpy as any).usuario = null;

		const route = { params: { idCurso: '1' } } as unknown as ActivatedRouteSnapshot;
		const state = {} as RouterStateSnapshot;
		const result = await guard.canActivate(route, state);
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
		expect(result).toBeFalse();
	});

	it('should return true when idCurso is 0', async () => {
		TestBed.configureTestingModule({
			providers: [ProfGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: CursosService, useValue: cursosServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(ProfGuard);
		(loginServiceSpy as any).usuario = { idUsuario: 1 };

		const route = { params: { idCurso: '0' } } as unknown as ActivatedRouteSnapshot;
		const state = {} as RouterStateSnapshot;
		const result = await guard.canActivate(route, state);
		expect(result).toBeTrue();
	});

	it('should navigate to root and return false when curso is not found', async () => {
		TestBed.configureTestingModule({
			providers: [ProfGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: CursosService, useValue: cursosServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(ProfGuard);
		(loginServiceSpy as any).usuario = { idUsuario: 1 };
		cursosServiceSpy.getCurso.and.returnValue(Promise.resolve(null as any));

		const route = { params: { idCurso: '123' } } as unknown as ActivatedRouteSnapshot;
		const state = {} as RouterStateSnapshot;
		const result = await guard.canActivate(route, state);
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
		expect(result).toBeFalse();
	});

	it('should return true when user is a professor of the curso', async () => {
		TestBed.configureTestingModule({
			providers: [ProfGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: CursosService, useValue: cursosServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(ProfGuard);
		(loginServiceSpy as any).usuario = { idUsuario: 1 };
		cursosServiceSpy.getCurso.and.returnValue(Promise.resolve({ profesoresCurso: [1, 2] } as any));

		const route = { params: { idCurso: '123' } } as unknown as ActivatedRouteSnapshot;
		const state = {} as RouterStateSnapshot;
		const result = await guard.canActivate(route, state);
		expect(result).toBeTrue();
	});

	it('should navigate to root and return false when user is not a professor of the curso', async () => {
		TestBed.configureTestingModule({
			providers: [ProfGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: CursosService, useValue: cursosServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(ProfGuard);
		(loginServiceSpy as any).usuario = { idUsuario: 5 };
		cursosServiceSpy.getCurso.and.returnValue(Promise.resolve({ profesoresCurso: [1, 2] } as any));

		const route = { params: { idCurso: '123' } } as unknown as ActivatedRouteSnapshot;
		const state = {} as RouterStateSnapshot;
		const result = await guard.canActivate(route, state);
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
		expect(result).toBeFalse();
	});

	it('should handle error when getCurso throws', async () => {
		TestBed.configureTestingModule({
			providers: [ProfGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: CursosService, useValue: cursosServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(ProfGuard);
		(loginServiceSpy as any).usuario = { idUsuario: 1 };
		cursosServiceSpy.getCurso.and.returnValue(Promise.reject('Network error'));

		const route = { params: { idCurso: '123' } } as unknown as ActivatedRouteSnapshot;
		const state = {} as RouterStateSnapshot;
		const result = await guard.canActivate(route, state);
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
		expect(result).toBeFalse();
	});
});
