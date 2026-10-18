import { ElementRef, QueryList } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CursoChat } from '../../../models/CursoChat';
import { MensajeChat } from '../../../models/MensajeChat';
import { ChatService } from '../../../services/chat.service';
import { LoginService } from '../../../services/login.service';
import { ChatComponent } from './chat.component';

describe('ChatComponent', () => {
	let component: ChatComponent;
	let fixture: ComponentFixture<ChatComponent>;
	let chatServiceSpy: jasmine.SpyObj<ChatService>;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;
	let routerSpy: jasmine.SpyObj<Router>;

	beforeEach(async () => {
		const cSpy = jasmine.createSpyObj('ChatService', ['mensajeLeido', 'getChat', 'enviarMensaje']);
		cSpy.getChat.and.returnValue(of({ idCurso: 5, clases: [], mensajes: [] } as unknown as CursoChat));
		const lSpy = jasmine.createSpyObj('LoginService', [], {
			usuario: { idUsuario: 1, nombreUsuario: 'User' },
		});
		const rSpy = jasmine.createSpyObj('Router', ['navigate']);
		const routeMock = {
			paramMap: of(
				new Map([
					['idCurso', '5'],
					['idMensaje', 'm1'],
				]),
			),
		};

		await TestBed.configureTestingModule({
			imports: [ChatComponent],
			providers: [
				{ provide: ChatService, useValue: cSpy },
				{ provide: LoginService, useValue: lSpy },
				{ provide: Router, useValue: rSpy },
				{ provide: ActivatedRoute, useValue: routeMock },
			],
		}).compileComponents();

		chatServiceSpy = TestBed.inject(ChatService) as jasmine.SpyObj<ChatService>;
		loginServiceSpy = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
		routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;

		fixture = TestBed.createComponent(ChatComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should handle getChat error gracefully', async () => {
		TestBed.resetTestingModule();
		const cSpy = jasmine.createSpyObj('ChatService', ['mensajeLeido', 'getChat', 'enviarMensaje']);
		cSpy.getChat.and.returnValue(throwError(() => new Error('Chat error')));
		const lSpy = jasmine.createSpyObj('LoginService', [], { usuario: { idUsuario: 1 } });
		const rSpy = jasmine.createSpyObj('Router', ['navigate']);
		const routeMock = { paramMap: of(new Map([['idCurso', '5']])) };

		spyOn(console, 'error');

		await TestBed.configureTestingModule({
			imports: [ChatComponent],
			providers: [
				{ provide: ChatService, useValue: cSpy },
				{ provide: LoginService, useValue: lSpy },
				{ provide: Router, useValue: rSpy },
				{ provide: ActivatedRoute, useValue: routeMock },
			],
		}).compileComponents();

		const f2 = TestBed.createComponent(ChatComponent);
		f2.detectChanges();

		expect(console.error).toHaveBeenCalledWith('Error en recibir el chat: Chat error');
	});

	it('should handle null idCurso case', async () => {
		TestBed.resetTestingModule();
		const cSpy = jasmine.createSpyObj('ChatService', ['mensajeLeido', 'getChat']);
		const lSpy = jasmine.createSpyObj('LoginService', [], { usuario: null });
		const rSpy = jasmine.createSpyObj('Router', ['navigate']);
		const routeMock = { paramMap: of(new Map()) };

		spyOn(console, 'error');

		await TestBed.configureTestingModule({
			imports: [ChatComponent],
			providers: [
				{ provide: ChatService, useValue: cSpy },
				{ provide: LoginService, useValue: lSpy },
				{ provide: Router, useValue: rSpy },
				{ provide: ActivatedRoute, useValue: routeMock },
			],
		}).compileComponents();

		const f3 = TestBed.createComponent(ChatComponent);
		f3.detectChanges();

		expect(console.error).toHaveBeenCalledWith('El curso es nulo');
	});

	it('should call ngOnDestroy and clean up', () => {
		component.ngOnDestroy();
		expect(component.idCurso).toBeNull();
		expect(component.chat).toBeNull();
		expect(component.respuesta).toBeNull();
	});

	it('should toggle abreChatClase', () => {
		expect(component.claseAbiertaId).toBeNull();
		component.abreChatClase(10);
		expect(component.claseAbiertaId).toBe(10);
		component.abreChatClase(10);
		expect(component.claseAbiertaId).toBeNull();
	});

	it('should create pregunta and set placeholder', () => {
		const mockInput = document.createElement('input');
		mockInput.id = 'mexc-10';
		component.mexcInputs = new QueryList<ElementRef<HTMLInputElement>>();
		component.mexcInputs.reset([new ElementRef(mockInput)]);

		component.creaPregunta(10, 95);

		expect(component.pregunta).toEqual({ minute: 1, second: 35 });
		expect(component.claseAbiertaId).toBe(10);
		expect(mockInput.placeholder).toBe('Haz una pregunta en 1:35');
	});

	it('should cierraPregunta and reset placeholder', () => {
		const mockInput = document.createElement('input');
		mockInput.id = 'mexc-10';
		mockInput.placeholder = 'Algo';
		component.mexcInputs = new QueryList<ElementRef<HTMLInputElement>>();
		component.mexcInputs.reset([new ElementRef(mockInput)]);

		component.cierraPregunta(10);

		expect(component.pregunta).toBeNull();
		expect(mockInput.placeholder).toBe('Escribe tu mensaje en la clase...');
	});

	it('should navigate to video with and without momento', () => {
		component.navegaAlVideo(5, 10, 45);
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/repro', 1, 5, 10], { queryParams: { momento: 45 } });

		component.navegaAlVideo(5, 10);
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/repro', 1, 5, 10]);
	});

	it('should send message for top level chat', () => {
		component.idCurso = 5;
		const mockInput = document.createElement('input');
		mockInput.value = 'Hola mundo';
		component.mexInput = new ElementRef(mockInput);
		component.respuesta = { idMensaje: 'r1' } as MensajeChat;

		component.enviarMensaje();

		expect(chatServiceSpy.enviarMensaje).toHaveBeenCalledWith(5, 0, 'Hola mundo', 'r1', null);
		expect(mockInput.value).toBe('');
		expect(component.respuesta).toBeNull();
	});

	it('should send message for class chat', () => {
		component.idCurso = 5;
		const mockInput = document.createElement('input');
		mockInput.id = 'mexc-10';
		mockInput.value = 'Hola clase';
		component.mexcInputs = new QueryList<ElementRef<HTMLInputElement>>();
		component.mexcInputs.reset([new ElementRef(mockInput)]);
		component.respuestaClase = { idMensaje: 'rc1' } as MensajeChat;

		component.enviarMensaje(10);

		expect(chatServiceSpy.enviarMensaje).toHaveBeenCalledWith(5, 10, 'Hola clase', 'rc1', null);
		expect(mockInput.value).toBe('');
		expect(component.respuestaClase).toBeNull();
	});

	it('should log error if sending message with null idCurso', () => {
		component.idCurso = null;
		spyOn(console, 'error');
		component.enviarMensaje();
		expect(console.error).toHaveBeenCalledWith('El curso es null');
	});

	it('should scrollToMessage in top level messages', () => {
		const mockEl = document.createElement('div');
		mockEl.id = 'mex-m1';
		spyOn(mockEl, 'scrollIntoView');
		spyOn(mockEl, 'focus');
		component.mexElements = new QueryList<ElementRef>();
		component.mexElements.reset([new ElementRef(mockEl)]);

		component.scrollToMessage('m1');
		expect(mockEl.scrollIntoView).toHaveBeenCalled();
		expect(mockEl.focus).toHaveBeenCalled();
	});

	it('should scrollToMessage in class messages by opening class chat', fakeAsync(() => {
		const mockEl = document.createElement('div');
		mockEl.id = 'mex-m2';
		spyOn(mockEl, 'scrollIntoView');
		spyOn(mockEl, 'focus');
		component.mexElements = new QueryList<ElementRef>();
		component.mexElements.reset([]);

		component.chat = {
			idCurso: 5,
			clases: [{ idClase: 10, mensajes: [{ idMensaje: 'm2' }] }],
			mensajes: [],
		} as any;

		component.scrollToMessage('m2');
		component.mexElements.reset([new ElementRef(mockEl)]);
		tick(150);

		expect(component.claseAbiertaId).toBe(10);
		expect(mockEl.scrollIntoView).toHaveBeenCalled();
		expect(mockEl.focus).toHaveBeenCalled();
	}));

	it('should send message with reply (respuesta) for top level chat', () => {
		component.idCurso = 5;
		const mockInput = document.createElement('input');
		mockInput.value = 'Respuesta hilo';
		component.mexInput = new ElementRef(mockInput);
		component.respuesta = { idMensaje: 'parent1' } as MensajeChat;

		component.enviarMensaje();

		expect(chatServiceSpy.enviarMensaje).toHaveBeenCalledWith(5, 0, 'Respuesta hilo', 'parent1', null);
		expect(component.respuesta).toBeNull();
	});
});
