import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Renderer2 } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { AppComponent } from './app.component';
import { LoginModalService } from './services/login-modal.service';
import { LoginService } from './services/login.service';

describe('AppComponent', () => {
	let component: AppComponent;
	let fixture: ComponentFixture<AppComponent>;
	let isVisibleSubject: BehaviorSubject<boolean>;
	let mockModalService: jasmine.SpyObj<LoginModalService>;
	let mockLoginService: jasmine.SpyObj<LoginService>;
	let router: Router;
	let renderer: Renderer2;

	beforeEach(async () => {
		isVisibleSubject = new BehaviorSubject<boolean>(false);
		mockModalService = jasmine.createSpyObj('LoginModalService', ['show', 'hide'], {
			isVisible$: isVisibleSubject.asObservable(),
		});
		mockLoginService = jasmine.createSpyObj('LoginService', ['logout'], {
			usuario: null,
		});

		await TestBed.configureTestingModule({
			imports: [AppComponent],
			providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: LoginModalService, useValue: mockModalService }, { provide: LoginService, useValue: mockLoginService }],
		}).compileComponents();

		fixture = TestBed.createComponent(AppComponent);
		component = fixture.componentInstance;
		router = TestBed.inject(Router);
		renderer = fixture.componentRef.injector.get(Renderer2);
		spyOn(router, 'navigate').and.resolveTo(true);
	});

	it('should create the app', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
		expect(component.title).toBe('Sovereign School');
	});

	describe('ngOnInit', () => {
		it('should subscribe to modalService.isVisible$ and update isModalVisible', () => {
			// Arrange
			component.ngOnInit();

			// Act
			isVisibleSubject.next(true);

			// Assert
			expect(component.isModalVisible).toBeTrue();
		});

		it('should configure matchMedia listener and handle dark theme when theme not in localStorage', () => {
			// Arrange
			let mediaListener: ((e: any) => void) | undefined;
			const fakeMediaQueryList = {
				addEventListener: jasmine.createSpy('addEventListener').and.callFake((event: string, callback: any) => {
					mediaListener = callback;
				}),
			};
			spyOn(window, 'matchMedia').and.returnValue(fakeMediaQueryList as any);
			spyOn(renderer, 'addClass');
			spyOn(renderer, 'removeClass');
			spyOn(localStorage, 'getItem').and.returnValue(null);

			// Act
			component.ngOnInit();
			expect(fakeMediaQueryList.addEventListener).toHaveBeenCalledWith('change', jasmine.any(Function));

			// Assert branch 1: e.matches is true and 'theme' not in localStorage
			if (mediaListener) {
				delete (localStorage as any)['theme'];
				mediaListener({ matches: true });
				expect(renderer.addClass).toHaveBeenCalledWith(document.documentElement, 'dark');

				// Assert branch 2: e.matches is false and 'theme' not in localStorage
				mediaListener({ matches: false });
				expect(renderer.removeClass).toHaveBeenCalledWith(document.documentElement, 'dark');

				// Assert branch 3: 'theme' exists in localStorage, should do nothing
				(localStorage as any)['theme'] = 'light';
				(renderer.addClass as jasmine.Spy).calls.reset();
				(renderer.removeClass as jasmine.Spy).calls.reset();
				mediaListener({ matches: true });
				expect(renderer.addClass).not.toHaveBeenCalled();
				expect(renderer.removeClass).not.toHaveBeenCalled();
				delete (localStorage as any)['theme'];
			}
		});
	});

	describe('openModal', () => {
		it('should call modalService.show()', () => {
			// Arrange & Act
			component.openModal();

			// Assert
			expect(mockModalService.show).toHaveBeenCalled();
		});
	});

	describe('salir', () => {
		it('should set vistaMenu to false, call logout and navigate to root', () => {
			// Arrange
			component.vistaMenu = true;

			// Act
			component.salir();

			// Assert
			expect(component.vistaMenu).toBeFalse();
			expect(mockLoginService.logout).toHaveBeenCalled();
			expect(router.navigate).toHaveBeenCalledWith(['/']);
		});
	});

	describe('changeTheme', () => {
		afterEach(() => {
			localStorage.removeItem('theme');
		});

		it('should toggle to light mode when current theme is dark', () => {
			// Arrange
			localStorage.setItem('theme', 'dark');
			const removeSpy = spyOn(renderer, 'removeClass');

			// Act
			component.changeTheme();

			// Assert
			expect(localStorage.getItem('theme')).toBe('light');
			expect(removeSpy).toHaveBeenCalledWith(document.documentElement, 'dark');
		});

		it('should toggle to dark mode when current theme is not dark', () => {
			// Arrange
			localStorage.setItem('theme', 'light');
			const addSpy = spyOn(renderer, 'addClass');

			// Act
			component.changeTheme();

			// Assert
			expect(localStorage.getItem('theme')).toBe('dark');
			expect(addSpy).toHaveBeenCalledWith(document.documentElement, 'dark');
		});
	});

	describe('ngOnDestroy', () => {
		it('should unsubscribe from subscription on destroy', () => {
			// Arrange
			const unsubSpy = spyOn((component as any).subscription, 'unsubscribe');

			// Act
			component.ngOnDestroy();

			// Assert
			expect(unsubSpy).toHaveBeenCalled();
		});
	});
});
