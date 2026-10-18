import { Init } from './app/models/Init';
import { getGlobalInitCache, setGlobalInitCache } from './init-cache';

describe('InitCache', () => {
	it('should set and get global init cache correctly', () => {
		// Arrange
		const mockInit: Init = new Init([], [], {
			usuariosCount: 10,
			cursosCount: 5,
			clasesCount: 20,
		} as any);

		// Act
		setGlobalInitCache(mockInit);
		const cached = getGlobalInitCache();

		// Assert
		expect(cached).toEqual(mockInit);
	});

	it('should return null when cache is not set', () => {
		// Arrange
		(globalThis as any)['__GLOBAL_INIT_CACHE__'] = undefined;

		// Act
		const cached = getGlobalInitCache();

		// Assert
		expect(cached).toBeNull();
	});
});
