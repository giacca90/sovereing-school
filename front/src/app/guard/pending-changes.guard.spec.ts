import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { pendingChangesGuard } from './pending-changes.guard';

describe('pendingChangesGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => pendingChangesGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
