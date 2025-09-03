import { AngularNodeAppEngine, createNodeRequestHandler, isMainModule, writeResponseToNodeResponse } from '@angular/ssr/node';
import compression from 'compression';
import cookieParser from 'cookie-parser';
import type { NextFunction, Request, Response } from 'express';
import express from 'express';
import fs from 'fs';
import * as http from 'http';
import * as https from 'https';
import { join } from 'node:path';
import { Init } from './app/models/Init';
import { Usuario } from './app/models/Usuario';
import { setGlobalInitCache } from './init-cache';
import { crearJwt, verificarJwt } from './jwt.util';

const URL = process.env['FRONT_URL'] || 'https://localhost:4200';
const browserDistFolder = join(import.meta.dirname, '../browser');
const app = express();

// Wrapper para middlewares async
const asyncHandler = (fn: any) => (req: Request, res: Response, next: NextFunction) => Promise.resolve(fn(req, res, next)).catch(next);

// Middleware
app.use(express.json());
app.use(cookieParser());
app.use(compression());
const angularApp = new AngularNodeAppEngine();

// Servir static files
app.use(express.static(browserDistFolder, { maxAge: '1y', index: false, redirect: false }));

// Middleware para inyectar env y usuario
app.use(
	asyncHandler(async (req: Request, res: Response, next: NextFunction) => {
		const envScript = `
      <script id="env">
        window.__env = {
          BACK_BASE: '${process.env['BACK_BASE'] || 'https://localhost:8080'}',
          BACK_STREAM: '${process.env['BACK_STREAM'] || 'https://localhost:8090'}',
          BACK_CHAT: '${process.env['BACK_CHAT'] || 'https://localhost:8070'}',
          BACK_CHAT_WSS: '${process.env['BACK_CHAT_WSS'] || 'wss://localhost:8070'}',
          BACK_STREAM_WSS: '${process.env['BACK_STREAM_WSS'] || 'wss://localhost:8090'}'
        };
      </script>
    `;
		res.locals['envScript'] = envScript;

		const token = req.cookies['ssrUserToken'];
		//console.log('[SSR] Verificando token');

		if (token) {
			const payload = verificarJwt(token);
			if (payload) {
				try {
					if (!payload.id_usuario) return;
					const usuario = await fetchUsuario(payload.id_usuario);
					res.locals['usuario'] = usuario || null;
					(global as any).ssrUsuario = usuario || null;

					res.cookie('ssrUserToken', token, {
						httpOnly: true,
						secure: true,
						sameSite: 'lax',
						maxAge: 15 * 24 * 60 * 60 * 1000,
					});
				} catch (err) {
					console.error('[SSR] Error al obtener usuario:', err);
					res.locals['usuario'] = null;
					(global as any).ssrUsuario = null;
				}
			} else {
				res.clearCookie('ssrUserToken');
			}
		} else {
			res.locals['usuario'] = null;
			(global as any).ssrUsuario = null;
		}

		next();
	}),
);

// Manejo de errores
app.use((err: any, req: Request, res: Response, next: NextFunction) => {
	console.error('Error en el servidor:', err);
	res.status(500).send('Error interno del servidor');
});

/**
 * Función para obtener usuario desde backend
 * @param id_usuario Number
 * @returns  Promise<Usuario | null>
 */
async function fetchUsuario(id_usuario: number): Promise<Usuario | null> {
	const BACK_BASE = process.env['BACK_BASE_DOCKER'] || 'https://localhost:8080';
	try {
		const headers: Record<string, string> = {};
		const ssrToken = crearJwt({ id_usuario });
		headers['Authorization'] = `Bearer ${ssrToken}`;

		const resp = await fetch(`${BACK_BASE}/usuario/${id_usuario}`, { method: 'GET', headers });
		if (!resp.ok) {
			console.error(`[SSR] Backend devolvió ${resp.status}:`, await resp.text());
			return null;
		}

		return (await resp.json()) as Usuario;
	} catch (err) {
		console.error('[SSR] Error fetch usuario:', err);
		return null;
	}
}

// Refresh cache
app.post(
	'/refresh-cache',
	asyncHandler(async (req: Request, res: Response) => {
		try {
			const newInit: Init = req.body;
			setGlobalInitCache(newInit);
			console.log('[server.ts] Cache global actualizado');
			res.status(200).json({ message: 'Cache global actualizado con éxito!!!' });
		} catch (e) {
			console.error('[server.ts] Error al actualizar cache SSR:', e);
			res.status(500).send({ message: e instanceof Error ? e.message : e });
		}
	}),
);

// SSR login
app.post(
	'/ssr-login',
	asyncHandler(async (req: Request, res: Response) => {
		const { token } = req.body;
		if (!token) {
			res.status(400).json({ ok: false, message: 'Token no enviado' });
			return;
		}

		const payloadFront = verificarJwt(token);
		if (!payloadFront?.id_usuario) {
			res.status(401).json({ ok: false, message: 'Token inválido' });
			return;
		}

		const ssrToken = crearJwt({ id_usuario: payloadFront.id_usuario });
		res.cookie('ssrUserToken', ssrToken, {
			httpOnly: true,
			secure: true,
			sameSite: 'lax',
			maxAge: 15 * 24 * 60 * 60 * 1000,
		});
		res.status(200).json({ ok: true });
	}),
);

// SSR logout
app.get('/ssr-logout', (req: Request, res: Response) => {
	res.clearCookie('ssrUserToken');
	res.status(200).json({ ok: true });
});

// Angular SSR handler
app.use(
	asyncHandler(async (req: Request, res: Response, next: NextFunction) => {
		const response = await angularApp.handle(req);
		if (!response) return next();
		if (response.status === 404) return next();

		let html = await response.text();

		// Sustituimos las variables de entorno
		if (res.locals['envScript']) {
			html = html.replace(/<script id="env">[\s\S]*?<\/script>/, res.locals['envScript']);
		}

		// Precargamos el usuario
		html = html.replace(
			'</head>',
			`<script>
        window['TRANSFER_STATE'] = window['TRANSFER_STATE'] || {};
        window['TRANSFER_STATE']['usuario'] = ${JSON.stringify(res.locals['usuario'] || null)};
      </script></head>`,
		);

		const newResponse = new Response(html, {
			status: response.status,
			statusText: response.statusText,
			headers: response.headers,
		});

		return writeResponseToNodeResponse(newResponse, res);
	}),
);

// HTTPS server
if (isMainModule(import.meta.url)) {
	const key = fs.readFileSync('/certs/key.pem');
	const cert = fs.readFileSync('/certs/cert.pem');

	// ------------------------------
	// Servidor HTTP/1.1 (puerto 4201)
	// ------------------------------
	http.createServer((req, res) => {
		const remoteIp = req.socket.remoteAddress || '';

		console.log('Remote IP:', remoteIp);
		if (remoteIp.startsWith('172.18.') || remoteIp === '127.0.0.1') {
			app(req, res);
		} else {
			const host = req.headers.host;
			res.writeHead(301, { Location: `https://${host}${req.url}` });
			res.end();
		}
	}).listen(4201, () => console.log('HTTP escuchando en 4201'));

	// ------------------------------
	// Servidor HTTPS/HTTP1.1 seguro (puerto 4200)
	// ------------------------------
	https.createServer({ key, cert }, app).listen(4200, () => console.log('🔒 Servidor HTTPS escuchando en ' + URL));
}

export const reqHandler = createNodeRequestHandler(app);
