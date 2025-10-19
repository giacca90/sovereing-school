import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CursosService } from '../../services/cursos.service';
import { LoginService } from '../../services/login.service';

@Component({
	selector: 'app-cursos-usuario',
	standalone: true,
	imports: [],
	templateUrl: './cursos-usuario.component.html',
	styleUrl: './cursos-usuario.component.css',
})
export class CursosUsuarioComponent implements OnInit {
	constructor(
		public loginService: LoginService,
		public cursoService: CursosService,
		private readonly cdr: ChangeDetectorRef,
		public router: Router,
	) {}

	ngOnInit(): void {
		this.cdr.detectChanges();
	}
}
