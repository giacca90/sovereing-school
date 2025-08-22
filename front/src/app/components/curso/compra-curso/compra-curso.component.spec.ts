import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CompraCursoComponent } from './compra-curso.component';

describe('CompraCursoComponent', () => {
  let component: CompraCursoComponent;
  let fixture: ComponentFixture<CompraCursoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompraCursoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CompraCursoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
