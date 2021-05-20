import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArtifactArtifactsComponent } from './artifact-artifacts.component';

describe('ProjectArtifactsComponent', () => {
  let component: ArtifactArtifactsComponent;
  let fixture: ComponentFixture<ArtifactArtifactsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ArtifactArtifactsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ArtifactArtifactsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
