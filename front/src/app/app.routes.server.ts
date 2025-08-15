import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
	{
		path: '',
		renderMode: RenderMode.Server,
	},
	{
		path: 'privacy',
		renderMode: RenderMode.Prerender,
	},
	{
		path: 'confirm-email',
		renderMode: RenderMode.Prerender,
	},
	{
		path: 'curso/:id_curso',
		renderMode: RenderMode.Client,
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
		path: '**',
		renderMode: RenderMode.Server,
	},
];

export default serverRoutes;
