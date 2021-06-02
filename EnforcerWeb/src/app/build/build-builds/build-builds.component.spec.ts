import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BuildBuildsComponent } from './build-builds.component';

describe('BuildBuildsComponent', () => {
  let component: BuildBuildsComponent;
  let fixture: ComponentFixture<BuildBuildsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BuildBuildsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BuildBuildsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
