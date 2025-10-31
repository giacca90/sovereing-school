export class Clase {
	public idClase: number;

	public nombreClase: string;

	public descriccionClase: string;

	public contenidoClase: string;

	public tipoClase: number;

	public direccionClase: string;

	public posicionClase: number;

	public cursoClase: number;

	constructor(idClase: number, nombreClase: string, descriccionClase: string, contenidoClase: string, tipoClase: number, direccionClase: string, posicionClase: number, cursoClase: number) {
		this.idClase = idClase;
		this.nombreClase = nombreClase;
		this.descriccionClase = descriccionClase;
		this.contenidoClase = contenidoClase;
		this.tipoClase = tipoClase;
		this.direccionClase = direccionClase;
		this.posicionClase = posicionClase;
		this.cursoClase = cursoClase;
	}
}
