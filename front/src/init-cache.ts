// init-cache.ts
import { Init } from './app/models/Init';

export let globalInitCache: Init | null = null;

export function setGlobalInitCache(data: Init) {
	globalInitCache = data;
}

export function getGlobalInitCache(): Init | null {
	return globalInitCache;
}
