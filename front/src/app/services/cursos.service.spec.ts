import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Curso } from '../models/Curso';
import { Plan } from '../models/Plan';
import { Usuario } from '../models/Usuario';
import { CursosService } from './cursos.service';

describe('CursosService', () => {
	let service: CursosService;
	let httpMock: HttpTestingController;

	beforeEach(() => {
		(window as any).__env = { BACK_BASE: 'http://localhost:8080', BACK_STREAM: 'http://localhost:8081' };
		TestBed.configureTestingModule({
			providers: [CursosService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
		});
		service = TestBed.inject(CursosService);
		httpMock = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		delete (window as any).__env;
		httpMock.verify();
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('backURL and backURLStreaming should handle window.__env', () => {
		(window as any).__env = {
			BACK_BASE: 'http://localhost:8080',
			BACK_STREAM: 'http://localhost:8081',
		};
		expect(service.backURL).toBe('http://localhost:8080');
		expect(service.backURLStreaming).toBe('http://localhost:8081');
		delete (window as any).__env;
	});

	it('backURL and backURLStreaming should return empty string when __env is missing', () => {
		delete (window as any).__env;
		expect(service.backURL).toBe('');
		expect(service.backURLStreaming).toBe('');
	});

	it('getCurso should return null if idCurso is 0 or not found', async () => {
		const res1 = await service.getCurso(0);
		expect(res1).toBeNull();

		const res2 = await service.getCurso(999);
		expect(res2).toBeNull();
	});

	it('getCurso should return cached curso if clasesCurso exists and fromServer is false', async () => {
		const mockCurso: Curso = {
			idCurso: 1,
			nombreCurso: 'Angular Test',
			descripcionCorta: 'Desc',
			imagenCurso: 'img.jpg',
			profesoresCurso: [1],
			clasesCurso: [{ idClase: 10, posicionClase: 1, cursoClase: 0, nombreClase: 'Clase 1' }] as any,
		};
		service.cursos.set([mockCurso]);
		const curso = await service.getCurso(1, false);
		expect(curso).toEqual(mockCurso);
	});

	it('getCurso should fetch from server if clasesCurso is missing or fromServer is true', async () => {
		const mockCurso: Curso = {
			idCurso: 1,
			nombreCurso: 'Angular Test',
			descripcionCorta: 'Desc',
			imagenCurso: 'img.jpg',
			profesoresCurso: [1],
		};
		service.cursos.set([mockCurso]);

		const promise = service.getCurso(1, true);

		const req = httpMock.expectOne('http://localhost:8080/cursos/getCurso/1');
		expect(req.request.method).toBe('GET');
		req.flush({
			idCurso: 1,
			nombreCurso: 'Angular Test',
			descripcionCorta: 'Desc',
			imagenCurso: 'img.jpg',
			profesoresCurso: [1],
			clasesCurso: [{ idClase: 10, posicionClase: 1, cursoClase: 0, nombreClase: 'Clase 1' }],
		});

		const curso = await promise;
		expect(curso?.clasesCurso?.length).toBe(1);
		expect(curso?.clasesCurso?.[0].cursoClase).toBe(1);
	});

	it('updateCurso should handle success response', (done) => {
		const mockCurso: Curso = { idCurso: 1, nombreCurso: 'Updated', descripcionCorta: 'Desc', imagenCurso: 'img.jpg', profesoresCurso: [1] };
		service.updateCurso(mockCurso).subscribe((res) => {
			expect(res).toEqual(mockCurso);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/cursos/update');
		expect(req.request.method).toBe('PUT');
		req.flush(mockCurso, { status: 200, statusText: 'OK' });
	});

	it('updateCurso should handle error response', (done) => {
		const mockCurso: Curso = { idCurso: 1, nombreCurso: 'Updated', descripcionCorta: 'Desc', imagenCurso: 'img.jpg', profesoresCurso: [1] };
		service.updateCurso(mockCurso).subscribe({
			error: (err) => {
				expect(err).toBeTruthy();
				done();
			},
		});

		const req = httpMock.expectOne('http://localhost:8080/cursos/update');
		req.flush('Error', { status: 400, statusText: 'Bad Request' });
	});

	it('getCurso should return null on server error or null response', async () => {
		const mockCurso: Curso = { idCurso: 1, nombreCurso: 'Test', descripcionCorta: 'D', imagenCurso: 'i.jpg', profesoresCurso: [1] };
		service.cursos.set([mockCurso]);

		const p1 = service.getCurso(1, true);
		httpMock.expectOne('http://localhost:8080/cursos/getCurso/1').flush(null);
		const r1 = await p1;
		expect(r1).toBeNull();

		const p2 = service.getCurso(1, true);
		httpMock.expectOne('http://localhost:8080/cursos/getCurso/1').flush('Error', { status: 500, statusText: 'Error' });
		const r2 = await p2;
		expect(r2).toBeNull();
	});

	it('updateCurso should throw error if curso is null', () => {
		expect(() => service.updateCurso(null)).toThrowError('El curso no existe!!!');
	});

	it('addImagenCurso should return image url', (done) => {
		const formData = new FormData();
		service.addImagenCurso(formData).subscribe((url) => {
			expect(url).toBe('http://img.com/1.jpg');
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/subeFotos');
		expect(req.request.method).toBe('POST');
		req.flush(['http://img.com/1.jpg'], { status: 200, statusText: 'OK' });
	});

	it('getCursosProfe should return courses belonging to professor', () => {
		const curso1: Curso = { idCurso: 1, nombreCurso: 'C1', descripcionCorta: 'D1', imagenCurso: 'i1.jpg', profesoresCurso: [1, 2] };
		const curso2: Curso = { idCurso: 2, nombreCurso: 'C2', descripcionCorta: 'D2', imagenCurso: 'i2.jpg', profesoresCurso: [3] };
		service.cursos.set([curso1, curso2]);

		const profe: Usuario = { idUsuario: 1 } as any;
		const res = service.getCursosProfe(profe);
		expect(res).toEqual([curso1]);
	});

	it('deleteCurso should remove curso on success', (done) => {
		const curso: Curso = { idCurso: 1, nombreCurso: 'C1', descripcionCorta: 'D1', imagenCurso: 'i1.jpg', profesoresCurso: [1] };
		service.cursos.set([curso]);

		service.deleteCurso(curso).subscribe((success) => {
			expect(success).toBe(true);
			expect(service.cursos().length).toBe(0);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/cursos/delete/1');
		expect(req.request.method).toBe('DELETE');
		req.flush('Deleted', { status: 200, statusText: 'OK' });
	});

	it('getStatusCurso should return status number', (done) => {
		service.getStatusCurso(1).subscribe((status) => {
			expect(status).toBe(2);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8081/status/1');
		expect(req.request.method).toBe('GET');
		req.flush(2, { status: 200, statusText: 'OK' });
	});

	it('getAllCursos should load all cursos', (done) => {
		(window as any).__env = { BACK_BASE: 'http://localhost:8080' };
		const cursos: Curso[] = [{ idCurso: 1, nombreCurso: 'C1', descripcionCorta: 'D1', imagenCurso: 'i1.jpg', profesoresCurso: [1], clasesCurso: [] }];
		service.getAllCursos().subscribe((res) => {
			expect(res as any).toEqual(cursos);
			expect(service.cursos()).toEqual(cursos);
			delete (window as any).__env;
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/cursos/getAll');
		expect(req.request.method).toBe('GET');
		req.flush(cursos, { status: 200, statusText: 'OK' });
	});

	it('getCurso should return null on server error or failure', async () => {
		const mockCurso: Curso = {
			idCurso: 1,
			nombreCurso: 'Angular Test',
			descripcionCorta: 'Desc',
			imagenCurso: 'img.jpg',
			profesoresCurso: [1],
		};
		service.cursos.set([mockCurso]);

		const promise = service.getCurso(1, true);
		const req = httpMock.expectOne('http://localhost:8080/cursos/getCurso/1');
		req.flush('Error', { status: 500, statusText: 'Server Error' });

		const curso = await promise;
		expect(curso).toBeNull();
	});

	it('updateCurso should throw error on HTTP failure', (done) => {
		const mockCurso: Curso = { idCurso: 1, nombreCurso: 'Updated', descripcionCorta: 'Desc', imagenCurso: 'img.jpg', profesoresCurso: [1] };
		service.updateCurso(mockCurso).subscribe({
			error: (err) => {
				expect(err).toBeTruthy();
				done();
			},
		});

		const req = httpMock.expectOne('http://localhost:8080/cursos/update');
		req.flush('Error', { status: 500, statusText: 'Server Error' });
	});

	it('addImagenCurso should return null on error', (done) => {
		const formData = new FormData();
		service.addImagenCurso(formData).subscribe((url) => {
			expect(url).toBeNull();
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/usuario/subeFotos');
		req.flush('Error', { status: 500, statusText: 'Error' });
	});

	it('deleteCurso should return false on error', (done) => {
		const curso: Curso = { idCurso: 1, nombreCurso: 'C1', descripcionCorta: 'D1', imagenCurso: 'i1.jpg', profesoresCurso: [1] };
		service.deleteCurso(curso).subscribe((success) => {
			expect(success).toBe(false);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/cursos/delete/1');
		req.flush('Error', { status: 400, statusText: 'Bad Request' });
	});

	it('getStatusCurso should return 0 on error', (done) => {
		service.getStatusCurso(1).subscribe((status) => {
			expect(status).toBe(0);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8081/status/1');
		req.flush('Error', { status: 500, statusText: 'Error' });
	});

	it('getAllCursos should return empty array on error', (done) => {
		service.getAllCursos().subscribe((res) => {
			expect(res).toEqual([]);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/cursos/getAll');
		req.flush('Error', { status: 500, statusText: 'Error' });
	});

	it('getPlanesCurso should return planes on success and empty array on error', (done) => {
		const planes: Plan[] = [{ idPlan: 1, nombrePlan: 'Plan 1' } as any];
		service.getPlanesCurso(1).subscribe((res) => {
			expect(res).toEqual(planes);

			// Test error branch
			service.getPlanesCurso(1).subscribe((errRes) => {
				expect(errRes).toEqual([]);
				done();
			});
			const reqErr = httpMock.expectOne('http://localhost:8080/cursos/getPlanesCurso/1');
			reqErr.flush('Error', { status: 500, statusText: 'Error' });
		});

		const req = httpMock.expectOne('http://localhost:8080/cursos/getPlanesCurso/1');
		req.flush(planes, { status: 200, statusText: 'OK' });
	});
});
