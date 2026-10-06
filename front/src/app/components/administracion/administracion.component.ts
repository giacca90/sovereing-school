import { ChangeDetectorRef, Component, ChangeDetectionStrategy } from '@angular/core';
import { Curso } from '../../models/Curso';
import { CursoChat } from '../../models/CursoChat';
import { Usuario } from '../../models/Usuario';
import { ChatService } from '../../services/chat.service';
import { CursosService } from '../../services/cursos.service';
import { InitService } from '../../services/init.service';
import { UsuariosService } from '../../services/usuarios.service';

@Component({
	selector: 'app-administracion',
	imports: [],
	templateUrl: './administracion.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './administracion.component.css',
})
export class AdministracionComponent {
	tipo: number = 0; // 0: Vacío  1: Usuarios  2: Cursos  3: Chats
	usuarios: Usuario[] = [];
	usuariosSel: Usuario[] = [];

	cursos: Curso[] = [];
	cursosSel: Curso[] = [];

	chats: CursoChat[] = [];
	chatsSel: CursoChat[] = [];

	usuarioVisible: number | null = null;
	cursoVisible: number | null = null;
	chatVisible: number | null = null;
	claseChatVisible: number | null = null;

	/**
	 * Constructor del componente.
	 * @param {UsuariosService} usuariosService - Servicio de usuarios.
	 * @param {CursosService} cursosService - Servicio de cursos.
	 * @param {ChatService} chatsService - Servicio de chat.
	 * @param {InitService} initService - Servicio de inicialización.
	 */
	constructor(
		private readonly usuariosService: UsuariosService,
		public cursosService: CursosService,
		private readonly chatsService: ChatService,
		private readonly initService: InitService,
		private cdr: ChangeDetectorRef,
	) {}

	/**
	 * Carga todos los usuarios para la vista de administración.
	 */
	cargaUsuarios() {
		if (this.usuarios.length === 0) {
			this.usuariosService.getAllUsuarios().subscribe((data: Usuario[] | null) => {
				if (data) {
					this.usuarios = data;
					this.usuariosSel = data;
					this.cdr.detectChanges();
				}
			});
		}
		this.tipo = 1;
	}

	/**
	 * Carga todos los cursos para la vista de administración.
	 */
	cargaCursos() {
		if (this.cursos.length === 0) {
			this.cursosService.getAllCursos().subscribe((data: Curso[] | null) => {
				if (data) {
					this.cursos = data;
					this.cursosSel = data;
					this.cdr.detectChanges();
				}
			});
		}
		this.tipo = 2;
	}

	/**
	 * Carga todos los chats para la vista de administración.
	 */
	cargaChats() {
		if (this.chats.length === 0) {
			this.chatsService.getAllChats().subscribe((data: CursoChat[] | null) => {
				if (data) {
					this.chats = data;
					this.chatsSel = data;
					this.cdr.detectChanges();
				}
			});
		}
		this.tipo = 3;
	}

	/**
	 * Filtra la lista de usuarios.
	 * @param {Event} $event - Evento de entrada de búsqueda.
	 */
	buscaUsuarios($event: Event) {
		const value: string = ($event.target as HTMLInputElement).value;
		if (value.length === 0) {
			this.usuariosSel = this.usuarios;
		} else {
			this.usuariosSel = this.usuarios.filter((u) => u.nombreUsuario.toLowerCase().includes(value.toLowerCase()) || u.rollUsuario?.toLowerCase().includes(value.toLowerCase()) || u.idUsuario.toString().includes(value));
		}
		this.cdr.detectChanges();
	}

	/**
	 * Filtra la lista de cursos.
	 * @param {Event} $event - Evento de entrada de búsqueda.
	 */
	buscaCursos($event: Event) {
		const value: string = ($event.target as HTMLInputElement).value;
		if (value.length === 0) {
			this.cursosSel = this.cursos;
		} else {
			this.cursosSel = this.cursos.filter((c) => c.nombreCurso.toLowerCase().includes(value.toLowerCase()) || c.profesoresCurso.toString().toLowerCase().includes(value.toLowerCase()) || c.idCurso.toString().includes(value));
		}
		this.cdr.detectChanges();
	}

	/**
	 * Filtra la lista de chats.
	 * @param {Event} $event - Evento de entrada de búsqueda.
	 */
	buscaChats($event: Event) {
		const value: string = ($event.target as HTMLInputElement).value;
		if (value.length === 0) {
			this.chatsSel = this.chats;
		} else {
			this.chatsSel = this.chats.filter((c) => c.nombreCurso.toLowerCase().includes(value.toLowerCase()) || c.idCurso.toString().includes(value));
		}
		this.cdr.detectChanges();
	}

	/**
	 * Elimina un usuario.
	 * @param {Usuario} usuario - Usuario a eliminar.
	 */
	eliminaUsuario(usuario: Usuario) {
		if (!confirm('¿Estás seguro que deseas eliminar este usuario?')) {
			return;
		}
		this.usuariosService.eliminaUsuario(usuario).subscribe((data: boolean) => {
			if (data) {
				this.usuariosService.getAllUsuarios().subscribe((data: Usuario[] | null) => {
					if (data) {
						this.usuarios = data;
						this.usuariosSel = data;
					}
				});
			}
			this.cdr.detectChanges();
		});
	}

	/**
	 * Elimina un curso.
	 * @param {Curso} curso - Curso a eliminar.
	 */
	eliminaCurso(curso: Curso) {
		if (!confirm('¿Estás seguro que deseas eliminar este curso?\n Esto eliminará también el chat de este curso')) {
			return;
		}
		this.cursosService.deleteCurso(curso).subscribe((data: boolean) => {
			if (data) {
				this.cursosService.getAllCursos().subscribe((data: Curso[] | null) => {
					if (data) {
						this.cursos = data;
						this.cursosSel = data;
					}
				});
			}
			this.cdr.detectChanges();
		});
	}

	/**
	 * Elimina un chat.
	 * @param {CursoChat} chat - Chat a eliminar.
	 */
	eliminaChat(chat: CursoChat) {
		if (!confirm('¿Estás seguro que deseas eliminar este chat?\n Esto no eliminará el curso de este chat')) {
			return;
		}
		this.chatsService.deleteChat(chat).subscribe((data: boolean) => {
			if (data) {
				this.chatsService.getAllChats().subscribe((data: CursoChat[] | null) => {
					if (data) {
						this.chats = data;
						this.chatsSel = data;
					}
				});
			}
			this.cdr.detectChanges();
		});
	}

	/**
	 * Alterna la visibilidad de los detalles de un usuario.
	 * @param {number} idUsuario - ID del usuario.
	 */
	mostrarUsuario(idUsuario: number) {
		this.usuarioVisible = this.usuarioVisible === idUsuario ? null : idUsuario;
	}

	/**
	 * Alterna la visibilidad de los detalles de un curso.
	 * @param {number} idCurso - ID del curso.
	 */
	mostrarCurso(idCurso: number) {
		this.cursoVisible = this.cursoVisible === idCurso ? null : idCurso;
	}

	/**
	 * Alterna la visibilidad de los detalles de un chat.
	 * @param {number} idCurso - ID del curso del chat.
	 */
	mostrarChat(idCurso: number) {
		this.chatVisible = this.chatVisible === idCurso ? null : idCurso;
	}

	/**
	 * Alterna la visibilidad de los detalles de una clase de chat.
	 * @param {number} idClase - ID de la clase.
	 */
	mostrarClaseChat(idClase: number) {
		this.claseChatVisible = this.claseChatVisible === idClase ? null : idClase;
	}

	/**
	 * Obtiene la foto del profesor.
	 * @param {number} idProfe - ID del profesor.
	 * @returns {string | undefined} URL de la foto o undefined.
	 */
	getProfessorPhoto(idProfe: number) {
		return this.usuariosService.profes.find((profe) => profe.idUsuario === idProfe)?.fotoUsuario[0];
	}
	/**
	 * Obtiene el nombre del profesor.
	 * @param {number} idProfe - ID del profesor.
	 * @returns {string | undefined} Nombre del profesor o undefined.
	 */
	getProfessorName(idProfe: number) {
		return this.usuariosService.profes.find((profe) => profe.idUsuario === idProfe)?.nombreUsuario;
	}
}
