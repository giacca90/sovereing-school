import { HttpClient, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { LoginService } from './login.service';
import { StreamingService } from './streaming.service';

describe('StreamingService', () => {
	let service: StreamingService;
	let httpClientSpy: jasmine.SpyObj<HttpClient>;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;

	beforeEach(() => {
		(window as any).__env = {
			BACK_STREAM: 'http://localhost:8080/stream',
			BACK_BASE: 'http://localhost:8080/api',
			BACK_STREAM_WSS: 'ws://localhost:8080',
		};

		httpClientSpy = jasmine.createSpyObj('HttpClient', ['get', 'post', 'put', 'delete']);
		loginServiceSpy = jasmine.createSpyObj('LoginService', ['logout', 'refreshToken'], {
			usuario: { idUsuario: 1 },
		});

		TestBed.configureTestingModule({
			providers: [StreamingService, { provide: HttpClient, useValue: httpClientSpy }, { provide: LoginService, useValue: loginServiceSpy }],
		});
		service = TestBed.inject(StreamingService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	describe('environment getters', () => {
		it('should return correct URL values from window.__env', () => {
			expect(service.URL).toBe('http://localhost:8080/stream');
			expect(service.backURL).toBe('http://localhost:8080/api');
			expect(service.backStreamURL).toBe('http://localhost:8080/stream');
			expect(service.webSocketUrlWebcam).toBe('ws://localhost:8080/live-webcam');
			expect(service.webSocketUrlOBS).toBe('ws://localhost:8080/live-obs');
		});

		it('should handle missing window.__env gracefully', () => {
			(window as any).__env = undefined;
			expect(service.URL).toBe('');
			expect(service.backURL).toBe('');
			expect(service.backStreamURL).toBe('');
			expect(service.webSocketUrlWebcam).toBe('');
			expect(service.webSocketUrlOBS).toBe('');
		});
	});

	describe('getVideo', () => {
		it('should call http.get with correct parameters', () => {
			const mockBlob = new Blob(['test'], { type: 'video/mp4' });
			httpClientSpy.get.and.returnValue(of(mockBlob));

			service.getVideo(1, 2).subscribe((blob) => {
				expect(blob).toEqual(mockBlob);
			});

			expect(httpClientSpy.get).toHaveBeenCalledWith('http://localhost:8080/stream/1/2', jasmine.any(Object));
		});
	});

	describe('subeVideo', () => {
		it('should upload video and return body on success', () => {
			const mockFile = new File(['dummy content'], 'test.mp4', { type: 'video/mp4' });
			const mockHttpResponse = new HttpResponse({ body: 'Upload Success', status: 200 });
			httpClientSpy.post.and.returnValue(of(mockHttpResponse));

			service.subeVideo(mockFile, 1, 2).subscribe((result) => {
				expect(result).toBe('Upload Success');
			});

			expect(httpClientSpy.post).toHaveBeenCalled();
		});

		it('should return null and log error on failure', () => {
			const mockFile = new File(['dummy content'], 'test.mp4', { type: 'video/mp4' });
			httpClientSpy.post.and.returnValue(throwError(() => new Error('Network error')));
			spyOn(console, 'error');

			service.subeVideo(mockFile, 1, 2).subscribe((result) => {
				expect(result).toBeNull();
			});

			expect(console.error).toHaveBeenCalled();
		});
	});

	describe('registrarProgreso', () => {
		it('should call http.post with segment params', () => {
			httpClientSpy.post.and.returnValue(of({ success: true }));

			service.registrarProgreso(1, 2, 3).subscribe((res) => {
				expect(res).toEqual({ success: true });
			});

			expect(httpClientSpy.post).toHaveBeenCalledWith(
				'http://localhost:8080/stream/registrar-fragmento',
				null,
				jasmine.objectContaining({
					params: { idCurso: '1', idClase: '2', segment: '3' },
					withCredentials: true,
				}),
			);
		});
	});

	describe('getPresets', () => {
		it('should call http.get with user id', () => {
			httpClientSpy.get.and.returnValue(of({}));

			service.getPresets().subscribe();

			expect(httpClientSpy.get).toHaveBeenCalledWith('http://localhost:8080/stream/presets/get/1', { responseType: 'json', withCredentials: true });
		});
	});

	describe('savePresets', () => {
		it('should call http.put with serialized presets map', () => {
			httpClientSpy.put.and.returnValue(of({ success: true }));
			const presetsMap = new Map<string, any>([['key1', { val: 1 }]]);

			service.savePresets(presetsMap);

			expect(httpClientSpy.put).toHaveBeenCalledWith('http://localhost:8080/stream/presets/save/1', JSON.stringify({ key1: { val: 1 } }));
		});
	});

	describe('media streaming controls', () => {
		it('detenerWebOBS should handle null ws gracefully', () => {
			spyOn(console, 'error');
			service.detenerWebOBS();
			expect(console.error).toHaveBeenCalledWith('No se pudo detener WebRTC');
			expect(service.emitiendo).toBeFalse();
		});

		it('detenerOBS should handle null ws gracefully', () => {
			spyOn(console, 'error');
			service.detenerOBS();
			expect(console.error).toHaveBeenCalledWith('No se pudo detener OBS');
		});

		it('stopMediaStreaming should stop observation and close ws', () => {
			service.stopMediaStreaming();
			expect(service.emitiendo).toBeFalse();
			expect(service.status).toBe('Transmisión detenida.');
		});

		it('emitirOBS should throw error if ws is not open', async () => {
			await expectAsync(service.emitirOBS()).toBeRejectedWithError('No se puede conectar con WebSocket');
		});

		it('emitirOBS should throw error if rtmpUrl is empty', async () => {
			// Mock ws readyState = WebSocket.OPEN (1)
			(service as any).ws = { readyState: 1 };
			service.rtmpUrl = '';
			await expectAsync(service.emitirOBS()).toBeRejectedWithError('No se puede emitir OBS sin una ruta RTMP válida');
		});
	});

	describe('WebSocket and WebRTC streaming', () => {
		class MockWebSocket {
			static OPEN = 1;
			url: string;
			onopen: (() => void) | null = null;
			onmessage: ((event: { data: string }) => void) | null = null;
			onerror: ((event: any) => void) | null = null;
			onclose: (() => void) | null = null;
			readyState = 1;
			constructor(url: string) {
				this.url = url;
				setTimeout(() => this.onopen?.(), 10);
			}
			send(data: string) {}
			close() {
				this.onclose?.();
			}
		}

		beforeEach(() => {
			(globalThis as any).WebSocket = MockWebSocket;
			(globalThis as any).RTCIceCandidate = class {
				constructor(public candidate: any) {}
			};
			(globalThis as any).RTCPeerConnection = class {
				onicecandidate: any = null;
				remoteDescription = { type: 'answer' };
				addTrack() {
					return {
						getParameters: () => ({ encodings: [{}] }),
						setParameters: () => Promise.resolve(),
					};
				}
				createOffer() {
					return Promise.resolve({ sdp: 'sdp' });
				}
				setLocalDescription() {
					return Promise.resolve();
				}
				setRemoteDescription() {
					return Promise.resolve();
				}
				addIceCandidate() {
					return Promise.resolve();
				}
			};
			localStorage.setItem('Token', 'mock-token');
		});

		afterEach(() => {
			localStorage.removeItem('Token');
		});

		it('startWebOBS should reject if WebSocket is not supported', async () => {
			const originalWS = (globalThis.window as any).WebSocket;
			(globalThis.window as any).WebSocket = undefined;
			await expectAsync(service.startWebOBS()).toBeRejectedWithError('❌ WebSocket no soportado en este navegador.');
			(globalThis.window as any).WebSocket = originalWS;
		});

		it('startWebOBS should reject if Token is missing', async () => {
			localStorage.removeItem('Token');
			await expectAsync(service.startWebOBS()).toBeRejectedWithError('Token JWT no encontrado');
			localStorage.setItem('Token', 'mock-token');
		});

		it('startWebOBS should resolve streamId on success message', async () => {
			const promise = service.startWebOBS();
			setTimeout(() => {
				(service as any).ws.onmessage({ data: JSON.stringify({ type: 'streamId', streamId: 'stream-123' }) });
			}, 20);
			const id = await promise;
			expect(id).toBe('stream-123');
			expect(service.streamId).toBe('stream-123');
		});

		it('startOBS should establish connection and request rtmp url', async () => {
			spyOn(MockWebSocket.prototype, 'send');
			service.startOBS();
			await new Promise((resolve) => setTimeout(resolve, 20));
			expect(service.status).toContain('Conexión WebSocket establecida');
		});

		it('emitirOBS should send message when ready and rtmpUrl is set', async () => {
			service.startOBS();
			await new Promise((resolve) => setTimeout(resolve, 20));
			service.rtmpUrl = 'rtmp://server/live';
			if ((service as any).ws) {
				(service as any).ws.readyState = 1;
				spyOn((service as any).ws, 'send');
			}

			await service.emitirOBS();
			expect((service as any).ws.send).toHaveBeenCalled();
		});

		it('emitirWebOBS should send offer and ice candidates', async () => {
			service.streamId = 'stream-123';
			const ws = new MockWebSocket('ws://test');
			ws.readyState = 1;
			(service as any).ws = ws;
			spyOn(ws, 'send');

			const mockStream = {
				getVideoTracks: () => [{ getSettings: () => ({ width: 1280, height: 720, frameRate: 30 }) }],
				getTracks: () => [
					{
						kind: 'video',
						getSettings: () => ({ width: 1280, height: 720, frameRate: 30 }),
					},
					{
						kind: 'audio',
					},
				],
			} as unknown as MediaStream;

			await service.emitirWebOBS(mockStream);
			expect(service.emitiendo).toBeTrue();

			// Test onicecandidate
			if ((service as any).pc.onicecandidate) {
				(service as any).pc.onicecandidate({ candidate: { candidate: 'cand' } });
				expect(ws.send).toHaveBeenCalled();
			}
		});

		it('emitirWebOBS should throw error if stream is invalid or ws not open', async () => {
			await expectAsync(service.emitirWebOBS(null as any)).toBeRejectedWithError('No se proporcionó un MediaStream válido.');

			service.streamId = 'stream-123';
			(service as any).ws = { readyState: 0 }; // CLOSED
			const mockStream = {
				getVideoTracks: () => [{ getSettings: () => ({ width: 1280, height: 720, frameRate: 30 }) }],
			} as any;
			await expectAsync(service.emitirWebOBS(mockStream)).toBeRejectedWithError('WebSocket no está abierto.');

			(service as any).ws = { readyState: 1 };
			service.streamId = null;
			await expectAsync(service.emitirWebOBS(mockStream)).toBeRejectedWithError('No hay streamId disponible.');

			service.streamId = 'stream-123';
			const mockStreamNoVideo = {
				getVideoTracks: () => [],
			} as any;
			await expectAsync(service.emitirWebOBS(mockStreamNoVideo)).toBeRejectedWithError('El MediaStream no contiene pista de video.');

			const mockStreamNoRes = {
				getVideoTracks: () => [{ getSettings: () => ({ width: 0, height: 0, frameRate: 0 }) }],
			} as any;
			await expectAsync(service.emitirWebOBS(mockStreamNoRes)).toBeRejectedWithError('No se puede emitir sin resolución o FPS.');
		});

		it('startWebOBS should fail if WebSocket not supported', async () => {
			const origWs = window.WebSocket;
			(window as any).WebSocket = undefined;
			await expectAsync(service.startWebOBS()).toBeRejectedWithError('❌ WebSocket no soportado en este navegador.');
			(window as any).WebSocket = origWs;
		});

		it('startWebOBS should fail if Token not found', async () => {
			localStorage.removeItem('Token');
			await expectAsync(service.startWebOBS()).toBeRejectedWithError('Token JWT no encontrado');
		});

		it('startOBS should return early if WebSocket not supported', () => {
			const origWs = window.WebSocket;
			(window as any).WebSocket = undefined;
			service.startOBS();
			expect(service.status).toContain('WebSocket no soportado');
			(window as any).WebSocket = origWs;
		});

		it('startOBS should return early if Token not found', () => {
			localStorage.removeItem('Token');
			spyOn(console, 'error');
			service.startOBS();
			expect(console.error).toHaveBeenCalledWith('Token JWT no encontrado en localStorage');
		});

		it('detenerWebOBS should send stop message when ws is present', () => {
			const ws = new MockWebSocket('ws://test');
			spyOn(ws, 'send');
			(service as any).ws = ws;
			service.streamId = 'stream-123';
			service.detenerWebOBS();
			expect(ws.send).toHaveBeenCalled();
			expect(service.emitiendo).toBeFalse();
		});

		it('detenerOBS should send stop message when ws is present', () => {
			const ws = new MockWebSocket('ws://test');
			spyOn(ws, 'send');
			(service as any).ws = ws;
			service.rtmpUrl = 'rtmp://live';
			service.detenerOBS();
			expect(ws.send).toHaveBeenCalled();
			expect(service.status).toContain('Deteniendo');
		});

		it('subeVideo should handle non-ok response', () => {
			const mockFile = new File(['dummy'], 'test.mp4');
			const mockResp = new HttpResponse({ body: null, status: 400, statusText: 'Bad Request' });
			httpClientSpy.post.and.returnValue(of(mockResp));

			service.subeVideo(mockFile, 1, 1).subscribe((res) => {
				expect(res).toBeNull();
			});
		});

		it('setupWebSocketHandlersOBS should handle auth, rtmp_url, error, start, info, unknown and parse error', async () => {
			service.startOBS();
			await new Promise((resolve) => setTimeout(resolve, 20));
			const ws = (service as any).ws;

			loginServiceSpy.refreshToken.and.returnValue(of('new-token'));
			ws.onmessage({ data: JSON.stringify({ type: 'auth', message: 'Token inválido' }) });
			expect(loginServiceSpy.refreshToken).toHaveBeenCalled();

			loginServiceSpy.refreshToken.and.returnValue(throwError(() => new Error('Refresh failed')));
			ws.onmessage({ data: JSON.stringify({ type: 'auth', message: 'fail' }) });

			ws.onmessage({ data: JSON.stringify({ type: 'rtmp_url', message: 'rtmp://server/live/123' }) });
			expect(service.rtmpUrl).toBe('rtmp://server/live/123');

			ws.onmessage({ data: JSON.stringify({ type: 'error', message: 'Some error' }) });
			expect(service.emitiendo).toBeFalse();

			ws.onmessage({ data: JSON.stringify({ type: 'start' }) });
			expect(service.emitiendo).toBeTrue();

			ws.onmessage({ data: JSON.stringify({ type: 'info', message: 'Info msg' }) });
			ws.onmessage({ data: JSON.stringify({ type: 'unknown_type' }) });

			// Parse error
			spyOn(console, 'error');
			ws.onmessage({ data: 'invalid-json' });
			expect(console.error).toHaveBeenCalled();
		});

		it('setupWebSocketHandlersWebOBS should handle auth, webrtc-answer, candidate, info, unknown and parse error', async () => {
			const promise = service.startWebOBS();
			await new Promise((resolve) => setTimeout(resolve, 20));
			const ws = (service as any).ws;

			loginServiceSpy.refreshToken.and.returnValue(of('new-token'));
			ws.onmessage({ data: JSON.stringify({ type: 'auth', message: 'Token expired' }) });

			loginServiceSpy.refreshToken.and.returnValue(throwError(() => new Error('Refresh error')));
			ws.onmessage({ data: JSON.stringify({ type: 'auth', message: 'fail' }) });

			// webrtc-answer
			(service as any).pc = {
				setRemoteDescription: () => Promise.resolve(),
				addIceCandidate: () => Promise.resolve(),
				remoteDescription: { type: 'answer' },
			};
			ws.onmessage({ data: JSON.stringify({ type: 'webrtc-answer', sdp: 'sdp' }) });

			// candidate without remoteDescription type yet
			(service as any).pc.remoteDescription = null;
			ws.onmessage({ data: JSON.stringify({ type: 'candidate', candidate: { candidate: 'cand' } }) });

			// candidate with remoteDescription type
			(service as any).pc.remoteDescription = { type: 'answer' };
			ws.onmessage({ data: JSON.stringify({ type: 'candidate', candidate: { candidate: 'cand' } }) });

			ws.onmessage({ data: JSON.stringify({ type: 'info', message: 'info' }) });
			ws.onmessage({ data: JSON.stringify({ type: 'unknown' }) });
			spyOn(console, 'error');
			ws.onmessage({ data: 'bad-json' });
			expect(console.error).toHaveBeenCalled();

			ws.onmessage({ data: JSON.stringify({ type: 'streamId', streamId: 'stream-123' }) });

			try {
				await promise;
			} catch (e) {}
		});
	});
});
