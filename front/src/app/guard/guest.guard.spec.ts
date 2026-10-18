import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';
import { GuestGuard } from './guest.guard';

describe('GuestGuard', () => {
	let guard: GuestGuard;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;
	let routerSpy: jasmine.SpyObj<Router>;

	beforeEach(() => {
		loginServiceSpy = jasmine.createSpyObj('LoginService', ['usuario']);
		routerSpy = jasmine.createSpyObj('Router', ['navigate']);
	});

	it('should be created', () => {
		TestBed.configureTestingModule({
			providers: [GuestGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(GuestGuard);
		expect(guard).toBeTruthy();
	});

	it('should return false when platform is server', () => {
		TestBed.configureTestingModule({
			providers: [GuestGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'server' }],
		});
		guard = TestBed.inject(GuestGuard);
		const result = guard.canActivate();
		expect(result).toBeFalse();
	});

	it('should navigate to root and return false when no user is logged in', () => {
		TestBed.configureTestingModule({
			providers: [GuestGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(GuestGuard);
		(loginServiceSpy as any).usuario = null;

		const result = guard.canActivate();
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
		expect(result).toBeFalse();
	});

	it('should return true when user is logged in', () => {
		TestBed.configureTestingModule({
			providers: [GuestGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(GuestGuard);
		(loginServiceSpy as any).usuario = { idUsuario: 1 };

		const result = guard.canActivate();
		expect(result).toBeTrue();
	});
});
