import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from  '../../auth.service';
import { EventEmitter } from '@angular/core';
import { NGEventHubService } from "../../ngevent-hub.service";
import { Project } from '../../model/Project' ;
import { Error } from '../../model/Error' ;
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers';

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-build-projects',
	templateUrl: './build-projects.component.html',
	styleUrls: [ './build-projects.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class BuildProjectsComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public project ;
	public projectEditor;
	public rowNode;
	public build = {
		"num_components": "",
		"last_build": ""
	} ;
	isShowForm = true;
	loginEventSub = null;
	BuildProjectEvent = new EventEmitter() ;

	constructor(
		private _authService: AuthService,
		private _ngEventHubService: NGEventHubService,
		private dialog: MatDialog ) {
		this.emptyProject();
		this.gridOptions = <GridOptions> {
			headerHeight:30,
			rowHeight:30,
			defaultColDef: {
				sortable: true,
				resizable: true,
				filter: 'agTextColumnFilter',
			},
		};
		
		this.gridOptions.getRowStyle = function(params) {
			// If a project is near ending, then yellow.
			// If a project is ended already, then red.
			// Otherwise, default.

			if (params.node.data.begin_date === null || params.node.data.begin_date === "" ) {
				return { background: '#FF8888' }
			}
			if (params.node.data.end_date === null || params.node.data.end_date === "" ) {
				return;
			}
			var endDate = new Date( params.node.data.end_date ) ;
			var now = new Date();
			var diff = endDate.getTime() - now.getTime() ;
			if( diff < 0 ) {
				return { background: '#FF8888' }
			}
			if( diff < 7*24*3600*1000 ) { // 1 week in ms
				return { background: 'yellow' }
			}
		}
		this.columnDefs = [
			{ headerName: "Project Id", field: "project_id", hide: true },
			{ headerName: "Acronym", field: "acronym" },
			{ headerName: "Business Owner", field: "business_owner" },
			{ headerName: "IT Owner", field: "it_owner" },
			{ headerName: "Begin Date", field: "begin_date" },
			{ headerName: "End Date", field: "end_date" },
		];
		this.rowData = [];
	}

  	ngOnInit(): void {
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "build-projects.component received LoginEvent" ) ;
		}) ;
		if( this._authService.getToken() !== "" ) {
			this.loadProjects();
		}
  	}

	ngOnDestroy(): void {
		if( this.loginEventSub ) {
			this.loginEventSub.unsubscribe();
		}
	}

	toggleForm(): void {
		this.isShowForm = !this.isShowForm;
	}

	onRowSelected(event): void {
		//console.log("row " + JSON.stringify(event.node.data) + " selected = " + event.node.selected);
		if( event.node.selected ) {
			this.project = event.node.data;
			this.rowNode = event.node;
		}  else if( event.node.data.project_id == this.project.project_id ) {
			this.emptyProject();
		}
		this.projectEditor = helpers.clone(this.project);
		this._ngEventHubService.BuildProjectSelectionEvent.emit( this.project ) ;
	}

	private emptyProject() {
		this.project = new Project() ;
		this.projectEditor = helpers.clone( this.project );
		this.rowNode = null;
	}

	private async loadProjects() {
		try {
			let url = environment.apiUrl + '/projects';
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
			if( response.status !== 200 ) {
				let data:Error = await response.json();
				console.log("Load Projects Failed, status code: " + response.status + " with " + data.detail );
				helpers.showMessage( this, this.dialog, 'Load Projects Failed',
					'status code: ' + response.status + ' ' + data.detail ) ;
			} else {
				let data = await response.json();
				console.log("Retrieved data # of rows: " + data.length);
				//console.log(JSON.stringify(data));
				this.gridOptions.api.setRowData(data);
			}
		} catch( error ) {
			helpers.showMessage( this, this.dialog, 'Load Projects Error: ', JSON.stringify(error) ) ;
		}
		this.emptyProject();
	}
}
