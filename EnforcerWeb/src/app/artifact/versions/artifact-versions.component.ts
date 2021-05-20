import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from '../../auth.service';
import { NGEventHubService} from '../../ngevent-hub.service' ;
import { Artifact } from '../../model/Artifact' ;
import { Error } from '../../model/Error';
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers' ;

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-artifact-versions',
	templateUrl: './artifact-versions.component.html',
	styleUrls: [ './artifact-versions.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class ArtifactVersionsComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public artifact: Artifact ;
	public artifactEditor: Artifact;
	public rowNode: RowNode;
	isShowForm: boolean = true;
	loginEventSub = null;
	artifactArtifactEventSub = null;
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
		console.log( "Ok, artifact-versions.component will subscribe to LoginEvent" ) ;
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "artifact-versions.component received LoginEvent" ) ;
		}) ;
		console.log( "Ok, artifact-versions.component will subscribe to ArtifactArtifactSelectionEvent" ) ;
		this.artifactArtifactEventSub = this._ngEventHubService.ArtifactArtifactSelectionEvent.subscribe( response => {
			console.log( "artifact-versions.component received ArtifactArtifactSelectionEvent" ) ;
			this.artifact = response;
			if( this.artifact.artifact_id !== undefined ) {
				// GET ALL ARTIFACTS FOR THIS group_name, component_name
				this.loadArtifactVersions(this.artifact);
			} else {
				this.rowData = [];
			}
			this.emptyArtifact();
		}) ;
		this.artifactChangeEventSub = this._ngEventHubService.ArtifactChangedEvent.subscribe( response => {
			let newartifact = response;
			if( newartifact.artifact_id === this.artifact.artifact_id ) {
				// TODO if changed from what i think it is, then substitute.
				console.log( "TODO - artifact-versions.component the current artifact has changed?" ) ;
			}
		} ) ;
  	}

  	ngOnDestroy(): void {
		  if( this.loginEventSub ) {
			this.loginEventSub.unsubscribe();
		  }
		  if( this.artifactArtifactEventSub ) {
			this.artifactArtifactEventSub.unsubscribe();
		  }
		  if( this.artifactChangeEventSub ) {
			this.artifactChangeEventSub.unsubscribe();
		  }
	}

	toggleForm(): void {
		this.isShowForm = !this.isShowForm;
	}

	onRowSelected(event): void {
		console.log("row " + JSON.stringify(event.node.data) + " selected = " + event.node.selected);
		if( event.node.selected ) {
			this.artifact = event.node.data;
			this.rowNode = event.node;
		} else {
			if( event.node.data.artifact_id == this.artifactEditor.artifact_id ) {
				this.emptyArtifact();
			}
		}
		this.artifactEditor = helpers.clone(this.artifact);
	}

	private emptyArtifact() {
		this.artifact = new Artifact() ;
		this.artifactEditor = helpers.clone( this.artifact );
		this.rowNode = null;
	}

	private async loadArtifactVersions( artifact: Artifact ) {
		try {
			status = status.toUpperCase();
			let url = environment.apiUrl + '/artifacts/findByGroupNameAndArtifactName?groupName=' +
					artifact.group_name + '&artifactName=' + artifact.artifact_name ;
			url = encodeURI(url);
			console.log("Calling " + url);
			let bearerToken = this._authService.getToken();
			let response = await fetch(url, {
				headers: {
					Accept: 'application/json',
					Authorization: 'Bearer ' + bearerToken
				},
				method: 'GET',
				mode: 'cors',
				credentials: "include"
			});

			console.log("ok to get the data? " + response.status);
			if (response.status === 200) {
				let data = await response.json();
				console.log("artifact-usage.component::Retrieved data # of rows: " + data.length);
				//console.log(JSON.stringify(data));
				this.rowData = data;
				this.gridOptions.api.setRowData(this.rowData);
			} else {
				this.rowData = [];
				// set error message....
				let data:Error = await response.json();
				console.log("GOT an http error " + response.status + " with " + data.detail );
				helpers.showMessage( this, this.dialog, 'Data Load Failed', 'status code: ' +
						response.status + ' ' + data.detail ) ;
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
		if( this.artifact.is_vendor_licensed != this.artifactEditor.is_vendor_licensed ) {
			msg = "Confirm Update! WARNING: changing 'vendor license' will move records between entities. " +
				"Information will be lost if fields don't match." ;
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
			let response = await fetch(url, {
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
				let data:Error = await response.json();
				console.log( "updateArtifact results: " + data.detail );
				helpers.showMessage( this, this.dialog, 'Update Failed',
					'status code: ' + response.status + ' ' + data.detail ) ;
			} else {
				let data: Artifact = await response.json() ;
				helpers.copy( this.artifactEditor, data ) ;
				helpers.copy(this.artifact, this.artifactEditor) ;
				this.rowNode.setData(this.artifact);
				this.gridOptions.api.redrawRows( { rowNodes: [this.rowNode] } ) ;
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

	async createArtifact() {
		helpers.showMessage( this, this.dialog, 'Create an Artifact', 'Not Yet Implemented' ) ;
	}

	async deleteArtifact() {
		helpers.showMessage( this, this.dialog, 'Delete an Artifact', 'Not Yet Implemented' ) ;
	}
}
