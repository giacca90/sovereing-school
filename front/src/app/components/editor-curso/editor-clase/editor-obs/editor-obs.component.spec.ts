import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditorObsComponent } from './editor-obs.component';

describe('EditorObsComponent', () => {
  let component: EditorObsComponent;
  let fixture: ComponentFixture<EditorObsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditorObsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditorObsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
