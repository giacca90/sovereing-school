import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Curso } from '../models/Curso';
import { Usuario } from '../models/Usuario';
import { LoginService } from './login.service';
import { UsuariosService } from './usuarios.service';

describe('UsuariosService', () => {
	let service: UsuariosService;
	let httpMock: HttpTestingController;
	let loginServiceMock: { usuario: Usuario | null };

	beforeEach(() => {
		loginServiceMock = { usuario: { idUsuario: 1, cursosUsuario: [] } as any };
		(window as any).__env = { BACK_BASE: 'http://localhost:8080' };

		TestBed.configureTestingModule({
			providers: [UsuariosService, { provide: LoginService, useValue: loginServiceMock }, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
		});
		service = TestBed.inject(UsuariosService);
		httpMock = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		delete (window as any).__env;
		httpMock.verify();
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('apiUrl should return correct base url', () => {
		expect(service.apiUrl).toBe('http://localhost:8080/usuario/');
	});

	it('getUsuario should perform GET request', () => {
		service.getUsuario(1);
		const req = httpMock.expectOne('http://localhost:8080/usuario/1');
		expect(req.request.method).toBe('GET');
		req.flush({ idUsuario: 1, nombreUsuario: 'Admin' });
	});

	it('getNombreProfe should return profesor name', () => {
		service.profes = [{ idUsuario: 10, nombreUsuario: 'Profesor X' } as any];
		expect(service.getNombreProfe(10)).toBe('Profesor X');
		expect(service.getNombreProfe(99)).toBeUndefined();
	});

	it('save should return image URLs on success', (done) => {
		const formData = new FormData();
		service.save(formData).subscribe((res) => {
			expect(res).toEqual(['img1.jpg']);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/subeFotos');
		expect(req.request.method).toBe('POST');
		req.flush(['img1.jpg'], { status: 200, statusText: 'OK' });
	});

	it('actualizaUsuario should return true on success', (done) => {
		const user: Usuario = { idUsuario: 1 } as any;
		service.actualizaUsuario(user).subscribe((success) => {
			expect(success).toBe(true);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/edit');
		expect(req.request.method).toBe('PUT');
		req.flush('Updated', { status: 200, statusText: 'OK' });
	});

	it('getAllUsuarios should return list of users', (done) => {
		service.getAllUsuarios().subscribe((users) => {
			expect(users?.length).toBe(1);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/getAll');
		expect(req.request.method).toBe('GET');
		req.flush([{ idUsuario: 1 }], { status: 200, statusText: 'OK' });
	});

	it('eliminaUsuario should delete user', (done) => {
		const user: Usuario = { idUsuario: 1 } as any;
		service.eliminaUsuario(user).subscribe((success) => {
			expect(success).toBe(true);
			done();
		});

		const reqs = httpMock.match((r) => r.url.includes('/usuario/delete/1') || r.url.includes('/usuario/getAll'));
		expect(reqs.length).toBeGreaterThan(0);
		reqs[0].flush('Deleted', { status: 200, statusText: 'OK' });
		if (reqs[1]) reqs[1].flush([], { status: 200, statusText: 'OK' });
	});

	it('cursoComprado should return true if already purchased', (done) => {
		const curso: Curso = { idCurso: 1 } as any;
		loginServiceMock.usuario = { idUsuario: 1, cursosUsuario: [curso] } as any;

		service.cursoComprado(curso).subscribe((res) => {
			expect(res).toBe(true);
			done();
		});
	});

	it('getUsuario should handle error response', () => {
		spyOn(console, 'error');
		service.getUsuario(1);
		const req = httpMock.expectOne('http://localhost:8080/usuario/1');
		req.flush('Error', { status: 404, statusText: 'Not Found' });
		expect(console.error).toHaveBeenCalled();
	});

	it('save should return null on error', (done) => {
		const formData = new FormData();
		service.save(formData).subscribe((res) => {
			expect(res).toBeNull();
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/subeFotos');
		req.flush('Error', { status: 500, statusText: 'Error' });
	});

	it('actualizaUsuario should return false on error', (done) => {
		const user: Usuario = { idUsuario: 1 } as any;
		service.actualizaUsuario(user).subscribe((success) => {
			expect(success).toBe(false);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/edit');
		req.flush('Error', { status: 400, statusText: 'Bad Request' });
	});

	it('getAllUsuarios should return empty array on error', (done) => {
		service.getAllUsuarios().subscribe((users) => {
			expect(users).toEqual([]);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/getAll');
		req.flush('Error', { status: 500, statusText: 'Error' });
	});

	it('eliminaUsuario should return false on error', (done) => {
		const user: Usuario = { idUsuario: 1 } as any;
		service.eliminaUsuario(user).subscribe((success) => {
			expect(success).toBe(false);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/delete/1');
		req.flush('Error', { status: 400, statusText: 'Bad Request' });
	});

	it('cursoComprado should buy course if not purchased', (done) => {
		const curso: Curso = { idCurso: 2 } as any;
		loginServiceMock.usuario = { idUsuario: 1, cursosUsuario: [] } as any;

		service.cursoComprado(curso).subscribe((res) => {
			expect(res).toBe(true);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/cursos');
		expect(req.request.method).toBe('PUT');
		req.flush('Success', { status: 200, statusText: 'OK' });
	});

	it('cursoComprado should return false when user is null', (done) => {
		loginServiceMock.usuario = null;
		const curso: Curso = { idCurso: 1 } as any;

		service.cursoComprado(curso).subscribe((res) => {
			expect(res).toBe(false);
			done();
		});
	});

	it('cursoComprado should return false on HTTP error', (done) => {
		const curso: Curso = { idCurso: 2 } as any;
		loginServiceMock.usuario = { idUsuario: 1, cursosUsuario: [] } as any;

		service.cursoComprado(curso).subscribe((res) => {
			expect(res).toBe(false);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/cursos');
		req.flush('Error', { status: 500, statusText: 'Server Error' });
	});
});
