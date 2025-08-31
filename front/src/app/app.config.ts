import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { provideClientHydration, withEventReplay, withHttpTransferCacheOptions } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { jwtRefreshInterceptor } from './interceptors/jwt-refresh.interceptor';
import { jwtInterceptor } from './interceptors/jwt.interceptor';
import { InitService } from './services/init.service';
import { LoginService } from './services/login.service';

export const appConfig: ApplicationConfig = {
	providers: [
		provideRouter(routes),

		provideClientHydration(
			withHttpTransferCacheOptions({
				includePostRequests: true,
			}),
			withEventReplay(),
		),

		provideHttpClient(withFetch(), withInterceptors([jwtInterceptor, jwtRefreshInterceptor])),

		provideAppInitializer(() => {
			const initService = inject(InitService);
			const loginService = inject(LoginService);

			return Promise.all([
				initService.carga(),
				loginService.cargarUsuarioDesdeTransferState(), // SSR + navegador
			]);
		}),
	],
};
