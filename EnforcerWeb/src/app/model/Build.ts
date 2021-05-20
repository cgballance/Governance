export class Build {
	public build_id: number;
	public build_ts: Date;
	public project_id: number;
	public component_id: number;
	public component_version: string;
	public infractions: string;
	public source: string;

	constructor( obj: Object = {}) {
		Object.assign(this, obj) ;
	}
 
	public toJSON() {
		return {
			build_id: this.build_id,
			build_ts: this.build_ts,
			project_id: this.project_id,
			component_id: this.component_id,
			component_version: this.component_version,
			source: this.source
		}
	}
}