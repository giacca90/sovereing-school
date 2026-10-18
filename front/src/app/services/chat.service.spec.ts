import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CursoChat } from '../models/CursoChat';
import { ChatService } from './chat.service';
import { LoginService } from './login.service';

describe('ChatService', () => {
	let service: ChatService;
	let httpMock: HttpTestingController;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;

	beforeEach(() => {
		loginServiceSpy = jasmine.createSpyObj('LoginService', ['refreshToken', 'logout'], {
			usuario: { idUsuario: 1 },
		});
		(window as any).__env = { BACK_CHAT_WSS: 'ws://localhost:8080', BACK_CHAT: 'http://localhost:8080' };

		TestBed.configureTestingModule({
			imports: [HttpClientTestingModule],
			providers: [ChatService, { provide: LoginService, useValue: loginServiceSpy }, { provide: PLATFORM_ID, useValue: 'browser' }],
		});
		service = TestBed.inject(ChatService);
		httpMock = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		httpMock.verify();
		delete (window as any).__env;
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('urlWss and url getters should return correct values', () => {
		expect(service.urlWss).toBe('ws://localhost:8080/chat-socket');
		expect(service.url).toBe('http://localhost:8080');
	});

	it('getAllChats should return list of curso chats on success', (done) => {
		const dummyChats: CursoChat[] = [{ idCurso: 1, nombreCurso: 'Curso 1' } as any];

		service.getAllChats().subscribe((chats) => {
			expect(chats).toEqual(dummyChats);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/getAll');
		expect(req.request.method).toBe('GET');
		req.event(new HttpResponse({ status: 200, body: dummyChats }));
	});

	it('getAllChats should return empty array on non-200 or error', (done) => {
		service.getAllChats().subscribe((chats) => {
			expect(chats).toEqual([]);
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/getAll');
		req.flush('Error', { status: 500, statusText: 'Server Error' });
	});

	it('deleteChat should return true on success message', (done) => {
		const chat: CursoChat = { idCurso: 1 } as any;

		service.deleteChat(chat).subscribe((success) => {
			expect(success).toBeTrue();
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/delete_curso_chat/1');
		expect(req.request.method).toBe('DELETE');
		req.flush('Curso chat borrado con exito!!!');
	});

	it('deleteChat should return false on error response', (done) => {
		const chat: CursoChat = { idCurso: 1 } as any;

		service.deleteChat(chat).subscribe((success) => {
			expect(success).toBeFalse();
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/delete_curso_chat/1');
		req.flush('Error deleting', { status: 400, statusText: 'Bad Request' });
	});

	it('enviarMensaje should publish message to client', () => {
		spyOn((service as any).client, 'publish');

		service.enviarMensaje(1, 10, 'Hola', 'msg-1', { minute: 1, second: 30 });
		service.enviarMensaje(1, 10, 'Hola simple', null, null);

		expect((service as any).client.publish).toHaveBeenCalled();
	});

	it('mensajeLeido should publish read status to client', () => {
		spyOn((service as any).client, 'publish');

		service.mensajeLeido('msg-1');

		expect((service as any).client.publish).toHaveBeenCalled();
	});

	it('getters should handle missing __env', () => {
		delete (window as any).__env;
		expect(service.urlWss).toBe('');
		expect(service.url).toBe('');
	});

	it('onStompError should handle Token invalido error with successful refresh', () => {
		const client = (service as any).client;
		loginServiceSpy.refreshToken.and.returnValue(of('new-token'));
		spyOn(client, 'deactivate');
		spyOn(client, 'activate');

		if (client.onStompError) {
			client.onStompError({ headers: { message: 'Token inválido' } });
		}
		expect(loginServiceSpy.refreshToken).toHaveBeenCalled();
	});

	it('onStompError should handle Token invalido error with failed refresh', () => {
		const client = (service as any).client;
		loginServiceSpy.refreshToken.and.returnValue(throwError(() => new Error('Failed')));
		spyOn(client, 'deactivate');

		if (client.onStompError) {
			client.onStompError({ headers: { message: 'Token inválido' } });
		}
		expect(loginServiceSpy.logout).toHaveBeenCalled();
	});

	it('onWebSocketError should log error', () => {
		const client = (service as any).client;
		spyOn(console, 'error');
		if (client.onWebSocketError) {
			client.onWebSocketError({ message: 'WS Error' });
		}
		expect(console.error).toHaveBeenCalled();
	});

	it('onStompError should handle other errors without token refresh', () => {
		const client = (service as any).client;
		if (client.onStompError) {
			client.onStompError({ headers: { message: 'Some other error' } });
		}
		expect(loginServiceSpy.refreshToken).not.toHaveBeenCalled();
	});

	it('deleteChat should return false on status != 200', (done) => {
		const chat: CursoChat = { idCurso: 1 } as any;

		service.deleteChat(chat).subscribe((success) => {
			expect(success).toBeFalse();
			done();
		});

		const req = httpMock.expectOne('http://localhost:8080/delete_curso_chat/1');
		req.flush('Other message', { status: 200, statusText: 'OK' });
	});

	it('getChat should wait if not connected and handle subscription', (done) => {
		const client = (service as any).client;
		let connectedVal = false;
		Object.defineProperty(client, 'connected', {
			get: () => connectedVal,
			configurable: true,
		});
		(service as any).currentSubscription = { unsubscribe: () => {} };
		spyOn(client, 'subscribe').and.returnValue({ unsubscribe: () => {} });
		spyOn(client, 'publish');

		service.getChat(1).subscribe((curso) => {
			expect(curso).toBeNull();
			done();
		});

		// simulate connection after timeout
		setTimeout(() => {
			connectedVal = true;
		}, 600);
	});

	it('getChat should work when connected and handle existing subscription', () => {
		const client = (service as any).client;
		Object.defineProperty(client, 'connected', {
			get: () => true,
			configurable: true,
		});
		const mockSub = { unsubscribe: jasmine.createSpy('unsubscribe') };
		(service as any).currentSubscription = mockSub;
		spyOn(client, 'subscribe').and.returnValue({ unsubscribe: () => {} });
		spyOn(client, 'publish');

		service.getChat(1).subscribe();
		expect(mockSub.unsubscribe).toHaveBeenCalled();
	});

	it('initUsuario should handle Token inválido response', () => {
		const client = (service as any).client;
		let subCallback: any;
		spyOn(client, 'subscribe').and.callFake((dest: string, cb: any) => {
			subCallback = cb;
			return { unsubscribe: () => {} };
		});
		spyOn(client, 'publish');
		spyOn(client, 'deactivate');
		spyOn(JSON, 'parse').and.returnValue({ idUsuario: 1 } as any);

		(service as any).initUsuario();

		loginServiceSpy.refreshToken.and.returnValue(of(null));
		subCallback({ body: 'Token inválido en init' });
		expect(loginServiceSpy.logout).toHaveBeenCalled();

		loginServiceSpy.refreshToken.and.returnValue(throwError(() => new Error('Refresh error')));
		subCallback({ body: 'Token inválido en init' });
		expect(loginServiceSpy.logout).toHaveBeenCalled();
	});

	it('initUsuario should handle Token inválido with successful refresh', () => {
		const client = (service as any).client;
		let subCallback: any;
		spyOn(client, 'subscribe').and.callFake((dest: string, cb: any) => {
			subCallback = cb;
			return { unsubscribe: () => {} };
		});
		spyOn(client, 'publish');
		loginServiceSpy.refreshToken.and.returnValue(of('new-token-123'));

		(service as any).initUsuario();
		subCallback({ body: 'Token inválido' });

		expect(client.publish).toHaveBeenCalledWith(jasmine.objectContaining({ destination: '/app/refresh-token' }));
	});
});
