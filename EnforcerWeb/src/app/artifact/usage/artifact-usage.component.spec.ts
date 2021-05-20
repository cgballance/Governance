import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArtifactUsageComponent } from './artifact-usage.component';

describe('ArtifactUsaqgeComponent', () => {
  let component: ArtifactUsageComponent;
  let fixture: ComponentFixture<ArtifactUsageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ArtifactUsageComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ArtifactUsageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
