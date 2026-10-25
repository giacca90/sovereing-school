import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { CursoChat } from '../../../models/CursoChat';
import { InitChatUsuario } from '../../../models/InitChatUsuario';
import { MensajeChat } from '../../../models/MensajeChat';
import { Usuario } from '../../../models/Usuario';
import { ChatService } from '../../../services/chat.service';
import { LoginService } from '../../../services/login.service';
import { HomeChatComponent } from './home-chat.component';

describe('HomeChatComponent', () => {
	let component: HomeChatComponent;
	let fixture: ComponentFixture<HomeChatComponent>;
	let routerSpy: jasmine.SpyObj<Router>;
	let loginServiceSpy: jasmine.SpyObj<LoginService>;
	let chatServiceSpy: { initSubject: BehaviorSubject<InitChatUsuario | null> };

	beforeEach(async () => {
		const rSpy = jasmine.createSpyObj('Router', ['navigate']);
		const lSpy = jasmine.createSpyObj('LoginService', [], {
			usuario: { idUsuario: 1, nombreUsuario: 'TestUser' } as Usuario,
		});
		const initSub = new BehaviorSubject<InitChatUsuario | null>(null);
		const cSpy = { initSubject: initSub };

		await TestBed.configureTestingModule({
			imports: [HomeChatComponent],
			providers: [
				{ provide: Router, useValue: rSpy },
				{ provide: LoginService, useValue: lSpy },
				{ provide: ChatService, useValue: cSpy },
			],
		}).compileComponents();

		routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
		loginServiceSpy = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
		chatServiceSpy = cSpy;

		fixture = TestBed.createComponent(HomeChatComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should redirect to / if user is not logged in', async () => {
		TestBed.resetTestingModule();
		const rSpy = jasmine.createSpyObj('Router', ['navigate']);
		const lSpy = jasmine.createSpyObj('LoginService', [], { usuario: null });
		const initSub = new BehaviorSubject<InitChatUsuario | null>(null);
		const cSpy = { initSubject: initSub };

		await TestBed.configureTestingModule({
			imports: [HomeChatComponent],
			providers: [
				{ provide: Router, useValue: rSpy },
				{ provide: LoginService, useValue: lSpy },
				{ provide: ChatService, useValue: cSpy },
			],
		}).compileComponents();

		const fixture2 = TestBed.createComponent(HomeChatComponent);
		fixture2.detectChanges();

		expect(rSpy.navigate).toHaveBeenCalledWith(['/']);
	});

	it('should update chats and cursos when initSubject emits valid init data for current user', () => {
		const initData: InitChatUsuario = {
			idUsuario: 1,
			mensajes: [{ idMensaje: 10, idCurso: 5, texto: 'Hola' } as unknown as MensajeChat],
			cursos: [
				{
					idCurso: 5,
					nombreCurso: 'Curso Angular',
					mensajes: [
						{
							idMensaje: 10,
							idCurso: 5,
							nombreUsuario: 'Test',
							nombreClase: 'Clase 1',
							idClase: 1,
						} as unknown as MensajeChat,
					],
				} as unknown as CursoChat,
			],
		};

		chatServiceSpy.initSubject.next(initData);

		expect(component.chats().length).toBe(1);
		expect(component.cursos().length).toBe(1);
		expect(component.cargando()).toBeFalse();
	});

	it('should set cargando to false when initSubject emits invalid or mismatched user id', () => {
		const initData: InitChatUsuario = {
			idUsuario: 999,
			mensajes: [],
			cursos: [],
		};

		chatServiceSpy.initSubject.next(initData);

		expect(component.cargando()).toBeFalse();
	});

	it('should handle error from initSubject gracefully', () => {
		spyOn(console, 'error');
		chatServiceSpy.initSubject.error(new Error('Test error'));
		expect(console.error).toHaveBeenCalledWith('Error en recibir la respuesta: Test error');
	});

	it('should navigate to message view on navegaMensaje', () => {
		const mensaje = { idMensaje: 10, idCurso: 5 } as unknown as MensajeChat;
		component.navegaMensaje(mensaje);
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/chat/', '5', 10]);
	});

	it('should navigate to curso view on navegaCurso', () => {
		const curso = { idCurso: 5, nombreCurso: 'Curso' } as CursoChat;
		component.navegaCurso(curso);
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/chat/', '5']);
	});
});
