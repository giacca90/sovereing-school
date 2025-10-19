import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
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
	styleUrl: './perfil-usuario.component.css',
})
export class PerfilUsuarioComponent implements OnDestroy {
	editable: boolean = false;
	usuario: Usuario | null = null;
	fotos: Map<string, File> = new Map();
	private readonly subscription: Subscription = new Subscription();

	constructor(
		private readonly loginService: LoginService,
		private readonly usuarioService: UsuariosService,
		private readonly initService: InitService,
	) {
		this.usuario = JSON.parse(JSON.stringify(this.loginService.usuario));
	}

	cargaFoto(event: Event) {
		const input = event.target as HTMLInputElement;
		if (!input.files) {
			return;
		}
		if (this.usuario?.foto_usuario[0].startsWith('#')) {
			this.usuario.foto_usuario = [];
		}
		// Procesa cada archivo seleccionado
		Array.from(input.files).forEach((file, index) => {
			// Genera una URL temporal para previsualizar el archivo
			const objectURL = URL.createObjectURL(file);
			if (index === 0 && this.usuario?.foto_usuario.length === 0) {
				(document.getElementById('fotoPrincipal') as HTMLImageElement).src = objectURL;
			}
			this.fotos.set(objectURL, file);
			this.usuario?.foto_usuario.push(objectURL); // Guarda la URL temporal para previsualizar
		});
	}

	async save() {
		// Verifica si hay cambios en el usuario o la foto principal
		if (JSON.stringify(this.usuario) !== JSON.stringify(this.loginService.usuario) || (document.getElementById('fotoPrincipal') as HTMLImageElement).src !== this.usuario?.foto_usuario[0]) {
			const savePromises: Promise<void>[] = []; // Almacena las promesas de guardado
			// Si hay fotos para procesar
			if (this.fotos.size > 0) {
				const fotoPrincipal: string = (document.getElementById('fotoPrincipal') as HTMLImageElement).src;
				this.usuario?.foto_usuario.forEach((foto, index) => {
					if (foto.startsWith('blob:')) {
						const formData = new FormData();
						const file = this.fotos.get(foto);
						if (file !== undefined) {
							formData.append('files', file as Blob, file.name);
							// Convierte la suscripción a una promesa y la almacena en savePromises
							const savePromise = lastValueFrom(this.usuarioService.save(formData))
								.then((response) => {
									if (this.usuario?.foto_usuario && response) {
										// Actualiza la foto en la posición correcta
										this.usuario.foto_usuario[index] = response[0];
										if (fotoPrincipal === foto) {
											(document.getElementById('fotoPrincipal') as HTMLImageElement).src = response[0];
										}
									}
								})
								.catch((e) => {
									console.error('Error en save() ' + e.message);
								});

							savePromises.push(savePromise);
						}
					}
				});
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

	actualizaUsuario() {
		const temp: Usuario = JSON.parse(JSON.stringify(this.loginService.usuario));
		if (this.usuario?.foto_usuario && this.loginService.usuario?.foto_usuario !== undefined) {
			let fotoPrincipal: string = (document.getElementById('fotoPrincipal') as HTMLImageElement).src;
			if (fotoPrincipal === undefined) {
				fotoPrincipal = this.usuario.foto_usuario[0];
			}
			if (fotoPrincipal !== this.usuario.foto_usuario[0]) {
				const f: string[] = [];
				f.push(fotoPrincipal);
				this.usuario.foto_usuario.forEach((foto: string) => {
					if (foto !== fotoPrincipal && !foto.startsWith('#')) {
						f.push(foto);
					}
				});
				this.usuario.foto_usuario = f;
			}
			temp.foto_usuario = this.usuario.foto_usuario;
			temp.nombre_usuario = this.usuario.nombre_usuario;
			temp.presentacion = this.usuario.presentacion;
			temp.cursos_usuario?.forEach((curso) => {
				curso.clases_curso = undefined;
				curso.planes_curso = undefined;
				curso.precio_curso = undefined;
				curso.profesores_curso.forEach((profe) => {
					profe.fecha_registro_usuario = undefined;
					profe.cursos_usuario = undefined;
					profe.plan_usuario = undefined;
					profe.roll_usuario = undefined;
					if (profe.id_usuario === this.usuario?.id_usuario) {
						profe.foto_usuario = temp.foto_usuario;
					}
				});
			});

			if (temp.plan_usuario?.nombre_plan) temp.plan_usuario.nombre_plan = undefined;
			if (temp.plan_usuario?.precio_plan) temp.plan_usuario.precio_plan = undefined;
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

	cambiaFoto(index: number) {
		if (this.usuario?.foto_usuario) {
			(document.getElementById('fotoPrincipal') as HTMLImageElement).src = this.usuario.foto_usuario[index];
			if (this.editable) {
				for (let i = 0; i < this.usuario?.foto_usuario.length; i++) {
					document.getElementById('foto-' + i)?.classList.remove('border', 'border-black');
				}
				(document.getElementById('foto-' + index) as HTMLImageElement).classList.add('border', 'border-black');
				(document.getElementById('foto-' + index) as HTMLImageElement).classList.add();
			}
		}
	}
	ngOnDestroy(): void {
		this.subscription.unsubscribe();
	}

	generateRandomColor(): string {
		return (
			'#' +
			Math.floor(Math.random() * 16777215)
				.toString(16)
				.padStart(6, '0')
		);
	}
}
