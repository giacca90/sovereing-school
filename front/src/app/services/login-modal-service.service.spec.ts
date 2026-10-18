import { TestBed } from '@angular/core/testing';
import { LoginModalService } from './login-modal.service';

describe('LoginModalService', () => {
	let service: LoginModalService;

	beforeEach(() => {
		TestBed.configureTestingModule({});
		service = TestBed.inject(LoginModalService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('should initially have isVisible$ as false', (done) => {
		service.isVisible$.subscribe((visible) => {
			expect(visible).toBeFalse();
			done();
		});
	});

	it('should set isVisible$ to true when show() is called', (done) => {
		service.show();
		service.isVisible$.subscribe((visible) => {
			expect(visible).toBeTrue();
			done();
		});
	});

	it('should set isVisible$ to false when hide() is called after show()', (done) => {
		service.show();
		service.hide();
		service.isVisible$.subscribe((visible) => {
			expect(visible).toBeFalse();
			done();
		});
	});
});
