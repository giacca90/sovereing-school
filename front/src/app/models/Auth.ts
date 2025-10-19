import { Usuario } from './Usuario';

export class Auth {
	public status: boolean;
	public message: string;
	public usuario: Usuario;
	public accessToken: string;

	constructor(status: boolean, message: string, usuario: Usuario, accessToken: string) {
		this.status = status;
		this.message = message;
		this.usuario = usuario;
		this.accessToken = accessToken;
	}
}
