import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
	{
		path: '', // Página principal renderizada en el cliente
		renderMode: RenderMode.Server,
	},
	{
		path: 'privacy', // Páginas estáticas que se pueden prerender
		renderMode: RenderMode.Prerender,
	},
	{
		path: 'confirm-email',
		renderMode: RenderMode.Prerender,
	},
	{
		path: 'curso/:id_curso', // Páginas públicas con datos dinámicos
		renderMode: RenderMode.Client, // Cambiado a Server en lugar de Prerender
	},
	{
		path: 'chat/:id_curso',
		renderMode: RenderMode.Client,
	},
	{
		path: 'chat/:id_curso/:id_mensaje',
		renderMode: RenderMode.Client,
	},
	{
		path: 'repro/:id_curso/:id_clase',
		renderMode: RenderMode.Client,
	},
	{
		path: 'editorCurso/:id_curso',
		renderMode: RenderMode.Client,
	},
	{
		path: 'cursosUsuario',
		renderMode: RenderMode.Client,
	},
	{
		path: 'tus-chat',
		renderMode: RenderMode.Client,
	},
	{
		path: 'perfil',
		renderMode: RenderMode.Client,
	},
	{
		path: 'administracion',
		renderMode: RenderMode.Client,
	},
	{
		path: '**', // Todas las demás rutas se renderizan en el servidor
		renderMode: RenderMode.Server,
	},
];

export default serverRoutes;
