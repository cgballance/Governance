import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectLicensedComponent } from './project-licensed.component';

describe('ProjectLicensedComponent', () => {
  let component: ProjectLicensedComponent;
  let fixture: ComponentFixture<ProjectLicensedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectLicensedComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectLicensedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
