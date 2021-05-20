import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectArtifactsComponent } from './project-artifacts.component';

describe('ProjectArtifactsComponent', () => {
  let component: ProjectArtifactsComponent;
  let fixture: ComponentFixture<ProjectArtifactsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectArtifactsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectArtifactsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
