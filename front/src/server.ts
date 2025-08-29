import { AngularNodeAppEngine, createNodeRequestHandler, isMainModule, writeResponseToNodeResponse } from '@angular/ssr/node';
import compression from 'compression';
import type { NextFunction, Request, Response } from 'express';
import express from 'express';
import fs from 'fs';
import http2 from 'http';
import https2 from 'https';
import { join } from 'node:path';
import { Init } from './app/models/Init';
import { setGlobalInitCache } from './init-cache';

// IMPORTA tus rutas
const URL = process.env['FRONT_URL'] || 'https://localhost:4200';

const browserDistFolder = join(import.meta.dirname, '../browser');
const app = express();

// Middleware para poder parsear JSON en POST
app.use(express.json());

// Pasa las serverRoutes al engine
const angularApp = new AngularNodeAppEngine();

// Compresion
app.use(compression());

/**
 * Serve static files from /browser
 */
app.use(
	express.static(browserDistFolder, {
		maxAge: '1y',
		index: false,
		redirect: false,
	}),
);

/**
 * Inject environment variables
 */
app.use((req, res, next) => {
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
	next();
});

/**
 * Manejo de errores
 */
app.use((err: any, req: Request, res: Response, next: NextFunction) => {
	console.error('Error en el servidor:', err);
	res.status(500).send('Error interno del servidor');
});

/**
 * ---------------------------------------------------
 * Intentar arrancar (bootstrap) el main.server compilado al inicio
 * ---------------------------------------------------
 *
 * Si has hecho la build SSR correctamente, en dist/<project>/server
 * tendrás un main.js (o main.mjs). Si existe, lo importamos y lo ejecutamos
 * para que registre InitService via setInitService(...) durante el bootstrap.
 *
 * Si no existe, no fallamos el proceso: la ruta /refresh-cache devolverá 503
 * hasta que se haga el bootstrap manualmente o desplegues el server bundle.
 */
async function tryBootstrapServerBundle() {
	try {
		// Ajusta la ruta según tu salida de build: aquí se asume dist/front/server/main.js
		const serverMainPath = join(process.cwd(), 'dist/front/server/server.mjs');

		if (fs.existsSync(serverMainPath)) {
			// Import dinámico del bundle server compilado y ejecución de su export por defecto
			// (se asume que main.server exporta una función default que realiza el bootstrap y registra InitService)
			const mod = await import(serverMainPath);
			const bootstrapFn = mod.default || mod; // si es CJS vendrá directo en mod
			if (typeof bootstrapFn === 'function') {
				await bootstrapFn();
				console.log('[server.ts] Bundle server arrancado correctamente desde', serverMainPath);
			} else {
				console.warn('[server.ts] El módulo server no exporta una función bootstrap.');
			}
		} else {
			console.warn('[server.ts] No existe el bundle server precompilado en:', serverMainPath);
		}
	} catch (err) {
		console.error('[server.ts] Error arrancando bundle server:', err);
	}
}

// Llamada al arrancar el proceso (no bloqueante)
tryBootstrapServerBundle();

/**
 * Refrescar cache de la página en caso de cambios en el backend
 *
 * Nota: este endpoint debe estar *antes* del handler de Angular (más abajo),
 * para que no lo capture el middleware de prerender y evitar errores de body/lock.
 */
app.post('/refresh-cache', (req, res) => {
	console.log('<---------------------------------------------->');
	try {
		const newInit: Init = req.body;
		setGlobalInitCache(newInit);
		console.log('[server.ts] Actualizando cache global');
		console.log(newInit.estadistica);
		res.status(200).json({ message: 'Cache global actualizado con éxito!!!' });
	} catch (e) {
		console.error('[server.ts] Error al actualizar cache SSR:', e);
		res.status(500).send({ message: e instanceof Error ? e.message : e });
	}
});

/**
 * Handle all other requests by rendering the Angular application.
 * Manejo tolerante a errores 404 o conexión para prerender.
 *
 * Este middleware queda *DESPUÉS* de las rutas específicas (como /refresh-cache)
 * para evitar que Angular intente manejar peticiones que quieran ser tratadas por Express.
 */
app.use((req, res, next) => {
	angularApp
		.handle(req)
		.then(async (response) => {
			if (!response) return next();
			if (response.status === 404) return next();

			if (response.body) {
				const html = await response.text();
				const modifiedHtml = html.replace(/<script id="env">[\s\S]*?<\/script>/, res.locals['envScript']);
				const newResponse = new Response(modifiedHtml, {
					status: response.status,
					statusText: response.statusText,
					headers: response.headers,
				});
				return writeResponseToNodeResponse(newResponse, res);
			}

			return next();
		})
		.catch((err) => {
			console.warn('Error prerendering (se ignora para no fallar):', err);
			// Si hay error de conexión o fetch fallido, evitar fallo: enviar respuesta básica o seguir
			res.status(200).send('<html><body><h1>Servidor Angular prerender fallback</h1></body></html>');
		});
});

/**
 * Start the server if this module is the main entry point.
 */
if (isMainModule(import.meta.url)) {
	const key = fs.readFileSync('/certs/key.pem'); // ruta absoluta en contenedor
	const cert = fs.readFileSync('/certs/cert.pem'); // ruta absoluta en contenedor

	// HTTP → HTTPS redirection
	http2
		.createServer((req, res) => {
			const host = req.headers.host;
			res.writeHead(301, { Location: `https://${host}${req.url}` });
			res.end();
		})
		.listen(80, () => console.log('HTTP redirige a HTTPS en puerto 80'));

	// HTTPS server
	https2.createServer({ key, cert }, app).listen(4200, () => {
		console.log('🔒 Servidor HTTPS escuchando en ' + URL);
	});
}

/**
 * Request handler used by Angular CLI or Firebase
 */
export const reqHandler = createNodeRequestHandler(app);
