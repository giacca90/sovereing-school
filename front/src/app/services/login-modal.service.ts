import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
	providedIn: 'root',
})
export class LoginModalService {
	private readonly isVisible = new BehaviorSubject<boolean>(false);
	isVisible$ = this.isVisible.asObservable();

	/**
	 * Muestra el modal de inicio de sesión.
	 */
	show() {
		this.isVisible.next(true);
	}

	/**
	 * Oculta el modal de inicio de sesión.
	 */
	hide() {
		this.isVisible.next(false);
	}
}
