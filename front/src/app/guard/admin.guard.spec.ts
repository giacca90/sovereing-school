import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';
import { AdminGuard } from './admin.guard';

describe('AdminGuard', () => {
	let guard: AdminGuard;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;
	let routerSpy: jasmine.SpyObj<Router>;

	beforeEach(() => {
		loginServiceSpy = jasmine.createSpyObj('LoginService', ['usuario']);
		routerSpy = jasmine.createSpyObj('Router', ['navigate']);
	});

	it('should be created', () => {
		TestBed.configureTestingModule({
			providers: [AdminGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(AdminGuard);
		expect(guard).toBeTruthy();
	});

	it('should return false when platform is server', () => {
		TestBed.configureTestingModule({
			providers: [AdminGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'server' }],
		});
		guard = TestBed.inject(AdminGuard);
		const result = guard.canActivate();
		expect(result).toBeFalse();
	});

	it('should navigate to root and return false when user is not admin', () => {
		TestBed.configureTestingModule({
			providers: [AdminGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(AdminGuard);
		(loginServiceSpy as any).usuario = { rollUsuario: 'user' };

		const result = guard.canActivate();
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
		expect(result).toBeFalse();
	});

	it('should return true when user is admin', () => {
		TestBed.configureTestingModule({
			providers: [AdminGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(AdminGuard);
		(loginServiceSpy as any).usuario = { rollUsuario: 'admin' };

		const result = guard.canActivate();
		expect(result).toBeTrue();
	});
});
