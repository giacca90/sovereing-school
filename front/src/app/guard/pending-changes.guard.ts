// pending-changes.guard.ts
import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';
import { CanComponentDeactivate } from '../interfaces/can-component-deactivate';

@Injectable({
	providedIn: 'root',
})
export class PendingChangesGuard implements CanDeactivate<CanComponentDeactivate> {
	canDeactivate(component: CanComponentDeactivate): boolean | Promise<boolean> {
		// Llama a la función del componente para decidir si permite salir
		return component.canDeactivate ? component.canDeactivate() : true;
	}
}
