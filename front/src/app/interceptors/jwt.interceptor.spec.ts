import { HttpHandlerFn, HttpInterceptorFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { jwtInterceptor } from './jwt.interceptor';

describe('jwtInterceptor', () => {
	const executeInterceptor: HttpInterceptorFn = (req, next) => TestBed.runInInjectionContext(() => jwtInterceptor(req, next));

	beforeEach(() => {
		TestBed.configureTestingModule({});
		localStorage.clear();
	});

	it('should be created', () => {
		const interceptor: HttpInterceptorFn = (req, next) => TestBed.runInInjectionContext(() => jwtInterceptor(req, next));
		expect(interceptor).toBeTruthy();
	});

	it('should add basic auth header when url includes init', (done) => {
		TestBed.overrideProvider(PLATFORM_ID, { useValue: 'browser' });
		const req = new HttpRequest('GET', '/api/init');
		const next: HttpHandlerFn = (request) => {
			expect(request.headers.has('Authorization')).toBeTrue();
			expect(request.headers.get('Authorization')).toContain('Basic');
			return of(new HttpResponse({ status: 200 }));
		};

		executeInterceptor(req, next).subscribe(() => done());
	});

	it('should add bearer token when token exists in localStorage on browser', (done) => {
		TestBed.overrideProvider(PLATFORM_ID, { useValue: 'browser' });
		localStorage.setItem('Token', 'my-test-token');
		const req = new HttpRequest('GET', '/api/cursos');
		const next: HttpHandlerFn = (request) => {
			expect(request.headers.get('Authorization')).toBe('Bearer my-test-token');
			return of(new HttpResponse({ status: 200 }));
		};

		executeInterceptor(req, next).subscribe(() => done());
	});

	it('should add basic auth when no token exists in localStorage on browser', (done) => {
		TestBed.overrideProvider(PLATFORM_ID, { useValue: 'browser' });
		const req = new HttpRequest('GET', '/api/cursos');
		const next: HttpHandlerFn = (request) => {
			expect(request.headers.get('Authorization')).toContain('Basic');
			return of(new HttpResponse({ status: 200 }));
		};

		executeInterceptor(req, next).subscribe(() => done());
	});

	it('should add basic auth on server platform', (done) => {
		TestBed.overrideProvider(PLATFORM_ID, { useValue: 'server' });
		const req = new HttpRequest('GET', '/api/cursos');
		const next: HttpHandlerFn = (request) => {
			expect(request.headers.get('Authorization')).toContain('Basic');
			return of(new HttpResponse({ status: 200 }));
		};

		executeInterceptor(req, next).subscribe(() => done());
	});

	it('should not add token when url includes refresh on browser', (done) => {
		TestBed.overrideProvider(PLATFORM_ID, { useValue: 'browser' });
		localStorage.setItem('Token', 'my-token');
		const req = new HttpRequest('GET', '/login/refresh');
		const next: HttpHandlerFn = (request) => {
			expect(request.headers.has('Authorization')).toBeFalse();
			return of(new HttpResponse({ status: 200 }));
		};

		executeInterceptor(req, next).subscribe(() => done());
	});
});
