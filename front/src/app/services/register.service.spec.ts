import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NuevoUsuario } from '../models/NuevoUsuario';
import { RegisterService } from './register.service';

describe('RegisterService', () => {
	let service: RegisterService;
	let httpMock: HttpTestingController;

	beforeEach(() => {
		(window as any).__env = { BACK_BASE: 'http://localhost:8080' };
		TestBed.configureTestingModule({
			imports: [HttpClientTestingModule],
			providers: [RegisterService],
		});
		service = TestBed.inject(RegisterService);
		httpMock = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		httpMock.verify();
		delete (window as any).__env;
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('apiUrl getter should return correct value', () => {
		expect(service.apiUrl).toBe('http://localhost:8080');
	});

	it('apiUrl should return empty string when __env is missing', () => {
		delete (window as any).__env;
		expect(service.apiUrl).toBe('');
	});

	it('registrarNuevoUsuario should resolve true on success message', async () => {
		const user: NuevoUsuario = { nombre: 'Test', email: 'test@test.com', password: '123' } as any;
		const promise = service.registrarNuevoUsuario(user);

		const req = httpMock.expectOne('http://localhost:8080/usuario/nuevo');
		expect(req.request.method).toBe('POST');
		req.event(new HttpResponse({ status: 200, body: 'Correo enviado con éxito!!!' }));

		const result = await promise;
		expect(result).toBeTrue();
	});

	it('registrarNuevoUsuario should resolve false on different body or error', async () => {
		const user: NuevoUsuario = { nombre: 'Test', email: 'test@test.com', password: '123' } as any;
		const promise = service.registrarNuevoUsuario(user);

		const req = httpMock.expectOne('http://localhost:8080/usuario/nuevo');
		req.flush('Error', { status: 400, statusText: 'Bad Request' });

		const result = await promise;
		expect(result).toBeFalse();
	});

	it('registrarNuevoUsuario should call alert on 200 with unexpected body', () => {
		spyOn(window, 'alert');
		const user: NuevoUsuario = { nombre: 'Test', email: 'test@test.com', password: '123' } as any;
		service.registrarNuevoUsuario(user);

		const req = httpMock.expectOne('http://localhost:8080/usuario/nuevo');
		req.event(new HttpResponse({ status: 200, body: 'Some other message' }));
		expect(window.alert).toHaveBeenCalled();
	});

	it('registrarNuevoUsuario should resolve false when status is not 200', async () => {
		const user: NuevoUsuario = { nombre: 'Test', email: 'test@test.com', password: '123' } as any;
		const promise = service.registrarNuevoUsuario(user);

		const req = httpMock.expectOne('http://localhost:8080/usuario/nuevo');
		req.event(new HttpResponse({ status: 201, body: null }));

		const result = await promise;
		expect(result).toBeFalse();
	});
});
