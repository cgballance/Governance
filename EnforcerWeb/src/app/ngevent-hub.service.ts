import { Injectable } from '@angular/core';
import { EventEmitter } from '@angular/core';
import { Project } from './model/Project' ;
import { Artifact } from './model/Artifact' ;

//
// Nothing complicated here. The implementation is a placeholder for something more attractive
// later...TODO...
//

@Injectable({
  providedIn: 'root'
})
export class NGEventHubService {
  
  LoginEvent = new EventEmitter() ;
  
  //
  // The selection events are generally used to deal with cascading
  // view dependencies.
  //
  ProjectSelectionEvent = new EventEmitter<Project>() ;
  ProjectArtifactSelectionEvent = new EventEmitter<Artifact>();
  BuildProjectSelectionEvent = new EventEmitter() ;
  BuildComponentSelectionEvent = new EventEmitter() ;
  BuildBuildSelectionEvent = new EventEmitter();
  BuildItemSelectionEvent = new EventEmitter();
  ArtifactArtifactSelectionEvent = new EventEmitter<Artifact>();
  ArtifactUsageSelectionEvent = new EventEmitter<Artifact>();

  //
  // The values of an Object are changed and you MAY be interested in that.
  // The event will emit the current object state.
  //
  ArtifactChangedEvent = new EventEmitter<Artifact>();
  ArtifactDeletedEvent = new EventEmitter<Artifact>();
  ArtifactCreatedEvent = new EventEmitter<Artifact>();
  ProjectCreatedEvent = new EventEmitter<Project>() ;
  ProjectChangedEvent = new EventEmitter<Project>();
  ProjectDeletedEvent = new EventEmitter<Project>();
  
  constructor() {
   }
}
