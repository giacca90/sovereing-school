import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditorClaseComponent } from './editor-clase.component';

describe('EditorClaseComponent', () => {
  let component: EditorClaseComponent;
  let fixture: ComponentFixture<EditorClaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditorClaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditorClaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
