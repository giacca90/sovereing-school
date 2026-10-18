import { ElementRef } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { EditorObsComponent } from './editor-obs.component';

describe('EditorObsComponent', () => {
	let component: EditorObsComponent;
	let fixture: ComponentFixture<EditorObsComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [EditorObsComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(EditorObsComponent);
		component = fixture.componentInstance;
		component.player = {
			dispose: jasmine.createSpy('dispose'),
			on: jasmine.createSpy('on'),
			src: jasmine.createSpy('src'),
			tech: jasmine.createSpy('tech').and.returnValue({
				el: () => ({
					captureStream: () => ({}) as MediaStream,
				}),
			}),
		};
	});

	it('should create the component', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
	});

	describe('ngAfterViewInit', () => {
		it('should call startOBS when isBrowser is true', () => {
			// Arrange
			component.isBrowser = true;
			spyOn(component, 'startOBS');

			// Act
			component.ngAfterViewInit();

			// Assert
			expect(component.startOBS).toHaveBeenCalled();
		});

		it('should not call startOBS when isBrowser is false', () => {
			// Arrange
			component.isBrowser = false;
			spyOn(component, 'startOBS');

			// Act
			component.ngAfterViewInit();

			// Assert
			expect(component.startOBS).not.toHaveBeenCalled();
		});
	});

	describe('ngOnDestroy', () => {
		it('should dispose the video player', () => {
			// Arrange & Act
			component.ngOnDestroy();

			// Assert
			expect(component.player.dispose).toHaveBeenCalled();
		});
	});

	describe('startOBS', () => {
		it('should emit startOBS event and initialize player after timeout', fakeAsync(() => {
			// Arrange
			spyOn(component.obsEvent, 'emit');
			spyOn<any>(component, 'initVideoJS').and.resolveTo();
			spyOn<any>(component, 'initMediaStream');
			const mockVideo = document.createElement('video');
			component.videoOBSRef = new ElementRef(mockVideo);
			component.urlPreview = 'http://localhost/test.m3u8';
			component.rtmpUrl = 'rtmp://live.stream/app/streamKey123';

			// Act
			component.startOBS();

			// Assert
			expect(component.obsEvent.emit).toHaveBeenCalledWith({ type: 'startOBS', message: '' });

			tick(350);
			tick();
			expect(component.rtmpServer).toBe('rtmp://live.stream/app');
			expect(component.rtmpKey).toBe('streamKey123');
		}));
	});

	describe('emiteOBS', () => {
		it('should alert if m3u8Loaded is false', async () => {
			// Arrange
			spyOn(window, 'alert');
			component.m3u8Loaded = false;

			// Act
			await component.emiteOBS();

			// Assert
			expect(window.alert).toHaveBeenCalledWith('Debes conectarte primero con OBS');
		});

		it('should emit emiteOBS event and calculate timer when m3u8 is loaded and rtmpUrl is provided', async () => {
			// Arrange
			component.m3u8Loaded = true;
			component.rtmpUrl = 'rtmp://test/live/key';
			spyOn(component.obsEvent, 'emit');
			spyOn(component, 'calculaTiempoGrabacion');

			// Act
			await component.emiteOBS();

			// Assert
			expect(component.obsEvent.emit).toHaveBeenCalledWith({
				type: 'emiteOBS',
				message: 'rtmp://test/live/key',
			});
			expect(component.calculaTiempoGrabacion).toHaveBeenCalled();
		});

		it('should return without emitting if rtmpUrl is empty', async () => {
			// Arrange
			component.m3u8Loaded = true;
			component.rtmpUrl = '';
			spyOn(component.obsEvent, 'emit');

			// Act
			await component.emiteOBS();

			// Assert
			expect(component.obsEvent.emit).not.toHaveBeenCalled();
		});
	});

	describe('detenerEmision', () => {
		it('should emit stopOBS and clear player source', () => {
			// Arrange
			spyOn(component.obsEvent, 'emit');

			// Act
			component.detenerEmision();

			// Assert
			expect(component.obsEvent.emit).toHaveBeenCalledWith({ type: 'stopOBS', message: '' });
			expect(component.player.src).toBe('');
			expect(component.player.srcObject).toBeNull();
		});
	});

	describe('tooltip and clipboard events', () => {
		it('should copy text to clipboard and update tooltip text', async () => {
			// Arrange
			const writeTextSpy = jasmine.createSpy('writeText').and.resolveTo();
			spyOnProperty(navigator, 'clipboard', 'get').and.returnValue({
				writeText: writeTextSpy,
			} as unknown as Clipboard);

			// Act
			component.copyToClipboard('clave-rtmp');
			await Promise.resolve();

			// Assert
			expect(writeTextSpy).toHaveBeenCalledWith('clave-rtmp');
			expect(component.tooltipText).toBe('Copiado al portapapeles');
		});

		it('should update tooltip coordinates on mouse move', () => {
			// Arrange
			const mouseEvent = new MouseEvent('mousemove', { clientX: 150, clientY: 200 });

			// Act
			component.onMouseMove(mouseEvent);

			// Assert
			expect(component.tooltipPos).toEqual({ x: 150, y: 170 });
		});

		it('should show tooltip on mouse over and hide on mouse leave', () => {
			// Arrange & Act
			component.onMouseOver();
			expect(component.showTooltip).toBeTrue();
			expect(component.tooltipText).toBe('Haz click para copiar');

			component.onMouseLeave();
			expect(component.showTooltip).toBeFalse();
		});
	});

	describe('calculaTiempoGrabacion and formatTime', () => {
		it('should format seconds properly', () => {
			// Arrange & Act & Assert
			const format = (component as any).formatTime.bind(component);
			expect(format(0)).toBe('00:00:00');
			expect(format(65)).toBe('00:01:05');
			expect(format(3665)).toBe('01:01:05');
			expect(format(NaN)).toBe('00:00:00');
			expect(format(Infinity)).toBe('00:00:00');
		});

		it('should increment recording time while emitiendo is true', fakeAsync(() => {
			// Arrange
			component.emitiendo = true;

			// Act
			component.calculaTiempoGrabacion();
			expect(component.tiempoGrabacion).toBe('00:00:00');
			tick(1000);
			expect(component.tiempoGrabacion).toBe('00:00:01');
			tick(1000);
			expect(component.tiempoGrabacion).toBe('00:00:02');
		}));
	});

	describe('visualizeAudio', () => {
		it('should setup AudioContext and connect nodes without throwing', () => {
			// Arrange
			const dummyAudioEl = document.createElement('div');
			const mockSource = { connect: jasmine.createSpy('connect') };
			const mockAnalyser = {
				fftSize: 0,
				frequencyBinCount: 16,
				getByteFrequencyData: jasmine.createSpy('getByteFrequencyData'),
			};
			const mockAudioCtx = {
				createAnalyser: jasmine.createSpy('createAnalyser').and.returnValue(mockAnalyser),
				createMediaStreamSource: jasmine.createSpy('createMediaStreamSource').and.returnValue(mockSource),
			};
			spyOn(window as any, 'AudioContext').and.returnValue(mockAudioCtx);
			const mockStream = {} as MediaStream;

			// Act & Assert
			expect(() => component.visualizeAudio(mockStream, dummyAudioEl)).not.toThrow();
		});

		it('should handle startOBS when videoOBSRef is missing', fakeAsync(() => {
			spyOn(console, 'error');
			component.videoOBSRef = { nativeElement: null } as any;
			component.startOBS();
			tick(350);
			expect(console.error).toHaveBeenCalledWith('Elemento con ID "OBS" no encontrado');
		}));

		it('should handle prepareRTMPData when rtmpUrl is empty', () => {
			component.rtmpUrl = '';
			(component as any).prepareRTMPData();
			expect(component.rtmpServer).toBe('');
		});

		it('should handle initVideoJS when urlPreview is empty', fakeAsync(() => {
			component.urlPreview = '';
			const mockVideo = document.createElement('video');
			spyOn(component as any, 'initVideoJS').and.callThrough();
			(component as any).initVideoJS(mockVideo);
			tick(100);
			// Should retry
			expect((component as any).initVideoJS).toHaveBeenCalled();
		}));

		it('should handle calculaTiempoGrabacion when emitiendo is false', fakeAsync(() => {
			component.emitiendo = false;
			component.calculaTiempoGrabacion();
			tick(1000);
			expect(component.tiempoGrabacion).toBe('00:00:00');
		}));
	});
});
