import { AngularNodeAppEngine, createNodeRequestHandler, isMainModule, writeResponseToNodeResponse } from '@angular/ssr/node';
import type { NextFunction, Request, Response } from 'express';
import express from 'express';
import fs from 'fs';
import http from 'http';
import https from 'https';
import { join } from 'node:path';

// IMPORTA tus rutas

const browserDistFolder = join(import.meta.dirname, '../browser');
const app = express();

// Pasa las serverRoutes al engine
const angularApp = new AngularNodeAppEngine();

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
	/* const envScript = `
    <script id="env">
      window.__env = {
        BACK_BASE: '${process.env['BACK_BASE'] || 'https://localhost:8080'}',
        BACK_STREAM: '${process.env['BACK_STREAM'] || 'https://localhost:8090'}',
        BACK_CHAT: '${process.env['BACK_CHAT'] || 'https://localhost:8070'}',
        BACK_CHAT_WSS: '${process.env['BACK_CHAT_WSS'] || 'wss://localhost:8070'}',
        BACK_STREAM_WSS: '${process.env['BACK_STREAM_WSS'] || 'wss://localhost:8090'}'
      };
    </script>
  `; */

	// TODO: Cambiar las URLs de backend en el entorno de producción
	const envScript = `
    <script id="env">
      window.__env = {
        BACK_BASE: '${'https://localhost:8080'}',
        BACK_STREAM: '${'https://localhost:8090'}',
        BACK_CHAT: '${'https://localhost:8070'}',
        BACK_CHAT_WSS: '${'wss://localhost:8070'}',
        BACK_STREAM_WSS: '${'wss://localhost:8090'}'
      };
    </script>
  `;

	res.locals['envScript'] = envScript;
	next();
});

/**
 * Handle all other requests by rendering the Angular application.
 * Manejo tolerante a errores 404 o conexión para prerender.
 */
app.use((req, res, next) => {
	angularApp
		.handle(req)
		.then(async (response) => {
			if (!response) {
				// Si no hay respuesta, seguir con siguiente middleware
				return next();
			}

			// Si hay status 404, ignorar error y pasar al siguiente middleware
			if (response.status === 404) {
				return next();
			}

			// Procesar normalmente respuestas exitosas
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
 * Manejo de errores
 */
app.use((err: any, req: Request, res: Response, next: NextFunction) => {
	console.error('Error en el servidor:', err);
	res.status(500).send('Error interno del servidor');
});

/**
 * Start the server if this module is the main entry point.
 */
if (isMainModule(import.meta.url)) {
	const projectRoot = process.cwd();

	const key = fs.readFileSync('/certs/key.pem'); // ruta absoluta en contenedor
	const cert = fs.readFileSync('/certs/cert.pem'); // ruta absoluta en contenedor

	// HTTP → HTTPS redirection
	http.createServer((req, res) => {
		const host = req.headers.host;
		res.writeHead(301, { Location: `https://${host}${req.url}` });
		res.end();
	}).listen(80, () => console.log('HTTP redirige a HTTPS en puerto 80'));

	// HTTPS server
	https.createServer({ key, cert }, app).listen(4200, () => {
		console.log('🔒 Servidor HTTPS escuchando en https://localhost:4200');
	});
}

/**
 * Request handler used by Angular CLI or Firebase
 */
export const reqHandler = createNodeRequestHandler(app);
