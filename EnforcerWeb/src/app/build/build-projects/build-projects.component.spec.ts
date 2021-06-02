import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BuildProjectsComponent } from './build-projects.component';

describe('BuildProjectsComponent', () => {
  let component: BuildProjectsComponent;
  let fixture: ComponentFixture<BuildProjectsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BuildProjectsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BuildProjectsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
