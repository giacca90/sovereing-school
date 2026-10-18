import { TestBed } from '@angular/core/testing';
import { CanComponentDeactivate } from '../interfaces/can-component-deactivate';
import { PendingChangesGuard } from './pending-changes.guard';

describe('PendingChangesGuard', () => {
	let guard: PendingChangesGuard;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [PendingChangesGuard],
		});
		guard = TestBed.inject(PendingChangesGuard);
	});

	it('should be created', () => {
		expect(guard).toBeTruthy();
	});

	describe('canDeactivate', () => {
		it('should return true if component.canDeactivate is not defined', () => {
			const component = {} as CanComponentDeactivate;
			expect(guard.canDeactivate(component)).toBeTrue();
		});

		it('should return the result of component.canDeactivate() if defined', () => {
			const component: CanComponentDeactivate = {
				canDeactivate: () => false,
			};
			expect(guard.canDeactivate(component)).toBeFalse();
		});
	});
});
