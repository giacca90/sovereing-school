import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, PLATFORM_ID, provideAppInitializer } from '@angular/core';
import { provideClientHydration, withEventReplay, withHttpTransferCacheOptions } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { isPlatformBrowser } from '@angular/common';
import { routes } from './app.routes';
import { jwtRefreshInterceptor } from './interceptors/jwt-refresh.interceptor';
import { jwtInterceptor } from './interceptors/jwt.interceptor';
import { InitService } from './services/init.service';

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
			const platformId = inject(PLATFORM_ID);

			if (isPlatformBrowser(platformId)) {
				// Espera a que carga + auth terminen antes de render
				return Promise.all([initService.carga(), initService.cargarUsuario()]);
			}

			return initService.carga();
		}),
	],
};
