import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';
import { UserGuard } from './user.guard';

describe('guestGuard', () => {
	let guard: UserGuard;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;
	let routerSpy: jasmine.SpyObj<Router>;

	beforeEach(() => {
		loginServiceSpy = jasmine.createSpyObj('LoginService', ['usuario']);
		routerSpy = jasmine.createSpyObj('Router', ['navigate']);

		TestBed.configureTestingModule({
			providers: [UserGuard, { provide: LoginService, useValue: loginServiceSpy }, { provide: Router, useValue: routerSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		guard = TestBed.inject(UserGuard);
	});

	it('should be created', () => {
		expect(guard).toBeTruthy();
	});
});
