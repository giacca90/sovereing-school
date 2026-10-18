import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CompraCursoComponent } from './compra-curso.component';

describe('CompraCursoComponent', () => {
	let component: CompraCursoComponent;
	let fixture: ComponentFixture<CompraCursoComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [CompraCursoComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(CompraCursoComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create the component', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
	});

	it('should render informational message in the template', () => {
		// Arrange & Act
		const compiled = fixture.nativeElement as HTMLElement;
		const headings = compiled.querySelectorAll('h2');

		// Assert
		expect(headings.length).toBe(3);
		expect(headings[0].textContent).toContain('Por el momento, no es posible comprar los cursos de pago.');
		expect(headings[1].textContent).toContain('Esta pagina web es solo de prueba.');
		expect(headings[2].textContent).toContain('Disculpen las molestias.');
	});
});
