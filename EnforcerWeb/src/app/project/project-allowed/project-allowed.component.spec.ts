import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectAllowedComponent } from './project-allowed.component';

describe('ProjectAllowedComponent', () => {
  let component: ProjectAllowedComponent;
  let fixture: ComponentFixture<ProjectAllowedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectAllowedComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectAllowedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
