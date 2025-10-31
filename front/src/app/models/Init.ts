import { Estadistica } from './Estadistica';

export class Init {
	public profesInit: ProfesInit[];
	public cursosInit: CursosInit[];
	public estadistica: Estadistica;

	constructor(Curso: CursosInit[], _prof: ProfesInit[], _estadistica: Estadistica) {
		this.profesInit = _prof;
		this.cursosInit = Curso;
		this.estadistica = _estadistica;
	}
}

export class ProfesInit {
	idUsuario: number;
	nombreUsuario: string;
	fotoUsuario: string[];
	presentacion: string;

	constructor(_id: number, _nombre: string, _foto: string[], _presentacion: string) {
		this.idUsuario = _id;
		this.nombreUsuario = _nombre;
		this.fotoUsuario = _foto;
		this.presentacion = _presentacion;
	}
}

export class CursosInit {
	idCurso: number;
	nombreCurso: string;
	profesoresCurso: number[];
	descriccionCorta: string;
	imagenCurso: string;

	constructor(_id: number, _nombre: string, _prof: number[], _desc: string, _imagen: string) {
		this.idCurso = _id;
		this.nombreCurso = _nombre;
		this.profesoresCurso = _prof;
		this.descriccionCorta = _desc;
		this.imagenCurso = _imagen;
	}
}
