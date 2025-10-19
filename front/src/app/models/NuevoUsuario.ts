import { Curso } from './Curso';
import { Plan } from './Plan';

export class NuevoUsuario {
	public nombre_usuario: string | null = null;

	public correo_electronico: string | null = null;

	public password: string | null = null;

	public foto_usuario: string[] | null = null;

	public plan_usuario: Plan | null = null;

	public cursos_usuario: Curso[] | null = null;

	public fecha_registro_usuario: Date | null = null;
}
