export class Clase {
	public id_clase: number;

	public nombre_clase: string;

	public descriccion_clase: string;

	public contenido_clase: string;

	public tipo_clase: number;

	public direccion_clase: string;

	public posicion_clase: number;

	public curso_clase: number;

	constructor(id_clase: number, nombre_clase: string, descriccion_clase: string, contenido_clase: string, tipo_clase: number, direccion_clase: string, posicion_clase: number, curso_clase: number) {
		this.id_clase = id_clase;
		this.nombre_clase = nombre_clase;
		this.descriccion_clase = descriccion_clase;
		this.contenido_clase = contenido_clase;
		this.tipo_clase = tipo_clase;
		this.direccion_clase = direccion_clase;
		this.posicion_clase = posicion_clase;
		this.curso_clase = curso_clase;
	}
}
