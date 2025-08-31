// src/app/tokens.ts
import { InjectionToken } from '@angular/core';
import { Usuario } from './models/Usuario';

export const SSR_USUARIO = new InjectionToken<Usuario | null>('SSR_USUARIO');
