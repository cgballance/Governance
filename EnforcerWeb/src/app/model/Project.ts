export class Project {
	public project_id: number;
	public acronym: string;
	public business_owner?: string;
	public it_owner?: string;
	public begin_date?: Date;
	public end_date?: Date;

	constructor( obj: Object = {}) {
		Object.assign(this, obj) ;
	}
 
	public toJSON() {
		return {
			project_id: this.project_id,
			acronym: this.acronym,
			business_owner: this.business_owner,
			it_owner: this.it_owner,
			begin_date: this.begin_date,
			end_date: this.end_date
		}
	}
}