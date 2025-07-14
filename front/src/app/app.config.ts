import { ApplicationConfig, inject } from '@angular/core';
import { provideRouter } from '@angular/router';

import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideClientHydration, withHttpTransferCacheOptions } from '@angular/platform-browser';

import { routes } from './app.routes';
import { jwtRefreshInterceptor } from './interceptors/jwt-refresh.interceptor';
import { jwtInterceptor } from './interceptors/jwt.interceptor';

import { provideAppInitializer } from '@angular/core';
import { InitService } from './services/init.service';

export const appConfig: ApplicationConfig = {
	providers: [
		provideRouter(routes),
		provideClientHydration(
			withHttpTransferCacheOptions({
				includePostRequests: true,
			}),
		),
		provideHttpClient(withFetch(), withInterceptors([jwtInterceptor, jwtRefreshInterceptor])),

		provideAppInitializer(() => {
			const initService = inject(InitService);
			return initService.carga();
		}),
	],
};
