import { Curso } from './Curso';
import { Plan } from './Plan';

export class NuevoUsuario {
	public nombreUsuario: string | null = null;

	public correoElectronico: string | null = null;

	public password: string | null = null;

	public fotoUsuario: string[] | null = null;

	public planUsuario: Plan | null = null;

	public cursosUsuario: Curso[] | null = null;

	public fechaRegistroUsuario: Date | null = null;
}
