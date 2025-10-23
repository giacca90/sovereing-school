import { Routes } from '@angular/router';
import { GuestGuard } from './guard/guest.guard';
import { PendingChangesGuard } from './guard/pending-changes.guard';
import { ProfGuard } from './guard/prof.guard';
import { UserGuard } from './guard/user.guard';

export const routes: Routes = [
	{
		path: '',
		loadComponent: () => import('./components/home/home.component').then((m) => m.HomeComponent),
	},
	{
		path: 'cursosUsuario',
		loadComponent: () => import('./components/cursos-usuario/cursos-usuario.component').then((m) => m.CursosUsuarioComponent),
		canActivate: [GuestGuard],
	},
	{
		path: 'curso/:id_curso',
		loadComponent: () => import('./components/curso/curso.component').then((m) => m.CursoComponent),
	},
	{
		path: 'tus-chat',
		loadComponent: () => import('./components/chat/home-chat/home-chat.component').then((m) => m.HomeChatComponent),
		canActivate: [GuestGuard],
	},
	{
		path: 'chat/:id_curso',
		loadComponent: () => import('./components/chat/chat/chat.component').then((m) => m.ChatComponent),
		canActivate: [GuestGuard, UserGuard],
	},
	{
		path: 'chat/:id_curso/:id_mensaje',
		loadComponent: () => import('./components/chat/chat/chat.component').then((m) => m.ChatComponent),
		canActivate: [GuestGuard, UserGuard],
	},
	{
		path: 'repro/:id_curso/:id_clase',
		loadComponent: () => import('./components/reproduction/reproduction.component').then((m) => m.ReproductionComponent),
		canActivate: [GuestGuard, UserGuard],
	},
	{
		path: 'editorCurso/:id_curso',
		loadComponent: () => import('./components/editor-curso/editor-curso.component').then((m) => m.EditorCursoComponent),
		canActivate: [GuestGuard, ProfGuard],
		canDeactivate: [PendingChangesGuard],
	},
	{
		path: 'perfil',
		loadComponent: () => import('./components/perfil-usuario/perfil-usuario.component').then((m) => m.PerfilUsuarioComponent),
		canActivate: [GuestGuard],
	},
	{
		path: 'privacy',
		loadComponent: () => import('./components/privacy/privacy.component').then((m) => m.PrivacyComponent),
	},
	{
		path: 'confirm-email',
		loadComponent: () => import('./components/confirm-email/confirm-email.component').then((m) => m.ConfirmEmailComponent),
	},
	{
		path: 'administracion',
		loadComponent: () => import('./components/administracion/administracion.component').then((m) => m.AdministracionComponent),
	},
	{
		path: '**',
		redirectTo: '',
	},
];
