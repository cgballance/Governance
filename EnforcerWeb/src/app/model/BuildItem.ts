export class BuildItem {
	public builditem_id: number;
	public build_id: number;
	public group_name: string;
	public artifact_name: string;
	public version_name: string;
	public artifact_status_snapshot: string;
	public allowed: boolean;

	constructor( obj: Object = {}) {
		Object.assign(this, obj) ;
	}
 
	public toJSON() {
		return {
			builditem_id: this.builditem_id,
			build_id: this.build_id,
			group_name: this.group_name,
			artifact_name: this.artifact_name,
			version_name: this.version_name,
			artifact_status_snapshot: this.artifact_status_snapshot,
			allowed: this.allowed
		}
	}
}