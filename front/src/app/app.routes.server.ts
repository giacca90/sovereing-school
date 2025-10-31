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
		path: 'curso/:idCurso',
		renderMode: RenderMode.Client,
	},
	{
		path: 'chat/:idCurso',
		renderMode: RenderMode.Client,
	},
	{
		path: 'chat/:idCurso/:id_mensaje',
		renderMode: RenderMode.Client,
	},
	{
		path: 'repro/:idCurso/:idClase',
		renderMode: RenderMode.Client,
	},
	{
		path: 'editorCurso/:idCurso',
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
