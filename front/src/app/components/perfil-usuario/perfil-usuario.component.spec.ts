import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Plan } from '../../models/Plan';
import { Usuario } from '../../models/Usuario';
import { InitService } from '../../services/init.service';
import { LoginService } from '../../services/login.service';
import { UsuariosService } from '../../services/usuarios.service';
import { PerfilUsuarioComponent } from './perfil-usuario.component';

describe('PerfilUsuarioComponent', () => {
	let component: PerfilUsuarioComponent;
	let fixture: ComponentFixture<PerfilUsuarioComponent>;
	let mockLoginService: { usuario: Usuario | null };
	let mockUsuariosService: {
		save: jasmine.Spy;
		actualizaUsuario: jasmine.Spy;
	};
	let mockInitService: {
		carga: jasmine.Spy;
	};

	let dummyUsuario: Usuario;

	beforeEach(async () => {
		dummyUsuario = new Usuario(1, 'TestUser', ['https://example.com/avatar.jpg'], 'Bio de prueba', 'USER', new Plan(1, 'Pro', 10), [], new Date());

		mockLoginService = {
			usuario: structuredClone(dummyUsuario),
		};

		mockUsuariosService = {
			save: jasmine.createSpy('save').and.returnValue(of(['https://cdn.example.com/uploaded.jpg'])),
			actualizaUsuario: jasmine.createSpy('actualizaUsuario').and.returnValue(of({})),
		};

		mockInitService = {
			carga: jasmine.createSpy('carga'),
		};

		await TestBed.configureTestingModule({
			imports: [PerfilUsuarioComponent],
			providers: [
				{ provide: LoginService, useValue: mockLoginService },
				{ provide: UsuariosService, useValue: mockUsuariosService },
				{ provide: InitService, useValue: mockInitService },
			],
		}).compileComponents();

		fixture = TestBed.createComponent(PerfilUsuarioComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create the component and clone loginService user', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
		expect(component.usuario).toEqual(mockLoginService.usuario);
		expect(component.editable).toBeFalse();
		expect(component.selectedFotoIndex).toBe(0);
	});

	describe('cargaFoto', () => {
		it('should do nothing if input has no files', () => {
			// Arrange
			const mockEvent = {
				target: { files: null },
			} as unknown as Event;

			// Act
			component.cargaFoto(mockEvent);

			// Assert
			expect(component.fotos.size).toBe(0);
		});

		it('should reset fotoUsuario if first element starts with # (color placeholder)', () => {
			// Arrange
			component.usuario!.fotoUsuario = ['#ff5733'];
			const dummyFile = new File(['content'], 'avatar.png', { type: 'image/png' });
			const mockFileList = [dummyFile];
			const mockEvent = {
				target: { files: mockFileList },
			} as unknown as Event;

			spyOn(URL, 'createObjectURL').and.returnValue('blob:url-preview-1');

			// Act
			component.cargaFoto(mockEvent);

			// Assert
			expect(URL.createObjectURL).toHaveBeenCalledWith(dummyFile);
			expect(component.fotos.get('blob:url-preview-1')).toBe(dummyFile);
			expect(component.usuario!.fotoUsuario).toEqual(['blob:url-preview-1']);
		});

		it('should append files to existing fotoUsuario if not starting with #', () => {
			// Arrange
			component.usuario!.fotoUsuario = ['https://existing.png'];
			const dummyFile = new File(['content'], 'avatar2.png', { type: 'image/png' });
			const mockFileList = [dummyFile];
			const mockEvent = {
				target: { files: mockFileList },
			} as unknown as Event;

			spyOn(URL, 'createObjectURL').and.returnValue('blob:url-preview-2');

			// Act
			component.cargaFoto(mockEvent);

			// Assert
			expect(component.usuario!.fotoUsuario).toEqual(['https://existing.png', 'blob:url-preview-2']);
			expect(component.fotos.get('blob:url-preview-2')).toBe(dummyFile);
		});
	});

	describe('cambiaFoto', () => {
		it('should update selectedFotoIndex', () => {
			// Arrange & Act
			component.cambiaFoto(3);

			// Assert
			expect(component.selectedFotoIndex).toBe(3);
		});
	});

	describe('save', () => {
		it('should not perform save if there are no changes', async () => {
			// Arrange
			spyOn(component, 'actualizaUsuario');

			// Act
			await component.save();

			// Assert
			expect(mockUsuariosService.save).not.toHaveBeenCalled();
			expect(component.actualizaUsuario).not.toHaveBeenCalled();
		});

		it('should upload blob photos, update fotoUsuario and invoke actualizaUsuario', async () => {
			// Arrange
			const dummyFile = new File(['content'], 'foto.png', { type: 'image/png' });
			component.fotos.set('blob:test-foto', dummyFile);
			component.usuario!.fotoUsuario = ['blob:test-foto'];
			spyOn(component, 'actualizaUsuario');

			// Act
			await component.save();

			// Assert
			expect(mockUsuariosService.save).toHaveBeenCalled();
			expect(component.usuario!.fotoUsuario).toContain('https://cdn.example.com/uploaded.jpg');
			expect(component.actualizaUsuario).toHaveBeenCalled();
		});

		it('should handle photo upload error gracefully', async () => {
			// Arrange
			spyOn(console, 'error');
			mockUsuariosService.save.and.returnValue(throwError(() => new Error('Upload error')));
			const dummyFile = new File(['content'], 'foto.png', { type: 'image/png' });
			component.fotos.set('blob:test-foto', dummyFile);
			component.usuario!.fotoUsuario = ['blob:test-foto'];
			spyOn(component, 'actualizaUsuario');

			// Act
			await component.save();

			// Assert
			expect(console.error).toHaveBeenCalledWith('Error en save() Upload error');
			expect(component.actualizaUsuario).toHaveBeenCalled();
		});

		it('should log error when Promise.all throws', async () => {
			// Arrange
			spyOn(console, 'error');
			spyOn(Promise, 'all').and.rejectWith(new Error('Fatal promise error'));
			component.usuario!.nombreUsuario = 'Modificado'; // Genera cambio

			// Act
			await component.save();

			// Assert
			expect(console.error).toHaveBeenCalledWith('Error en save():', jasmine.any(Error));
		});

		it('should not update fotoUsuario if save response is null/undefined', async () => {
			mockUsuariosService.save.and.returnValue(of(null as any));
			const dummyFile = new File(['content'], 'foto.png', { type: 'image/png' });
			component.fotos.set('blob:test-foto', dummyFile);
			component.usuario!.fotoUsuario = ['blob:test-foto'];
			spyOn(component, 'actualizaUsuario');

			await component.save();
			expect(mockUsuariosService.save).toHaveBeenCalled();
			expect(component.actualizaUsuario).toHaveBeenCalled();
		});
	});

	describe('actualizaUsuario', () => {
		it('should return early if loginService.usuario is null', () => {
			// Arrange
			mockLoginService.usuario = null;

			// Act
			component.actualizaUsuario();

			// Assert
			expect(mockUsuariosService.actualizaUsuario).not.toHaveBeenCalled();
		});

		it('should reorder photos if selectedFotoIndex is not 0', () => {
			// Arrange
			component.usuario!.fotoUsuario = ['https://foto0.jpg', 'https://foto1.jpg', '#colorPlaceholder'];
			component.selectedFotoIndex = 1;

			// Act
			component.actualizaUsuario();

			// Assert
			expect(component.usuario!.fotoUsuario[0]).toBe('https://foto1.jpg');
			expect(component.usuario!.fotoUsuario[1]).toBe('https://foto0.jpg');
			expect(component.usuario!.fotoUsuario.includes('#colorPlaceholder')).toBeFalse();
			expect(mockUsuariosService.actualizaUsuario).toHaveBeenCalled();
			expect(mockInitService.carga).toHaveBeenCalled();
		});

		it('should clear planUsuario nombrePlan and precioPlan before sending update', () => {
			// Arrange
			component.usuario!.planUsuario = new Plan(1, 'Plus', 19.99);

			// Act
			component.actualizaUsuario();

			// Assert
			const sentUser = mockUsuariosService.actualizaUsuario.calls.mostRecent().args[0];
			expect(sentUser.planUsuario.nombrePlan).toBeUndefined();
			expect(sentUser.planUsuario.precioPlan).toBeUndefined();
		});

		it('should handle error when actualizaUsuario fails', () => {
			// Arrange
			spyOn(console, 'error');
			mockUsuariosService.actualizaUsuario.and.returnValue(throwError(() => new Error('Server update failed')));

			// Act
			component.actualizaUsuario();

			// Assert
			expect(console.error).toHaveBeenCalledWith('Error en actualizar usuario: Server update failed');
		});

		it('should not call actualizaUsuario when loginService.usuario.fotoUsuario is undefined', () => {
			mockLoginService.usuario!.fotoUsuario = undefined as any;
			component.usuario!.nombreUsuario = 'Changed';
			component.actualizaUsuario();
			expect(mockUsuariosService.actualizaUsuario).not.toHaveBeenCalled();
		});
	});

	describe('generateRandomColor', () => {
		it('should generate a valid 7-character hex color starting with #', () => {
			// Arrange & Act
			const color = component.generateRandomColor();

			// Assert
			expect(color).toMatch(/^#[0-9a-f]{6}$/i);
		});
	});

	describe('ngOnDestroy', () => {
		it('should unsubscribe all subscriptions on destroy', () => {
			// Arrange
			const subSpy = spyOn((component as any).subscription, 'unsubscribe');

			// Act
			component.ngOnDestroy();

			// Assert
			expect(subSpy).toHaveBeenCalled();
		});
	});
});
