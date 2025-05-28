import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditorVideoComponent } from './editor-video.component';

describe('EditorVideoComponent', () => {
  let component: EditorVideoComponent;
  let fixture: ComponentFixture<EditorVideoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditorVideoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditorVideoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
