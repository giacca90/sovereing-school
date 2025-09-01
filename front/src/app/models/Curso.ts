import { Clase } from './Clase';
import { Plan } from './Plan';
import { Usuario } from './Usuario';

export class Curso {
	public id_curso: number;

	public nombre_curso: string;

	public profesores_curso: Usuario[];

	public fecha_publicacion_curso?: Date;

	public clases_curso?: Clase[];

	public planes_curso?: Plan[];

	public descriccion_corta: string;

	public descriccion_larga?: string;

	public imagen_curso: string;

	public precio_curso?: number;

	constructor(_id_curso: number, _nombre_curso: string, _profesores_curso: Usuario[], _descriccion_corta: string, _imagen_curso: string, _fecha_publicacion_curso?: Date, _clases_curso?: Clase[], _planes_curso?: Plan[], _descriccion_larga?: string, _precio_curso?: number) {
		this.id_curso = _id_curso;
		this.nombre_curso = _nombre_curso;
		this.profesores_curso = _profesores_curso;
		this.fecha_publicacion_curso = _fecha_publicacion_curso;
		this.clases_curso = _clases_curso;
		this.planes_curso = _planes_curso;
		this.descriccion_corta = _descriccion_corta;
		this.descriccion_larga = _descriccion_larga;
		this.imagen_curso = _imagen_curso;
		this.precio_curso = _precio_curso;
	}
}
