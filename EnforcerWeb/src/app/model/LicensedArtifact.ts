export class LicensedArtifact {
	public licensed_artifact_id: number;
	public project_id: number;
	public artifact_id: number;
	public contract?: string;
	public vendor?: string;
	public approval_architect?: string;
	public approval_ts?: Date;

	constructor( obj: Object = {}) {
		Object.assign(this, obj) ;
	}
 
	public toJSON() {
		return {
			licensed_artifact_id: this.licensed_artifact_id,
			project_id: this.project_id,
			artifact_id: this.artifact_id,
			contract: this.contract,
			vendor: this.vendor,
			approval_architect: this.approval_architect,
			approval_ts: this.approval_ts,
		}
	}
}