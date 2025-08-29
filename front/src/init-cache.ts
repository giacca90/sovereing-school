// init-cache.ts
import { Init } from './app/models/Init';

const GLOBAL_KEY = '__GLOBAL_INIT_CACHE__';

export function setGlobalInitCache(data: Init) {
	(globalThis as any)[GLOBAL_KEY] = data;
}

export function getGlobalInitCache(): Init | null {
	return (globalThis as any)[GLOBAL_KEY] || null;
}
