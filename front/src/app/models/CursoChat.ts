import { ClaseChat } from './ClaseChat';
import { MensajeChat } from './MensajeChat';

export class CursoChat {
	public idCurso: number;

	public clases: ClaseChat[];

	public mensajes: MensajeChat[];

	public nombreCurso: string;

	public fotoCurso: string;

	constructor(_idCurso: number, _clases: ClaseChat[], _mensajes: MensajeChat[], _nombreCurso: string, _fotoCurso: string) {
		this.idCurso = _idCurso;
		this.clases = _clases;
		this.mensajes = _mensajes;
		this.nombreCurso = _nombreCurso;
		this.fotoCurso = _fotoCurso;
	}
}
