// init-singleton.ts
import { InitService } from './app/services/init.service';

let initServiceSingleton: InitService | null = null;

export function setInitService(service: InitService) {
	initServiceSingleton = service;
}

export function getInitService(): InitService {
	if (!initServiceSingleton) throw new Error('InitService no inicializado');
	return initServiceSingleton;
}
