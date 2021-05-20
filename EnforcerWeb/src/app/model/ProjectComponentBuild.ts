import { Project } from './Project' ;
import { Component } from './Component' ;
import { Build } from './Build' ;

export class ProjectComponentBuild {
	public project: Project;
	public component: Component;
	public build: Build;

	constructor( obj: Object = {}) {
		this.project = new Project();
		this.component = new Component();
		this.build = new Build();
		Object.assign(this, obj) ;
	}
 
	public toJSON() {
		return {
			project: JSON.stringify(this.project),
			component: JSON.stringify(this.component),
			build: JSON.stringify(this.build)
		}
	}
}