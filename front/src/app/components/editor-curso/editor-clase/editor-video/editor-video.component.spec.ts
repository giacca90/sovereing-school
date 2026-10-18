import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EditorVideoComponent } from './editor-video.component';
import { ElementRef } from '@angular/core';
import { Clase } from '../../../../models/Clase';

describe('EditorVideoComponent', () => {
	let component: EditorVideoComponent;
	let fixture: ComponentFixture<EditorVideoComponent>;

	const createDummyClase = (): Clase =>
		new Clase(1, 'Clase Video', 'Desc', 'video.mp4', 10, 'Notas', 0, 1);

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [EditorVideoComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(EditorVideoComponent);
		component = fixture.componentInstance;
		component.clase = createDummyClase();
	});

	it('should create the component', () => {
		// Arrange & Act
		fixture.detectChanges();

		// Assert
		expect(component).toBeTruthy();
	});

	describe('ngAfterViewInit', () => {
		it('should return early when clase has no direccionClase', () => {
			// Arrange
			component.clase.direccionClase = '';
			spyOn(console, 'log');

			// Act
			component.ngAfterViewInit();

			// Assert
			expect(console.log).not.toHaveBeenCalledWith(jasmine.stringMatching('📡 Cargando video desde:'));
		});

		it('should log error when videoPlayer nativeElement is missing', () => {
			// Arrange
			component.clase.direccionClase = 'test.mp4';
			component.videoPlayerRef = { nativeElement: null } as any;
			spyOn(console, 'error');

			// Act
			component.ngAfterViewInit();

			// Assert
			expect(console.error).toHaveBeenCalledWith('No se pudo obtener el elemento videoPlayer');
		});
	});

	describe('cargaVideo', () => {
		it('should alert and return if no files are selected', () => {
			// Arrange
			spyOn(window, 'alert');
			const event = { target: { files: [] } } as unknown as Event;

			// Act
			component.cargaVideo(event);

			// Assert
			expect(window.alert).toHaveBeenCalledWith('¡Sube un video válido!');
		});

		it('should read file, set video src and emit file when valid file selected', (done) => {
			// Arrange
			const dummyBlob = new Blob(['sample-video-content'], { type: 'video/mp4' });
			const dummyFile = new File([dummyBlob], 'sample.mp4', { type: 'video/mp4' });
			const videoEl = document.createElement('video');
			component.videoPlayerRef = new ElementRef(videoEl);

			component.videoSeleccionado.subscribe((file: File) => {
				// Assert
				expect(file).toBe(dummyFile);
				expect(videoEl.src).toContain('data:video/mp4');
				done();
			});

			const event = { target: { files: [dummyFile] } } as unknown as Event;

			// Act
			component.cargaVideo(event);
		});
	});

	describe('keyEvent', () => {
		it('should trigger input click when Enter key is pressed', () => {
			// Arrange
			const mockInput = document.createElement('input');
			spyOn(mockInput, 'click');
			component.videoUploadInputRef = new ElementRef(mockInput);

			// Act
			component.keyEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

			// Assert
			expect(mockInput.click).toHaveBeenCalled();
		});

		it('should not trigger input click on other keys', () => {
			// Arrange
			const mockInput = document.createElement('input');
			spyOn(mockInput, 'click');
			component.videoUploadInputRef = new ElementRef(mockInput);

			// Act
			component.keyEvent(new KeyboardEvent('keydown', { key: 'Space' }));

			// Assert
			expect(mockInput.click).not.toHaveBeenCalled();
		});
	});
});
