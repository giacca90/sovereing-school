import { MensajeChat } from './MensajeChat';

export class ClaseChat {
	public idClase: number;

	public idCurso: number;

	public nombreClase: string;

	public mensajes: MensajeChat[];

	constructor(_idClase: number, _idCurso: number, _nombreClase: string, _mensajes: MensajeChat[]) {
		this.idClase = _idClase;
		this.idCurso = _idCurso;
		this.nombreClase = _nombreClase;
		this.mensajes = _mensajes;
	}
}
