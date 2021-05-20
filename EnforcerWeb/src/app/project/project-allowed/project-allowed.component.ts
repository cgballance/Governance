import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from '../../auth.service';
import { EventEmitter } from '@angular/core';
import { NGEventHubService } from '../../ngevent-hub.service';
import { Project } from "../../model/Project" ;
import { AllowedArtifact } from '../../model/AllowedArtifact';
import { Error } from '../../model/Error' ;
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers';

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-project-allowed',
	templateUrl: './project-allowed.component.html',
	styleUrls: [ './project-allowed.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class ProjectAllowedComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public project: Project ;
	public allowed: AllowedArtifact ;
	public allowedEditor: AllowedArtifact;
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
		};
		this.columnDefs = [
			 {headerName: "Allowed Id", field: "allowed_artifact_id", hide: true },
			{ headerName: "Artifact Id", field: "artifact_id", hide: true },
			{ headerName: "Project Id", field: "project_id", hide: true },
			{ headerName: "Approval Architect", field: "approval_architect" },
			{ headerName: "Approval Timestamp", field: "approval_ts" },
		];
		this.rowData = [];
		this.emptyProject();
		this.emptyAllowed();
	}

  	ngOnInit(): void {
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "artifacts.component received LoginEvent" ) ;
		}) ;
		this.projectEventSub = this._ngEventHubService.ProjectSelectionEvent.subscribe( response => {
			console.log( "artifacts.component received project event. "  + JSON.stringify(response) ) ;
			this.project = response;
			if( this.project.project_id !== undefined ) {
				// load up the artifacts for this project!
				this.loadAllowedArtifactsForProject( this.project )
					.catch( reason => {
						alert( "loadAllowedArtifactsForProject failed: " + reason ) ;
						this.rowData = [] ;
						this.emptyAllowed();
					}) ;
			} else {
				this.rowData = [] ;
				this.emptyAllowed();
			}
		}) ;
		
		this.artifactEventSub = this._ngEventHubService.ProjectArtifactSelectionEvent.subscribe( art => {
			// Select/Unselect based upon artifact
			if( art.is_vendor_licensed === true ) {
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
			this.allowed = event.node.data;
			this.rowNode = event.node;
		} else {
			if( this.allowed.allowed_artifact_id == event.node.data.allowed_artifact_id ) {
				this.emptyAllowed();
			}
		}
		this.allowedEditor = helpers.clone(this.allowed);
	}

	public emptyProject() {
		this.project = new Project() ;
	}

	public emptyAllowed() {
		this.allowed = new AllowedArtifact();
		this.allowedEditor = helpers.clone( this.allowed );
		this.rowNode = null;
	}

	preUpdateAllowedArtifact() {
		var options;
		
		if( helpers.areEqual(this.allowed,this.allowedEditor) ) {
			return;
		}
		options = [
			{
				'callback': this.updateAllowedArtifact,
				'text': "Yes"
			},
			{
				'callback': function(){},
				'text': "No"
			}
		] ;
		let msg = "Confirm Update of Allowed Artifact for " + this.project.acronym; // point to original name...may have changed
		helpers.showMessage( this, this.dialog, "Confirmation", msg, options ) ;
	}

	async updateAllowedArtifact() {
		//
		// disable button, re-enable is handled after user changes a form value.
		//
		console.log( "Updating allowed with: " + JSON.stringify(this.allowedEditor) ) ;

		try {
			let url = environment.apiUrl + '/allowedartifacts';
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
				body: JSON.stringify( this.allowedEditor )
			});

			console.log("updateAllowedArtifact status: " + response.status);
			if( response.status !== 200 ) {
				let data:Error = await response.json();
				console.log( "updateLicensedArtifact results: " + data.detail );
				helpers.showMessage( this, this.dialog, 'Licensed Artifact Update Failed',
					'status code: ' + response.status + ' ' + data.detail ) ;
			} else {
				let data:AllowedArtifact = await response.json();
				helpers.copy( this.allowedEditor, data ) ;
				helpers.copy(this.allowed, this.allowedEditor) ;
				this.rowNode.setData(this.allowed); // repaint
				let updatedNodes:RowNode[] = [this.rowNode] ;
				this.gridOptions.api.redrawRows( {rowNodes: updatedNodes} );
				helpers.showMessage( this, this.dialog, 'Allowed Update', 'Done' ) ;
			}
			//
			// provide user feedback either way...
			//
		} catch (error) {
			console.log( 'Update of Allowed Artifact failed: ' + JSON.stringify(error) ) ;
			helpers.showMessage( this, this.dialog, 'Update of Allowed Artifact Failed', JSON.stringify(error) ) ;
		} finally {
		}
	}

	private async loadAllowedArtifactsForProject( project: Project ) {
		try {
			let url = environment.apiUrl + '/allowedartifacts/findByProjectId?projectId=' + project.project_id;
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
				let data = await response.json();
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