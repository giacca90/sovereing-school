export class Clase {
	public idClase: number;

	public nombreClase: string;

	public descripcionClase: string;

	public contenidoClase: string;

	// 0 - ESTATICO, 1 - OBS - 2 - WEBCAM
	public tipoClase: number;

	public direccionClase: string;

	public posicionClase: number;

	public cursoClase: number;

	constructor(idClase: number, nombreClase: string, descripcionClase: string, contenidoClase: string, tipoClase: number, direccionClase: string, posicionClase: number, cursoClase: number) {
		this.idClase = idClase;
		this.nombreClase = nombreClase;
		this.descripcionClase = descripcionClase;
		this.contenidoClase = contenidoClase;
		this.tipoClase = tipoClase;
		this.direccionClase = direccionClase;
		this.posicionClase = posicionClase;
		this.cursoClase = cursoClase;
	}
}
