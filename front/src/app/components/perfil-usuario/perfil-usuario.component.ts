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
		this.usuario = structuredClone(this.loginService.usuario);
	}

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
			if (index === 0 && this.usuario?.fotoUsuario.length === 0) {
				(document.getElementById('fotoPrincipal') as HTMLImageElement).src = objectURL;
			}
			this.fotos.set(objectURL, file);
			this.usuario?.fotoUsuario.push(objectURL); // Guarda la URL temporal para previsualizar
		});
	}

	async save() {
		// Verifica si hay cambios en el usuario o la foto principal
		if (JSON.stringify(this.usuario) !== JSON.stringify(this.loginService.usuario) || (document.getElementById('fotoPrincipal') as HTMLImageElement).src !== this.usuario?.fotoUsuario[0]) {
			const savePromises: Promise<void>[] = []; // Almacena las promesas de guardado
			// Si hay fotos para procesar
			if (this.fotos.size > 0) {
				const fotoPrincipal: string = (document.getElementById('fotoPrincipal') as HTMLImageElement).src;
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

	actualizaUsuario() {
		const temp: Usuario | null = structuredClone(this.loginService.usuario);
		if (!temp) return;
		if (this.usuario?.fotoUsuario && this.loginService.usuario?.fotoUsuario !== undefined) {
			let fotoPrincipal: string = (document.getElementById('fotoPrincipal') as HTMLImageElement).src;
			fotoPrincipal ??= this.usuario.fotoUsuario[0];
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

	cambiaFoto(index: number) {
		if (this.usuario?.fotoUsuario) {
			(document.getElementById('fotoPrincipal') as HTMLImageElement).src = this.usuario.fotoUsuario[index];
			if (this.editable) {
				for (let i = 0; i < this.usuario?.fotoUsuario.length; i++) {
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
