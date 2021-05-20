export class Component {
	public component_id: number;
	public project_id: number;
	public name: string;

	constructor( obj: Object = {}) {
		Object.assign(this, obj) ;
	}
 
	public toJSON() {
		return {
			component_id: this.component_id,
			project_id: this.project_id,
			name: this.name,
		}
	}
}