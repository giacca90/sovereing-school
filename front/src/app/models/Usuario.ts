import { Curso } from './Curso';
import { Plan } from './Plan';

export class Usuario {
	public idUsuario: number;

	public nombreUsuario: string;

	public fotoUsuario: string[];

	public presentacion: string;

	public rollUsuario?: string;

	public planUsuario?: Plan;

	public cursosUsuario?: Curso[];

	public fechaRegistroUsuario?: Date;

	constructor(_idUsuario: number, _nombreUsuario: string, _fotoUsuario: string[], _presentacion: string, _rollUsuario?: string, _planUsuario?: Plan, _cursosUsuario?: Curso[], _fechaRegistroUsuario?: Date) {
		this.idUsuario = _idUsuario;
		this.nombreUsuario = _nombreUsuario;
		this.fotoUsuario = _fotoUsuario;
		this.presentacion = _presentacion;
		this.rollUsuario = _rollUsuario;
		this.planUsuario = _planUsuario;
		this.cursosUsuario = _cursosUsuario;
		this.fechaRegistroUsuario = _fechaRegistroUsuario;
	}
}
