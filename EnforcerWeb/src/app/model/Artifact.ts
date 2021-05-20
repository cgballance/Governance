export class Artifact {
	public artifact_id: number;
	public status: string;
	public group_name: string;
	public artifact_name: string;
	public version_name: string;
	public is_vendor_licensed: boolean;
	public created_date: Date;
	public approval_date?: Date;
	public approval_authorization?: string;
	public approval_ts?: Date;
	public deprecation_authorization?: string;
	public deprecation_date?: Date;
	public deprecation_ts?: Date;
	public retirement_authorization?: string;
	public retirement_date?: Date;
	public retirement_ts?: Date;

	constructor( obj: Object = {}) {
		Object.assign(this, obj) ;
	}
 
	public toJSON() {
		return {
			artifact_id: this.artifact_id,
			status: this.status,
			group_name: this.group_name,
			artifact_name: this.artifact_name,
			version_name: this.version_name,
			is_vendor_licensed: this.is_vendor_licensed,
			created_date: this.created_date,
			approval_date: this.approval_date,
			approval_authorization: this.approval_authorization,
			approval_ts: this.approval_ts,
			deprecation_authorization: this.deprecation_authorization,
			deprecation_date: this.deprecation_date,
			deprecation_ts: this.deprecation_ts,
			retirement_authorization: this.approval_authorization,
			retirement_date: this.retirement_date,
			retirement_ts: this.retirement_ts,
		}
	}
}