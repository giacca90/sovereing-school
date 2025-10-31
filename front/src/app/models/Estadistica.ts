export class Estadistica {
	public profesores: number;
	public alumnos: number;
	public cursos: number;
	public clases: number;

	constructor(_profesores: number, _alumnos: number, Cursos: number, Clases: number) {
		this.profesores = _profesores;
		this.alumnos = _alumnos;
		this.cursos = Cursos;
		this.clases = Clases;
	}
}
