import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
	selector: 'app-privacy',
	standalone: true,
	imports: [CommonModule],
	templateUrl: './privacy.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './privacy.component.css',
})
export class PrivacyComponent {}
