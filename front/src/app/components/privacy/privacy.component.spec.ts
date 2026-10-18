import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PrivacyComponent } from './privacy.component';

describe('PrivacyComponent', () => {
	let component: PrivacyComponent;
	let fixture: ComponentFixture<PrivacyComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [PrivacyComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(PrivacyComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create the privacy component', () => {
		// Arrange & Act & Assert
		expect(component).toBeTruthy();
	});

	it('should render privacy policy header and relevant legal sections in template', () => {
		// Arrange
		const compiled = fixture.nativeElement as HTMLElement;
		const header = compiled.querySelector('h2');
		const subheadings = compiled.querySelectorAll('h4');

		// Act & Assert
		expect(header?.textContent).toContain('Política de Privacidad');
		expect(subheadings.length).toBeGreaterThanOrEqual(7);
		expect(compiled.textContent).toContain('Reglamento General de Protección de Datos (R.G.P.D.)');
		expect(compiled.textContent).toContain('Medidas de Seguridad');
	});
});
