import { TestBed } from '@angular/core/testing';

import { NGEventHubService } from './ngevent-hub.service';

describe('NGEventHubService', () => {
  let service: NGEventHubService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NGEventHubService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
