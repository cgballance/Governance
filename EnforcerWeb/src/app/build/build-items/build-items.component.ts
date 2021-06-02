import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from '../../auth.service';
import { NGEventHubService} from '../../ngevent-hub.service' ;
import { Project } from '../../model/Project' ;
import { Build } from '../../model/Build' ;
import { BuildItem } from '../../model/BuildItem' ;
import { Error } from '../../model/Error' ;
import { Component as ProjectComponent } from '../../model/Component' ;
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers';

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-build-items',
	templateUrl: './build-items.component.html',
	styleUrls: [ './build-items.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class BuildItemsComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public project: Project ;
	public component: ProjectComponent ;
	public build: Build ;
	public builditem: BuildItem ;
	public builditemEditor: BuildItem;
	public rowNode: RowNode;

	isShowForm = true;
	loginEventSub = null;
	projectEventSub = null;
	componentEventSub = null;
	buildEventSub = null;
	buildcomponentEventSub = null;

	constructor( 
		private _authService: AuthService,
		private _ngEventHubService: NGEventHubService,
		private dialog: MatDialog  ) {
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
		this.gridOptions.getRowStyle = function(params) {
			if (params.node.data.allowed == false ) {
				return { background: '#FFA0A0' }
			}
		}
		this.columnDefs = [
			{headerName: "BuildItem Id", field: "build_item_id", hide: true},
			{headerName: "Build Id", field: "build_id", hide: true},
			{headerName: "Group Name", field: "group_name"},
			{headerName: "Artifact Name", field: "artifact_name"},
			{headerName: "Version Name", field: "version_name"},
			{headerName: "Status Snapshot", field: "artifact_status_snapshot"},
			{headerName: "Allowed", field: "allowed"},
		];
		this.rowData = [];
		this.emptyComponent();
		this.emptyBuild();
		this.emptyBuildItem();
	}

  	ngOnInit(): void {
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "build-components.component received LoginEvent" ) ;
		}) ;
		this.projectEventSub = this._ngEventHubService.BuildProjectSelectionEvent.subscribe( response => {
			console.log( "build-items.component received project event. "  + JSON.stringify(response) ) ;
			this.project = response;
			this.emptyBuildItem();
			this.rowData = [];
		} ) ;
		this.buildcomponentEventSub = this._ngEventHubService.BuildComponentSelectionEvent.subscribe( response => {
			this.component = response;
			this.emptyBuildItem();
			this.rowData = [] ;
		}) ;
		this.buildEventSub = this._ngEventHubService.BuildBuildSelectionEvent.subscribe( response => {
			console.log( "BUILD ITEMS GOT BUILD SELECTION EVENT: "  + JSON.stringify(response) ) ;
			this.build = response;
			this.emptyBuildItem();
			this.rowData = [] ;
			if( this.build.build_id !== undefined ) {
				this.loadBuildItemsForBuild( this.build ) ;
			}
		});
  	}

  	ngOnDestroy(): void {
		  if( this.loginEventSub ) {
			this.loginEventSub.unsubscribe();
		  }
		  if( this.projectEventSub ) {
			this.projectEventSub.unsubscribe();
		  }
		  if( this.buildcomponentEventSub ) {
			this.buildcomponentEventSub.unsubscribe();
		  }
		  if( this.buildEventSub ) {
			this.buildEventSub.unsubscribe();
		  }
	}

	toggleForm(): void {
		this.isShowForm = !this.isShowForm;
	}

	onRowSelected(event): void {
		console.log("row " + JSON.stringify(event.node.data) + " selected = " + event.node.selected);
		if( event.node.selected ) {
			console.log( "Load the builditem form." ) ;
			this.builditem = event.node.data;
			this.rowNode = event.node;
		} else if( event.node.data.builditem_id == this.builditem.builditem_id ) {
			console.log( "empty out the current build-item" ) ;
			this.emptyBuildItem();
		}
		this.builditemEditor = helpers.clone(this.builditem);
		this._ngEventHubService.BuildItemSelectionEvent.emit( this.component ) ;
	}

	private emptyProject() {
		this.project = new Project() ;
	}

	private emptyComponent() {
		this.component = new ProjectComponent() ;
	}

	private emptyBuild() {
		this.build = new Build() ;
	}

	private emptyBuildItem() {
		this.builditem = new BuildItem() ;
		this.builditemEditor = helpers.clone( this.builditem );
		this.rowNode = null;
	}

	preRequestPermit() {
		var options = [
			{
				'callback': this.requestPermit,
				'text': "Yes"
			},
			{
				'callback': function(){},
				'text': "No"
			}
		] ;
		let msg = "Confirm Permit Request for Project: " + this.project.acronym; // point to original name...may have changed
		helpers.showMessage( this, this.dialog, "Confirmation", msg, options ) ;
	}

	async requestPermit() {
		helpers.showMessage( this, this.dialog, 'NOT YET IMPLEMENTED', 
			'PERMIT REQUEST NOT IMPLEMENTED YET<br/>Add through Artifact View'
		);
	}

	private async loadBuildItemsForBuild( build: Build ) {
		try {
			let url = environment.apiUrl + '/builditems/findByBuildId?buildId=' + build.build_id;
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
				console.log("Retrieved data # of rows: " + data.length);
				this.rowData = data;
				this.gridOptions.api.setRowData(this.rowData);
			} else {
				this.rowData = [];
				// set error message....
				let data:Error = await response.json();
				console.log("Load BuildItems failed, status code: " + response.status + " with " + data.detail );
				helpers.showMessage( this, this.dialog, 'Data Load Failed',
					'status code: ' + response.status + ' ' + data.detail ) ;
			}
		} catch( error ) {
			helpers.showMessage( this, this.dialog, 'Data Load Failed', 'Exception: ' + JSON.stringify(error) ) ;
		}
	}
}
