import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from '../../auth.service';
import { NGEventHubService} from '../../ngevent-hub.service' ;
import { Project } from '../../model/Project' ;
import { Error } from '../../model/Error' ;
import { Component as ProjectComponent } from '../../model/Component' ;
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers';

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-build-components',
	templateUrl: './build-components.component.html',
	styleUrls: [ './build-components.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class BuildComponentsComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public project: Project ;
	public component: ProjectComponent ;
	public componentEditor: ProjectComponent;
	public rowNode: RowNode ;

	isShowForm = true;
	loginEventSub = null;
	projectEventSub = null;

	constructor( 
		private _authService: AuthService,
		private _ngEventHubService: NGEventHubService,
		private dialog: MatDialog ) {
		this.emptyProject();
		this.emptyComponent();
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
			{headerName: "Component Id", field: "component_id", hide: true},
			{headerName: "Project Id", field: "project_id", hide: true},
			{headerName: "Name", field: "name"},
		];
		this.rowData = [];
	}

  	ngOnInit(): void {
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "build-components.component received LoginEvent" ) ;
		}) ;
		this.projectEventSub = this._ngEventHubService.BuildProjectSelectionEvent.subscribe( response => {
			console.log( "build-components.component received project event. "  + JSON.stringify(response) ) ;
			this.project = response;
			if( this.project.project_id !== undefined ) {
				// load up the known components for this project!
				this.loadComponentsForProject( this.project ) ;
			} else {
				this.emptyProject();
				this.emptyComponent();
				this.rowData = [];
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
		console.log("row " + JSON.stringify(event.node.data) + " selected = " + event.node.selected);
		if( event.node.selected ) {
			this.component = event.node.data;
			this.rowNode = event.node;
		} else if( event.node.data.component_id == this.component.component_id ) {
			this.emptyComponent();
		}
		this.componentEditor = helpers.clone(this.component);
		this._ngEventHubService.BuildComponentSelectionEvent.emit( this.component ) ;
	}

	private emptyProject() {
		this.project = new Project() ;
	}

	private emptyComponent() {
		this.component = new ProjectComponent() ;
		this.componentEditor = helpers.clone( this.component );
		this.rowNode = null;
	}

	private async loadComponentsForProject( project: Project ) {
		try {
			let url = environment.apiUrl + '/components/findByProjectId?projectId=' + project.project_id ;
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
				let data:Error = await response.json();
				console.log( "Load Components failed, status code: " + response.status + " with " + data.detail ) ;
				helpers.showMessage( this, this.dialog, 'Data Load Failed',
						'status code: ' + response.status + ' ' + data.detail ) ;
			}
		} catch( error ) {
			helpers.showMessage( this, this.dialog, 'Data Load Failed', 'Exception: ' + JSON.stringify(error) ) ;
		}
		this.emptyComponent();
	}
}
