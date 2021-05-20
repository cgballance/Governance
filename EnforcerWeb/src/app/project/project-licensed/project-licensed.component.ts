import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from '../../auth.service';
import { EventEmitter } from '@angular/core';
import { NGEventHubService } from "../../ngevent-hub.service";
import { Project } from '../../model/Project';
import { LicensedArtifact } from '../../model/LicensedArtifact';
import { Error } from '../../model/Error' ;
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers';

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-project-licensed',
	templateUrl: './project-licensed.component.html',
	styleUrls: [ './project-licensed.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class ProjectLicensedComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public project: Project ;
	public licensed: LicensedArtifact ;
	public licensedEditor: LicensedArtifact;
	public rowNode: RowNode;
	isShowForm = true;
	loginEventSub = null;
	projectEventSub = null;
	artifactEventSub = null;

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
			//onGridReady: () => {
			//	this.gridOptions.api.sizeColumnsToFit();
			//}
		};
		this.columnDefs = [
			{headerName: "License Id", field: "lic_artifact_id", hide: true},
			{headerName: "Artifact Id", field: "artifact_id", hide: true},
			{headerName: "Project Id", field: "project_id", hide: true},
			{headerName: "Contract", field: "contract"},
			{headerName: "Vendor", field: "vendor"},
			{ headerName: "Approval Architect", field: "approval_architect" },
			{ headerName: "Approval Timestamp", field: "approval_ts" },
		];
		this.rowData = [];
		this.emptyProject();
		this.emptyLicensed();
	}

  	ngOnInit(): void {
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "project-licensed.component received LoginEvent" ) ;
		}) ;
		this.projectEventSub = this._ngEventHubService.ProjectSelectionEvent.subscribe( response => {
			console.log( "project-licensed.component received project event. "  + JSON.stringify(response) ) ;
			this.project = response;
			this.emptyLicensed();
			this.rowData = [] ;
			if( this.project.project_id !== undefined ) {
				// load up the artifacts for this project!
				this.loadLicensedArtifactsForProject( this.project ) ;
			}
		}) ;

		this.artifactEventSub = this._ngEventHubService.ProjectArtifactSelectionEvent.subscribe( art => {
			// Select/Unselect based upon artifact
			if( art.is_vendor_licensed === false ) {
				this.gridOptions.api.deselectAll();
			} else {
				this.gridOptions.api.forEachNode( node => {
					if( node.data.artifact_id === art.artifact_id ) {
						node.setSelected(true);
					} else {
						if( node.isSelected ) {
							node.setSelected(false);
						}
					}
				} ) ;
			}
		}) ;
  	}

	ngOnDestroy(): void {
		if( this.loginEventSub ) {
			this.loginEventSub.unsubscribe();
		  }
		  if( this.projectEventSub ) {
			this.projectEventSub.unsubscribe();
		  }
		  if( this.artifactEventSub ) {
			  this.artifactEventSub.unsubscribe();
		  }
	}

	toggleForm(): void {
		this.isShowForm = !this.isShowForm;
	}

	onRowSelected(event): void {
		//console.log("row " + JSON.stringify(event.node.data) + " selected = " + event.node.selected);
		if( event.node.selected ) {
			this.licensed = event.node.data;
			this.rowNode = event.node;
		} else if( event.node.data.licensed_artifact_id == this.licensed.licensed_artifact_id ) {
			this.emptyLicensed();
		}
		this.licensedEditor = helpers.clone(this.licensed);
	}

	public emptyProject() {
		this.project = new Project()
	}

	public emptyLicensed() {
		this.licensed = new LicensedArtifact();
		this.licensedEditor = helpers.clone( this.licensed );
		this.rowNode = null;
	}

	preUpdateLicensedArtifact() {
		var options ;
		
		if( helpers.areEqual(this.licensed,this.licensedEditor) ) {
			return;
		}
		
		options = [
			{
				'callback': this.updateLicensedArtifact,
				'text': "Yes"
			},
			{
				'callback': function(){},
				'text': "No"
			}
		] ;
		let msg = "Confirm Update of Licensed Artifact for " + this.project.acronym; // point to original name...may have changed
		helpers.showMessage( this, this.dialog, "Confirmation", msg, options ) ;
	}

	async updateLicensedArtifact() {
		//
		// disable button, re-enable is handled after user changes a form value.
		//
		console.log( "Updating license with: " + JSON.stringify(this.licensedEditor) ) ;

		try {
			let url = environment.apiUrl + '/licensedartifacts';
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
				body: JSON.stringify( this.licensedEditor )
			});

			console.log("updateLicensedArtifact status: " + response.status);
			if( response.status !== 200 ) {
				let data:Error = await response.json();
				console.log( "updateLicensedArtifact results: " + data.detail );
				helpers.showMessage( this, this.dialog, 'Licensed Artifact Update Failed',
					'status code: ' + response.status + ' ' + data.detail ) ;
			} else {
				let data:LicensedArtifact = await response.json();
				helpers.copy( this.licensedEditor, data ) ;
				helpers.copy(this.licensed, this.licensedEditor) ;
				this.rowNode.setData(this.licensed); // repaint
				let updatedNodes:RowNode[] = [this.rowNode] ;
				this.gridOptions.api.redrawRows( {rowNodes: updatedNodes} );
				//this._ngEventHubService.ProjectChangedEvent.emit( this.project );
				helpers.showMessage( this, this.dialog, 'License Update', 'Done' ) ;
			}
			//
			// provide user feedback either way...
			//
		} catch (error) {
			console.log( 'Update of Licensed Artifact failed: ' + JSON.stringify(error) ) ;
			helpers.showMessage( this, this.dialog, 'Update of Licensed Artifact Failed', JSON.stringify(error) ) ;
		} finally {
		}
	}

	private async loadLicensedArtifactsForProject( project: Project ) {
		try {
			let url = environment.apiUrl + '/licensedartifacts/findByProjectId?projectId=' + project.project_id;
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
			if (response.status === 200) {
				let data:LicensedArtifact[] = await response.json();
				console.log("Retrieved data # of rows: " + data.length);
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
		} catch (error) {
			helpers.showMessage( this, this.dialog, 'Data Load Failed', 'Exception: ' +  JSON.stringify(error) ) ;
		}
	}
}
