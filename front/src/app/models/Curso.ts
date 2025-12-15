import { Clase } from './Clase';

export class Curso {
	public idCurso: number;

	public nombreCurso: string;

	public profesoresCurso: number[];

	public fechaPublicacionCurso?: Date;

	public clasesCurso?: Clase[];

	public planesCurso?: number[];

	public descripcionCorta: string;

	public descripcionLarga?: string;

	public imagenCurso: string;

	public precioCurso?: number;

	constructor(_idCurso: number, _nombreCurso: string, _profesoresCurso: number[], _descripcionCorta: string, _imagenCurso: string, _fechaPublicacionCurso?: Date, _clasesCurso?: Clase[], _planesCurso?: number[], _descripcionLarga?: string, _precioCurso?: number) {
		this.idCurso = _idCurso;
		this.nombreCurso = _nombreCurso;
		this.profesoresCurso = _profesoresCurso;
		this.fechaPublicacionCurso = _fechaPublicacionCurso;
		this.clasesCurso = _clasesCurso;
		this.planesCurso = _planesCurso;
		this.descripcionCorta = _descripcionCorta;
		this.descripcionLarga = _descripcionLarga;
		this.imagenCurso = _imagenCurso;
		this.precioCurso = _precioCurso;
	}
}
