import { CommonModule } from '@angular/common';
import { Component, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { lastValueFrom, Subscription } from 'rxjs';
import { Usuario } from '../../models/Usuario';
import { InitService } from '../../services/init.service';
import { LoginService } from '../../services/login.service';
import { UsuariosService } from '../../services/usuarios.service';

@Component({
	selector: 'app-perfil-usuario',
	standalone: true,
	imports: [FormsModule, CommonModule],
	templateUrl: './perfil-usuario.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './perfil-usuario.component.css',
})
export class PerfilUsuarioComponent implements OnDestroy {
	editable: boolean = false;
	usuario: Usuario | null = null;
	fotos: Map<string, File> = new Map();
	selectedFotoIndex: number = 0;
	private readonly subscription: Subscription = new Subscription();

	/**
	 * Constructor del componente.
	 * @param {LoginService} loginService - Servicio de autenticación.
	 * @param {UsuariosService} usuarioService - Servicio de usuarios.
	 * @param {InitService} initService - Servicio de inicialización.
	 */
	constructor(
		private readonly loginService: LoginService,
		private readonly usuarioService: UsuariosService,
		private readonly initService: InitService,
	) {
		this.usuario = structuredClone(this.loginService.usuario);
	}

	/**
	 * Carga una nueva foto de perfil.
	 * @param {Event} event - Evento de selección de archivo.
	 */
	cargaFoto(event: Event) {
		const input = event.target as HTMLInputElement;
		if (!input.files) {
			return;
		}
		if (this.usuario?.fotoUsuario[0].startsWith('#')) {
			this.usuario.fotoUsuario = [];
		}
		// Procesa cada archivo seleccionado
		Array.from(input.files).forEach((file, index) => {
			// Genera una URL temporal para previsualizar el archivo
			const objectURL = URL.createObjectURL(file);
			this.fotos.set(objectURL, file);
			this.usuario?.fotoUsuario.push(objectURL); // Guarda la URL temporal para previsualizar
		});
	}

	/**
	 * Guarda los cambios realizados en el perfil del usuario.
	 */
	async save() {
		// Verifica si hay cambios en el usuario o la foto principal
		const fotoPrincipalUrl = this.usuario?.fotoUsuario[this.selectedFotoIndex];
		if (JSON.stringify(this.usuario) !== JSON.stringify(this.loginService.usuario) || fotoPrincipalUrl !== this.loginService.usuario?.fotoUsuario[0]) {
			const savePromises: Promise<void>[] = []; // Almacena las promesas de guardado
			// Si hay fotos para procesar
			if (this.fotos.size > 0) {
				let index = 0;
				for (const foto of this.usuario?.fotoUsuario || []) {
					if (foto.startsWith('blob:')) {
						const formData = new FormData();
						const file = this.fotos.get(foto);
						if (file !== undefined) {
							formData.append('files', file as Blob, file.name);
							// Convierte la suscripción a una promesa y la almacena en savePromises
							const savePromise = lastValueFrom(this.usuarioService.save(formData))
								.then((response) => {
									if (this.usuario?.fotoUsuario && response) {
										// Actualiza la foto en la posición correcta
										this.usuario.fotoUsuario[index] = response[0];
									}
								})
								.catch((e) => {
									console.error('Error en save() ' + e.message);
								});

							savePromises.push(savePromise);
						}
					}
					index++;
				}
			}

			// Espera a que todas las promesas se resuelvan antes de continuar
			try {
				await Promise.all(savePromises);
				this.actualizaUsuario(); // Ejecuta la actualización del usuario solo cuando todo haya terminado
			} catch (error) {
				console.error('Error en save():', error);
			}
		}
	}

	/**
	 * Envía la actualización del usuario al servidor.
	 */
	actualizaUsuario() {
		const temp: Usuario | null = structuredClone(this.loginService.usuario);
		if (!temp) return;
		if (this.usuario?.fotoUsuario && this.loginService.usuario?.fotoUsuario !== undefined) {
			let fotoPrincipal: string = this.usuario.fotoUsuario[this.selectedFotoIndex];
			if (fotoPrincipal !== this.usuario.fotoUsuario[0]) {
				const f: string[] = [];
				f.push(fotoPrincipal);
				for (const foto of this.usuario.fotoUsuario) {
					if (foto !== fotoPrincipal && !foto.startsWith('#')) {
						f.push(foto);
					}
				}
				this.usuario.fotoUsuario = f;
			}
			temp.fotoUsuario = this.usuario.fotoUsuario;
			temp.nombreUsuario = this.usuario.nombreUsuario;
			temp.presentacion = this.usuario.presentacion;

			if (temp.planUsuario?.nombrePlan) temp.planUsuario.nombrePlan = undefined;
			if (temp.planUsuario?.precioPlan) temp.planUsuario.precioPlan = undefined;
			this.subscription.add(
				this.usuarioService.actualizaUsuario(temp).subscribe({
					next: () => {
						this.initService.carga();
					},
					error: (e: Error) => {
						console.error('Error en actualizar usuario: ' + e.message);
					},
				}),
			);
		}
	}

	/**
	 * Cambia la foto de perfil seleccionada.
	 * @param {number} index - Índice de la foto.
	 */
	cambiaFoto(index: number) {
		this.selectedFotoIndex = index;
	}

	/**
	 * Limpieza de recursos al destruir el componente.
	 */
	ngOnDestroy(): void {
		this.subscription.unsubscribe();
	}

	/**
	 * Genera un color aleatorio en formato hex.
	 * @returns {string} Color aleatorio.
	 */
	generateRandomColor(): string {
		return (
			'#' +
			Math.floor(Math.random() * 16777215)
				.toString(16)
				.padStart(6, '0')
		);
	}
}
