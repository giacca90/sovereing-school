import { isPlatformServer } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { LoginService } from '../services/login.service';

@Injectable({
	providedIn: 'root',
})
export class AdminGuard implements CanActivate {
	constructor(
		private readonly router: Router,
		private readonly loginService: LoginService,
		@Inject(PLATFORM_ID) private readonly platformId: object,
	) {}

	canActivate(): boolean {
		if (isPlatformServer(this.platformId)) return false;
		if (!this.loginService.usuario || this.loginService.usuario.roll_usuario !== 'admin') {
			this.router.navigate(['/']);
			return false;
		}
		return true;
	}
}
