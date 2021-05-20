import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from '../../auth.service';
import { NGEventHubService} from '../../ngevent-hub.service' ;
import { Project } from '../../model/Project';
import { Artifact } from '../../model/Artifact';
import { Error } from '../../model/Error' ;
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers';
import { RepositionScrollStrategy } from "@angular/cdk/overlay";

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-artifact-artifacts',
	templateUrl: './artifact-artifacts.component.html',
	styleUrls: [ './artifact-artifacts.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class ArtifactArtifactsComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public project: Project ;
	public artifact: Artifact ;
	public artifactEditor: Artifact;
	public rowNode: RowNode;
	isShowForm = true;
	loginEventSub = null;
	artifactChangeEventSub = null;
	statusToGet = null;

	constructor( 
		private _authService: AuthService,
		private _ngEventHubService: NGEventHubService,
		private dialog: MatDialog  ) {
		this.gridOptions = <GridOptions>{
			headerHeight:30,
			rowHeight:30,
			defaultColDef: {
				sortable: true,
				resizable: true,
				filter: 'agTextColumnFilter',
			},
			statusBar: {
				statusPanels: [
					{
						statusPanel: 'agTotalRowCountComponent',
						align: 'left',
					}
				]
			},
		};
		this.columnDefs = [
			{headerName: "Artifact Id", field: "artifact_id", hide: true},
			{headerName: "Status", field: "status"},
			{headerName: "Group Name", field: "group_name"},
			{headerName: "Artifact Name", field: "artifact_name"},
			{headerName: "Version Name", field: "version_name"},
			{headerName: "Vendor License", field: "is_vendor_licensed"},
			{headerName: "Created Date", field: "created_date"},
			{headerName: "Approval Date", field: "approval_date"},
			{headerName: "Approval Authorization", field: "approval_authorization"},
			{headerName: "Approval Timestamp", field: "approval_ts"},
			{headerName: "Deprecation Date", field: "deprecation_date"},
			{headerName: "Deprecation Authorization", field: "deprecation_authorization"},
			{headerName: "Deprecation Timestamp", field: "deprecation_ts"},
			{headerName: "Retirement Date", field: "retirement_date"},
			{headerName: "Retirement Authorization", field: "retirement_authorization"},
			{headerName: "Retirement Timestamp", field: "retirement_ts"},
		];
		this.rowData = [];
		this.emptyArtifact();
	}

  	ngOnInit(): void {
		console.log( "Ok, artifact-artifacts.component will subscribe to login" ) ;
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "artifact-artifacts.component received LoginEvent" ) ;
		}) ;
		
		this.artifactChangeEventSub = this._ngEventHubService.ArtifactChangedEvent.subscribe( response => {
			let newartifact = response;
			if( newartifact.artifact_id === this.artifact.artifact_id ) {
				// TODO if changed from what i think it is, then substitute.
				console.log( "TODO - artifact-artifacts.component the current artifact has changed?" ) ;
			}
		} ) ;
  	}

  	ngOnDestroy(): void {
		if( this.loginEventSub ) {
			this.loginEventSub.unsubscribe();
		}
		if( this.artifactChangeEventSub ) {
			this.artifactChangeEventSub.unsubscribe();
		}
	}

	toggleForm(): void {
		this.isShowForm = !this.isShowForm;
	}

	onRowSelected(event): void {
		//console.log("row " + JSON.stringify(event.node.data) + " selected = " + event.node.selected);
		if( event.node.selected ) {
			this.artifact = event.node.data;
			this.rowNode = event.node;
			this._ngEventHubService.ArtifactArtifactSelectionEvent.emit( this.artifact );
		} else {
			if( event.node.data.artifact_id == this.artifact.artifact_id ) {
				this.emptyArtifact();
				this._ngEventHubService.ArtifactArtifactSelectionEvent.emit( this.artifact );
			}
		}
		this.artifactEditor = helpers.clone(this.artifact);
	}

	private emptyArtifact() {
		this.artifact = new Artifact() ;
		this.artifactEditor = helpers.clone( this.artifact );
		this.rowNode = null;
	}

	public async search() {
		this.emptyArtifact();
		this.gridOptions.api.setColumnDefs(this.columnDefs);
		this.loadArtifactsWithStatus(this.statusToGet) ;
		this._ngEventHubService.ArtifactArtifactSelectionEvent.emit( this.artifact );
	}

	private async loadArtifactsWithStatus(status: string) {
		try {
			status = status.toUpperCase();
			let url = environment.apiUrl + '/artifacts/findByStatus?status=' + status;
			console.log("Calling " + url);
			let bearerToken = this._authService.getToken();
			let response:Response = await fetch(url, {
				headers: {
					Accept: 'application/json',
					Authorization: 'Bearer ' + bearerToken
				},
				method: 'GET',
				mode: 'cors',
				credentials: "include"
			});

			console.log("ok to get the data? " + response.status);
			if (response.status == 200) {
				let data: Artifact[] = await response.json();
				console.log("artifacts.component::Retrieved Artifact[] # of rows: " + data.length);
				this.rowData = data;
				this.gridOptions.api.setRowData(this.rowData);
			} else {
				this.rowData = [];
				// set error message....
				let error: Error = await response.json();
				console.log("GOT an http error " + response.status + " with " +  error.detail );
				helpers.showMessage( this, this.dialog, 'Data Load Failed', 'status code: ' +
						response.status + ' ' + error.detail ) ;
			}
		} catch( error ) {
			helpers.showMessage( this, this.dialog, 'Data Load Failed', 'Exception: ' +  JSON.stringify(error) ) ;
		}
	}

	preUpdateArtifact() {
		var options ;
		
		if( helpers.areEqual(this.artifact,this.artifactEditor) ) {
			return;
		}
		options = [
			{
				'callback': this.updateArtifact,
				'text': "Yes"
			},
			{
				'callback': function(){},
				'text': "No"
			}
		] ;
		let msg ;
		if( this.artifact.is_vendor_licensed !== this.artifactEditor.is_vendor_licensed ) {
			msg = "Confirm Update! WARNING: changing 'vendor license' will move records between entities. " +
				"Information will be discarded as appropriate." ;
		} else {
			msg = "Confirm Update." ;
		}
		helpers.showMessage( this, this.dialog, "Confirmation", msg, options ) ;
	}

	async updateArtifact() {

		//
		// disable button, re-enable is handled after user changes a form value.
		//
		try {
			let url = environment.apiUrl + '/artifacts';
			console.log("Calling " + url);
			let bearerToken = this._authService.getToken();
			let response:Response = await fetch(url, {
				headers: {
					'Content-Type': 'application/json',
					Accept: 'application/json',
					Authorization: 'Bearer ' + bearerToken
				},
				method: 'PUT',
				mode: 'cors',
				credentials: "include",
				body: JSON.stringify( this.artifactEditor )
			});

			console.log("updateArtifact status: " + response.status);
			if( response.status !== 200 ) {
				let data: Error = await response.json();
				console.log( "updateArtifact results: " + data.status + ' ' + data.detail );
				helpers.showMessage( this, this.dialog, 'Update Failed', 'status code: ' + data.status + ' ' + data.detail ) ;
			} else {
				let data: Artifact = await response.json() ;
				helpers.copy( this.artifactEditor, data ) ;
				helpers.copy( this.artifact, this.artifactEditor ) ;
				this.rowNode.setData(this.artifact);
				this.gridOptions.api.applyTransaction({ update: [this.rowNode.data] });
				//this.gridOptions.api.redrawRows( { rowNodes: [this.rowNode] } ) ;
				this._ngEventHubService.ArtifactChangedEvent.emit( this.artifact );
				helpers.showMessage( this, this.dialog, 'Artifact Updated', 'Done' ) ;
			}
			//
			// provide user feedback either way...
			//
		} catch (error) {
			console.log( 'Update Artifact failed: ' + JSON.stringify(error) ) ;
			helpers.showMessage( this, this.dialog, 'Update Artifact Failed', JSON.stringify(error) ) ;
		} finally {
		}
	}

	preCreateArtifact() {
		// just for safety...
		var options = [
			{
				'callback': this.createArtifact,
				'text': "Yes"
			},
			{
				'callback': function(){},
				'text': "No"
			}
		] ;
		let msg = "Confirm Creation";
		helpers.showMessage( this, this.dialog, "Confirmation", msg, options ) ;
	}

	async createArtifact() {
		try {
			let url = environment.apiUrl + '/artifacts' ;
			let obj: Artifact = new Artifact();
			obj = helpers.copy( obj, this.artifactEditor ) ;
			obj.artifact_id = null;
			console.log("Calling " + url);
			let bearerToken = this._authService.getToken();
			let response:Response = await fetch(url, {
				headers: {
					'Content-Type': 'application/json',
					Accept: 'application/json',
					Authorization: 'Bearer ' + bearerToken
				},
				method: 'POST',
				mode: 'cors',
				body: JSON.stringify( obj ),
				credentials: "include"
			});

			console.log("createArtifact status: " + response.status);
			if (response.status !== 201) {
				let data: Error = await response.json();
				console.log("createArtifact results: " + data.status + ' ' + data.detail );
				helpers.showMessage(this, this.dialog, 'CREATE Artifact Failed', 'status code: ' + response.status + ' ' + JSON.stringify(data));
			} else {
				let art:Artifact = await response.json();
				//
				// put into the table if the query type matches the status of the created object.
				//
				if( this.statusToGet == obj.status ) {
					this.gridOptions.api.applyTransaction({ add: [art] });
				}
				this._ngEventHubService.ArtifactCreatedEvent.emit(art);
				helpers.showMessage(this, this.dialog, 'Artifact CREATED', 'Done');
			}
		} catch (error) {
			console.log('CREATE Artifact failed: ' + JSON.stringify(error));
			helpers.showMessage(this, this.dialog, 'CREATE Artifact Failed', JSON.stringify(error));
		} finally {
		}
	}

	preDeleteArtifact() {
		//
		// Who is using this artifact currently?
		// show confirmation dialog with some into on how many projects reference this thing?
		//
		var options = [
			{
				'callback': this.deleteArtifact,
				'text': "Yes"
			},
			{
				'callback': function(){},
				'text': "No"
			}
		] ;
		let msg = "Confirm Deletion";
		helpers.showMessage( this, this.dialog, "Confirmation", msg, options ) ;
	}

	async deleteArtifact() {
		try {
			let url = environment.apiUrl + '/artifacts/' + this.artifactEditor.artifact_id;
			console.log("Calling " + url);
			let bearerToken = this._authService.getToken();
			let response = await fetch(url, {
				headers: {
					'Content-Type': 'application/json',
					Accept: 'application/json',
					Authorization: 'Bearer ' + bearerToken
				},
				method: 'DELETE',
				mode: 'cors',
				credentials: "include"
			});

			console.log("deleteArtifact status: " + response.status);
			if( response.status !== 200 ) {
				let data : Error = await response.json();
				console.log( "deleteArtifact results: " + JSON.stringify(data) );
				helpers.showMessage( this, this.dialog, 'Delete Failed', 'status code: ' + response.status + ' ' + data.detail ) ;
			} else {
				this.gridOptions.api.applyTransaction({ remove: [this.rowNode.data] });
				let thing = new Artifact();
				helpers.copy( thing, this.artifact ) ;
				this._ngEventHubService.ArtifactDeletedEvent.emit( thing );
				helpers.showMessage( this, this.dialog, 'Artifact Deleted', 'Done' ) ;
			}
		} catch (error) {
			console.log( 'Delete Artifact failed: ' + JSON.stringify(error) ) ;
			helpers.showMessage( this, this.dialog, 'Delete Artifact Failed', JSON.stringify(error) ) ;
		} finally {
		}
	}
}
