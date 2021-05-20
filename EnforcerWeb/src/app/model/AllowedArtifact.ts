export class AllowedArtifact {
	public allowed_artifact_id: number;
	public project_id: number;
	public artifact_id: number;
	public approval_architect?: string;
	public approval_ts?: Date;

	constructor( obj: Object = {}) {
		Object.assign(this, obj) ;
	}
 
	public toJSON() {
		return {
			allowed_artifact_id: this.allowed_artifact_id,
			project_id: this.project_id,
			artifact_id: this.artifact_id,
			approval_architect: this.approval_architect,
			approval_ts: this.approval_ts,
		}
	}
}