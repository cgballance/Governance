export class Error {
	public type: string;
	public title: string;
	public status: number;
	public detail: string;
	
	constructor( obj: Object = {}) {
		Object.assign(this, obj) ;
	}
 
	public toJSON() {
		return {
			type: this.type,
			title: this.title,
			status: this.status,
			detail: this.detail
		}
	}
}