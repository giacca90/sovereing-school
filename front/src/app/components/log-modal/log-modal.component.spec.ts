import { Component, ElementRef, Input, PLATFORM_ID } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { LoginModalService } from '../../services/login-modal.service';
import { LoginService } from '../../services/login.service';
import { LogModalComponent } from './log-modal.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';

@Component({
	selector: 'app-login',
	standalone: true,
	template: '<div>Mock Login</div>',
})
class MockLoginComponent {
	@Input() keyEvents: any;
}

@Component({
	selector: 'app-register',
	standalone: true,
	template: '<div>Mock Register</div>',
})
class MockRegisterComponent {
	@Input() keyEvents: any;
}

describe('LogModalComponent', () => {
	let component: LogModalComponent;
	let fixture: ComponentFixture<LogModalComponent>;
	let mockModalService: { hide: jasmine.Spy };
	let mockLoginService: {
		loginWithToken: jasmine.Spy;
		loginSSR: jasmine.Spy;
		usuario: any;
	};
	let mockRouter: { navigate: jasmine.Spy };

	beforeEach(async () => {
		mockModalService = {
			hide: jasmine.createSpy('hide'),
		};

		mockLoginService = {
			loginWithToken: jasmine.createSpy('loginWithToken'),
			loginSSR: jasmine.createSpy('loginSSR'),
			usuario: null,
		};

		mockRouter = {
			navigate: jasmine.createSpy('navigate'),
		};

		await TestBed.configureTestingModule({
			imports: [LogModalComponent],
			providers: [
				{ provide: LoginModalService, useValue: mockModalService },
				{ provide: LoginService, useValue: mockLoginService },
				{ provide: Router, useValue: mockRouter },
				{ provide: PLATFORM_ID, useValue: 'browser' },
			],
		})
			.overrideComponent(LogModalComponent, {
				remove: { imports: [LoginComponent, RegisterComponent] },
				add: { imports: [MockLoginComponent, MockRegisterComponent] },
			})
			.compileComponents();

		fixture = TestBed.createComponent(LogModalComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		// Arrange & Act
		fixture.detectChanges();

		// Assert
		expect(component).toBeTruthy();
		expect(component.isLoginHidden).toBeFalse();
	});

	describe('ngAfterViewInit', () => {
		it('should configure backBase and focus on modal when run in browser platform', fakeAsync(() => {
			// Arrange
			(globalThis.window as any).__env = { BACK_BASE: 'http://test-server:8080' };
			const mockDiv = document.createElement('div');
			spyOn(mockDiv, 'focus');
			component.modalEl = new ElementRef(mockDiv);

			// Act
			component.ngAfterViewInit();
			tick();

			// Assert
			expect(component.backBase).toBe('http://test-server:8080');
			expect(mockDiv.focus).toHaveBeenCalled();
		}));

		it('should do nothing if platform is not browser', fakeAsync(() => {
			// Arrange
			(component as any).platformId = 'server';
			component.backBase = '';

			// Act
			component.ngAfterViewInit();
			tick();

			// Assert
			expect(component.backBase).toBe('');
		}));
	});

	describe('keyboard and view switching interactions', () => {
		it('should emit keyDown events to keyEvents$ subject', () => {
			// Arrange
			spyOn(component.keyEvents$, 'next');
			const testEvent = new KeyboardEvent('keydown', { key: 'a' });

			// Act
			component.onKeyDown(testEvent);

			// Assert
			expect(component.keyEvents$.next).toHaveBeenCalledWith(testEvent);
		});

		it('should set isLoginHidden to false when clickLogin is called', () => {
			// Arrange
			component.isLoginHidden = true;

			// Act
			component.clickLogin();

			// Assert
			expect(component.isLoginHidden).toBeFalse();
		});

		it('should set isLoginHidden to true when clickRegister is called', () => {
			// Arrange
			component.isLoginHidden = false;

			// Act
			component.clickRegister();

			// Assert
			expect(component.isLoginHidden).toBeTrue();
		});

		it('should call alert and hide modal on close', () => {
			// Arrange
			spyOn(window, 'alert');

			// Act
			component.close();

			// Assert
			expect(window.alert).toHaveBeenCalledWith('close externo');
			expect(mockModalService.hide).toHaveBeenCalled();
		});
	});

	describe('oauth2LoginWith', () => {
		let openSpy: jasmine.Spy;

		beforeEach(() => {
			openSpy = spyOn(window, 'open').and.returnValue({} as Window);
			component.backBase = 'http://auth.server';
		});

		it('should open popup for google provider', () => {
			// Arrange & Act
			component.oauth2LoginWith('google');

			// Assert
			expect(openSpy).toHaveBeenCalledWith('http://auth.server/oauth2/authorization/google', '_blank', jasmine.any(String));
		});

		it('should open popup for github provider', () => {
			// Arrange & Act
			component.oauth2LoginWith('github');

			// Assert
			expect(openSpy).toHaveBeenCalledWith('http://auth.server/oauth2/authorization/github', '_blank', jasmine.any(String));
		});

		it('should listen to messages and process correct oauth responses', () => {
			// Arrange
			component.oauth2LoginWith('google');
			spyOn(localStorage, 'setItem');
			spyOn(localStorage, 'getItem').and.returnValue('token_value');

			// Act
			const messageEvent = new MessageEvent('message', {
				origin: 'http://auth.server',
				data: {
					accessToken: 'token_value',
					usuario: { idUsuario: 42, nombreUsuario: 'testOAuth' },
				},
			});
			window.dispatchEvent(messageEvent);

			// Assert
			expect(localStorage.setItem).toHaveBeenCalledWith('Token', 'token_value');
			expect(mockLoginService.loginWithToken).toHaveBeenCalledWith('token_value');
			expect(mockLoginService.usuario).toEqual({ idUsuario: 42, nombreUsuario: 'testOAuth' });
			expect(mockLoginService.loginSSR).toHaveBeenCalledWith('token_value');
			expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
			expect(mockModalService.hide).toHaveBeenCalled();
		});

		it('should ignore message events from unauthorized origins', () => {
			// Arrange
			component.oauth2LoginWith('google');
			spyOn(console, 'error');
			spyOn(localStorage, 'setItem');

			// Act
			const messageEvent = new MessageEvent('message', {
				origin: 'http://malicious-site.com',
				data: { accessToken: 'evil_token' },
			});
			window.dispatchEvent(messageEvent);

			// Assert
			expect(console.error).toHaveBeenCalledWith('Invalid origin: ', 'http://malicious-site.com');
			expect(localStorage.setItem).not.toHaveBeenCalled();
			expect(mockLoginService.loginWithToken).not.toHaveBeenCalled();
		});
	});

	describe('ngOnDestroy', () => {
		it('should complete keyEvents$ subject', () => {
			// Arrange
			spyOn(component.keyEvents$, 'complete');

			// Act
			component.ngOnDestroy();

			// Assert
			expect(component.keyEvents$.complete).toHaveBeenCalled();
		});
	});
});
