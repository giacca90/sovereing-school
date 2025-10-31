import { Clase } from './Clase';
import { Plan } from './Plan';
import { Usuario } from './Usuario';

export class Curso {
	public idCurso: number;

	public nombreCurso: string;

	public profesoresCurso: Usuario[];

	public fechaPublicacionCurso?: Date;

	public clasesCurso?: Clase[];

	public planesCurso?: Plan[];

	public descriccionCorta: string;

	public descriccionLarga?: string;

	public imagenCurso: string;

	public precioCurso?: number;

	constructor(_idCurso: number, _nombreCurso: string, _profesoresCurso: Usuario[], _descriccionCorta: string, _imagenCurso: string, _fechaPublicacionCurso?: Date, _clasesCurso?: Clase[], _planesCurso?: Plan[], _descriccionLarga?: string, _precioCurso?: number) {
		this.idCurso = _idCurso;
		this.nombreCurso = _nombreCurso;
		this.profesoresCurso = _profesoresCurso;
		this.fechaPublicacionCurso = _fechaPublicacionCurso;
		this.clasesCurso = _clasesCurso;
		this.planesCurso = _planesCurso;
		this.descriccionCorta = _descriccionCorta;
		this.descriccionLarga = _descriccionLarga;
		this.imagenCurso = _imagenCurso;
		this.precioCurso = _precioCurso;
	}
}
