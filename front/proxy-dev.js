const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();

app.use(
	'/',
	createProxyMiddleware({
		target: 'https://localhost:4200',
		changeOrigin: true,
		secure: false,
	}),
);

app.listen(4201, () => {
	console.log('🌍 Proxy HTTP en http://localhost:4201 -> https://localhost:4200');
});
