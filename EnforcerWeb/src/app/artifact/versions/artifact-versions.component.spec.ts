import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArtifactVersionsComponent } from './artifact-versions.component';

describe('ArtifactVersionsComponent', () => {
  let component: ArtifactVersionsComponent;
  let fixture: ComponentFixture<ArtifactVersionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ArtifactVersionsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ArtifactVersionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
