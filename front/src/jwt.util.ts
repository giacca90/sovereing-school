import jwt, { JwtPayload, SignOptions } from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';

const SECRET_KEY = process.env['JWT_KEY'] || 'clave-secreta-temporal';

interface JwtPayloadSSR {
	id_usuario: number;
}

/**
 * Crear JWT con expiración de 15 días
 */

export function crearJwt(payload: JwtPayloadSSR): string {
	const expiresInSeconds = 15 * 24 * 60 * 60; // 15 días
	const now = Math.floor(Date.now() / 1000);

	// payload siguiendo lo que hace el back en Java
	const claims = {
		rol: 'ROLE_ADMIN',
		id_usuario: payload.id_usuario,
	};

	const options: SignOptions = {
		issuer: 'AUTH0-JWT', // iss
		subject: 'SSR', // sub
		jwtid: uuidv4(), // jti
		//notBefore: now, // nbf
		expiresIn: expiresInSeconds, // exp
	};

	return jwt.sign(claims, SECRET_KEY, options);
}
/**
 * Verificar y decodificar un token
 *
 * @param token String
 * @returns (JwtPayload & { id_usuario?: number })
 */
export function verificarJwt(token: string): (JwtPayload & { id_usuario?: number }) | null {
	try {
		const decoded = jwt.verify(token, SECRET_KEY) as JwtPayload & { id_usuario?: number };
		return decoded;
	} catch (err: any) {
		if (err.name === 'TokenExpiredError') {
			console.error('[JWT] Token expirado:', err.expiredAt);
		} else if (err.name === 'JsonWebTokenError') {
			console.error('[JWT] Token inválido:', err.message);
		} else {
			console.error('[JWT] Error verificando token:', err);
		}
		return null;
	}
}
