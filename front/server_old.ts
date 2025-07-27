import { APP_BASE_HREF } from '@angular/common';
import { CommonEngine } from '@angular/ssr/node';
import express from 'express';
import fs from 'fs';
import http from 'http';
import https from 'https';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import bootstrap from './src/main.server';

export function createApp(): express.Express {
	const app = express();
	const __dirname = dirname(fileURLToPath(import.meta.url));
	const browserDist = resolve(__dirname, '../browser');
	const indexHtml = join(__dirname, 'index.server.html');
	const engine = new CommonEngine();

	// Servir estáticos
	app.use('/assets', express.static(join(browserDist, 'assets')));
	app.use(express.static(browserDist));

	// SSR catch-all con expresión regular para evitar errores de path-to-regexp
	app.get(/.*/, (req, res, next) => {
		engine
			.render({
				bootstrap,
				documentFilePath: indexHtml,
				url: `${req.protocol}://${req.headers.host}${req.originalUrl}`,
				publicPath: browserDist,
				providers: [{ provide: APP_BASE_HREF, useValue: req.baseUrl }],
			})
			.then((html) => {
				const newEnvScript = `
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

				// Sustituir el contenido del script original (por id)
				html = html.replace(/<script id="env">[\\s\\S]*?<\/script>/, newEnvScript);

				res.send(html);
			})
			.catch((err) => next(err));
	});

	return app;
}

// Leer certificados desde la raíz del proyecto (donde está package.json)
const projectRoot = process.cwd();
const key = fs.readFileSync(join(projectRoot, 'key.pem'));
const cert = fs.readFileSync(join(projectRoot, 'cert.pem'));

// Redirigir HTTP → HTTPS
http.createServer((req, res) => {
	const host = req.headers.host;
	res.writeHead(301, { Location: `https://${host}${req.url}` });
	res.end();
}).listen(80, () => console.log('HTTP redirige a HTTPS en puerto 80'));

// Arrancar servidor HTTPS
https.createServer({ key, cert }, createApp()).listen(4200, () => {
	console.log('🔒 Servidor HTTPS escuchando en https://localhost:4200');
});
