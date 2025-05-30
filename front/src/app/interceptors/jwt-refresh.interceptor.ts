import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { LoginService } from '../services/login.service';

export const jwtRefreshInterceptor: HttpInterceptorFn = (req, next) => {
	const loginService = inject(LoginService);

	return next(req).pipe(
		catchError((error: HttpErrorResponse) => {
			if (error.status === 401) {
				localStorage.removeItem('Token');
				return loginService.refreshToken().pipe(
					switchMap((token) => {
						if (token) {
							console.log('Hay token: \n');
							console.log(token);
							localStorage.setItem('Token', token);
							let clonedRequest = req.clone({
								setHeaders: {
									Authorization: `Bearer ${token}`,
								},
							});
							if (clonedRequest.body && clonedRequest.url.includes('loginWithToken')) {
								clonedRequest = clonedRequest.clone({ body: token });
							}
							return next(clonedRequest); // Reenviar la petición original
						} else {
							loginService.logout();
							return throwError(() => new Error('No se pudo refrescar el token'));
						}
					}),
					catchError((refreshError) => {
						console.error('Error refrescando token:', refreshError.message);
						loginService.logout();
						return throwError(() => refreshError);
					}),
				);
			}
			return throwError(() => error);
		}),
	);
};
