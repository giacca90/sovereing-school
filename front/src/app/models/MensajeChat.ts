export class MensajeChat {
	public idMensaje: string | null;
	public idCurso: number | null;
	public idClase: number | null;
	public idUsuario: number | undefined;

	public nombreCurso: string | null;
	public nombreClase: string | null;
	public nombreUsuario: string | null;

	public fotoCurso: string | null;
	public fotoUsuario: string | null;

	public respuesta: MensajeChat | null;
	public pregunta: number | null;

	public mensaje: string | null;

	public fecha: Date | null;

	constructor(_idMensaje: string | null, _idCurso: number | null, _idClase: number | null, _idUsuario: number | undefined, _nombreCurso: string | null, _nombreClase: string | null, _nombreUsuario: string | null, _fotoCurso: string | null, _fotoUsuario: string | null, _respuesta: MensajeChat | null, _pregunta: number | null, _mensaje: string | null, _fecha: Date | null) {
		this.idMensaje = _idMensaje;
		this.idCurso = _idCurso;
		this.idClase = _idClase;
		this.idUsuario = _idUsuario;
		this.nombreCurso = _nombreCurso;
		this.nombreClase = _nombreClase;
		this.nombreUsuario = _nombreUsuario;
		this.fotoCurso = _fotoCurso;
		this.fotoUsuario = _fotoUsuario;
		this.respuesta = _respuesta;
		this.pregunta = _pregunta;
		this.mensaje = _mensaje;
		this.fecha = _fecha;
	}
}
