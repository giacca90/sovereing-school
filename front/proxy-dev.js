// proxy-dev.js
import express from 'express';
import { createProxyMiddleware } from 'http-proxy-middleware';

const app = express();

app.use(
	'/',
	createProxyMiddleware({
		target: 'https://localhost:4200', // tu ng serve
		changeOrigin: true,
		secure: false, // aceptar self-signed cert
	}),
);

app.listen(4201, () => {
	console.log('🌍 Proxy HTTP en http://localhost:4201 -> https://localhost:4200');
});
