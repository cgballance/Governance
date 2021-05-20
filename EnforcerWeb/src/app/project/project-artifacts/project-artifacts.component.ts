import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from '../../auth.service';
import { NGEventHubService} from '../../ngevent-hub.service' ;
import { Project } from '../../model/Project' ;
import { Artifact } from '../../model/Artifact' ;
import { Error } from '../../model/Error' ;
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers';

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-project-artifacts',
	templateUrl: './project-artifacts.component.html',
	styleUrls: [ './project-artifacts.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class ProjectArtifactsComponent implements OnInit, OnDestroy {
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
	projectEventSub = null;

	constructor( 
		private _authService: AuthService,
		private _ngEventHubService: NGEventHubService,
		private dialog: MatDialog ) {
		this.gridOptions = <GridOptions> {
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
		this.emptyProject();
		this.emptyArtifact();
	}

  	ngOnInit(): void {
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "project-artifacts.component received LoginEvent" ) ;
		}) ;
		this.projectEventSub = this._ngEventHubService.ProjectSelectionEvent.subscribe( response => {
			console.log( "project-artifacts.component received project event. "  + JSON.stringify(response) ) ;
			this.project = response;
			this.emptyArtifact();
			if( this.project.project_id !== undefined ) {
				// load up the artifacts for this project!
				this.loadArtifactsForProject( this.project ) ;
			} else {
				this.rowData = [] ;
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
	}

	toggleForm(): void {
		this.isShowForm = !this.isShowForm;
	}

	onRowSelected(event): void {
		//console.log("row " + JSON.stringify(event.node.data) + " selected = " + event.node.selected);
		if( event.node.selected ) {
			this.artifact = event.node.data;
			this.rowNode = event.node;
		} else if( event.node.data.artifact_id == this.artifact.artifact_id ) {
			this.emptyArtifact();
		}
		this.artifactEditor = helpers.clone(this.artifact);
		this._ngEventHubService.ProjectArtifactSelectionEvent.emit( this.artifact ) ;
		console.log( "project-artifacts emitted ProjectArtifactSelectionEvent " + JSON.stringify(this.artifact) ) ;
	}

	private emptyProject() {
		this.project = new Project();
	}

	private emptyArtifact() {
		this.artifact = new Artifact() ;
		this.artifactEditor = helpers.clone( this.artifact );
		this.rowNode = null;
	}

	private async loadArtifactsForProject( project: Project ) {
		try {
			let url = environment.apiUrl + '/artifacts/findByProjectId?projectId=' + project.project_id;
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
				let data:Artifact[] = await response.json();
				console.log("artifacts.component::Retrieved data # of rows: " + data.length);
				this.rowData = data;
				this.gridOptions.api.setColumnDefs(this.columnDefs);
				this.gridOptions.api.setRowData(this.rowData);
			} else {
				this.rowData = [];
				// set error message....
				let data:Error = await response.json();
				console.log("Load Artifacts failed, status code: " + response.status + " with " + data.detail );
				helpers.showMessage( this, this.dialog, 'Data Load Failed', 'status code: ' +
						response.status + ' ' + data.detail ) ;
			}
		} catch (error) {
			helpers.showMessage( this, this.dialog, 'Data Load Failed', 'Exception: ' +  JSON.stringify(error) ) ;
		}
	}
}
