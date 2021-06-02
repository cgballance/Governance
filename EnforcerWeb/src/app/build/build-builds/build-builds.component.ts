import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from '../../auth.service';
import { NGEventHubService} from '../../ngevent-hub.service' ;
import { Project } from '../../model/Project' ;
import { Build } from '../../model/Build' ;
import { Error } from '../../model/Error' ;
import { Component as ProjectComponent } from '../../model/Component' ;
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers';

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-build-builds',
	templateUrl: './build-builds.component.html',
	styleUrls: [ './build-builds.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class BuildBuildsComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public project: Project ;
	public component: ProjectComponent ;
	public build: Build ;
	public buildEditor: Build;
	public rowNode: RowNode;
	
	isShowForm = true;
	loginEventSub = null;
	projectEventSub = null;
	componentEventSub = null;

	constructor( 
		private _authService: AuthService,
		private _ngEventHubService: NGEventHubService,
		private dialog: MatDialog  ) {
		this.emptyProject();
		this.emptyComponent();
		this.emptyBuild();
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
		this.gridOptions.getRowStyle = function(params) {
			if (params.node.data.infractions !== null &&  params.node.data.infractions !== "" ) {
				return { background: '#FFA0A0' }
			}
		}
		this.columnDefs = [
			{headerName: "Build Id", field: "build_id", hide: true},
			{headerName: "Build Timestamp", field: "build_ts"},
			{headerName: "Project Id", field: "project_id", hide: true},
			{headerName: "Component Id", field: "component_id", hide: true},
			{headerName: "Version", field: "component_version"},
			{headerName: "Source", field: "source"},
			{headerName: "Infractions", field: "infractions"},
		];
		this.rowData = [];
	}

  	ngOnInit(): void {
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "artifacts.component received LoginEvent" ) ;
		}) ;
		this.projectEventSub = this._ngEventHubService.BuildProjectSelectionEvent.subscribe( response => {
			console.log( "artifacts.component received project event. "  + JSON.stringify(response) ) ;
			this.project = response;
			this.emptyComponent();
			this.emptyBuild();
			this.rowData = [];
		}) ;
		this.componentEventSub = this._ngEventHubService.BuildComponentSelectionEvent.subscribe( response => {
			console.log( "build component event "  + JSON.stringify(response) ) ;
			this.component = response;
			this.emptyBuild();
			this.rowData = [];
			if( this.component !== undefined && this.component.component_id !== undefined ) {
				// load up the known components for this project!
				this.loadBuildsForComponent( this.component ) ;
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
		  if( this.componentEventSub ) {
			this.componentEventSub.unsubscribe();
		  }
	}

	toggleForm(): void {
		this.isShowForm = !this.isShowForm;
	}

	onRowSelected(event): void {
		console.log("row " + JSON.stringify(event.node.data) + " selected = " + event.node.selected);
		if( event.node.selected ) {
			this.build = event.node.data;
			this.rowNode = event.node;
		} else if( event.node.data.build_id == this.build.build_id ) {
			this.emptyBuild();
		}
		this.buildEditor = helpers.clone(this.build);
		this._ngEventHubService.BuildBuildSelectionEvent.emit( this.build ) ;
	}

	private emptyProject() {
		this.project = new Project() ;
	}

	private emptyComponent() {
		this.component = new ProjectComponent() ;
	}

	private emptyBuild() {
		this.build = new Build() ;
		this.buildEditor = helpers.clone( this.build );
		this.rowNode = null;
	}

	private async loadBuildsForComponent( component: ProjectComponent ) {
		console.log( "build-builds.component LOAD: " + JSON.stringify(component) ) ;
		try {
			let url = environment.apiUrl + '/builds/findByComponentId?componentId=' + component.component_id ;
			console.log( "Calling " + url ) ;
			let bearerToken = this._authService.getToken() ;
			let response = await fetch(url, {
				headers: {
					Accept: 'application/json',
					Authorization: 'Bearer ' + bearerToken
				},
				method: 'GET',
				mode: 'cors',
				credentials: "include"
			} );

			console.log( "ok to get the data? " + response.status ) ;
			if( response.status === 200 ) {
				let data = await response.json();
				console.log( "Retrieved data # of rows: " + data.length ) ;
				this.rowData = data;
				this.gridOptions.api.setRowData(this.rowData);
			} else {
				this.rowData = [] ;
				// set error message....
				let data:Error = await response.json();
				console.log( "Load Builds failed, status code: " + response.status + " with " + data.detail ) ;
				helpers.showMessage( this, this.dialog, 'Data Load Failed', 'status code: ' +
						response.status + ' ' + data.detail ) ;
			}
		} catch( error ) {
			helpers.showMessage( this, this.dialog, 'Data Load Failed', 'Exception: ' +  JSON.stringify(error) ) ;
		}
	}
}
