import { Estadistica } from './Estadistica';
import { CursosInit, Init, ProfesInit } from './Init';

describe('Init Models', () => {
	describe('Init', () => {
		it('should create an instance of Init with correct properties', () => {
			const cursos = [new CursosInit(1, 'Angular', [1], 'Desc', 'img.png')];
			const profes = [new ProfesInit(1, 'John Doe', ['photo.jpg'], 'Bio')];
			const estadistica = new Estadistica(10, 5, 2, 100);

			const init = new Init(cursos, profes, estadistica);

			expect(init).toBeTruthy();
			expect(init.cursosInit).toEqual(cursos);
			expect(init.profesInit).toEqual(profes);
			expect(init.estadistica).toEqual(estadistica);
		});
	});

	describe('ProfesInit', () => {
		it('should create an instance of ProfesInit with correct properties', () => {
			const prof = new ProfesInit(1, 'Jane Doe', ['jane.jpg'], 'Expert');

			expect(prof).toBeTruthy();
			expect(prof.idUsuario).toBe(1);
			expect(prof.nombreUsuario).toBe('Jane Doe');
			expect(prof.fotoUsuario).toEqual(['jane.jpg']);
			expect(prof.presentacion).toBe('Expert');
		});
	});

	describe('CursosInit', () => {
		it('should create an instance of CursosInit with correct properties', () => {
			const curso = new CursosInit(2, 'TypeScript', [1, 2], 'TS Desc', 'ts.png');

			expect(curso).toBeTruthy();
			expect(curso.idCurso).toBe(2);
			expect(curso.nombreCurso).toBe('TypeScript');
			expect(curso.profesoresCurso).toEqual([1, 2]);
			expect(curso.descripcionCorta).toBe('TS Desc');
			expect(curso.imagenCurso).toBe('ts.png');
		});
	});
});
