import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from  '../../auth.service';
import { EventEmitter } from '@angular/core';
import { NGEventHubService } from "../../ngevent-hub.service";
import { Project } from "../../model/Project";
import { Error } from "../../model/Error";
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers' ;

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-project-projects',
	templateUrl: './project-projects.component.html',
	styleUrls: [ './project-projects.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class ProjectProjectsComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public project: Project ;
	public projectEditor: Project;
	public rowNode: RowNode;
	isShowForm = true;
	loginEventSub = null;
	ProjectEvent = new EventEmitter() ;

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
			console.log( "end date of project is: " + params.node.data.end_date ) ;
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
		this.emptyProject();
	}

  	ngOnInit(): void {
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "projects.component received LoginEvent" ) ;
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
		} else if(event.node.data.project_id == this.project.project_id) {
			this.emptyProject();
		}
		this.projectEditor = helpers.clone(this.project);
		// Need to Emit an event.
		this._ngEventHubService.ProjectSelectionEvent.emit( this.project ) ;
	}

	public emptyProject() {
		this.project = new Project() ;
		this.projectEditor = helpers.clone( this.project );
		this.rowNode = null;
	}

	preUpdateProject() {
		var options ;
		
		if( helpers.areEqual(this.project,this.projectEditor) ) {
			return;
		}
		options = [
			{
				'callback': this.updateProject,
				'text': "Yes"
			},
			{
				'callback': function(){},
				'text': "No"
			}
		] ;
		let msg = "Confirm Update of Project: " + this.project.acronym; // point to original name...may have changed
		helpers.showMessage( this, this.dialog, "Confirmation", msg, options ) ;
	}

	async updateProject() {
		//
		// disable button, re-enable is handled after user changes a form value.
		//
		console.log( "Updating project with: " + JSON.stringify(this.projectEditor) ) ;

		try {
			let url = environment.apiUrl + '/projects' ;
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
				body: JSON.stringify( this.projectEditor )
			});

			console.log("updateProject status: " + response.status);
			if( response.status !== 200 ) {
				let data:Error = await response.json();
				console.log( "updateproject results: " + JSON.stringify(data) );
				helpers.showMessage( this, this.dialog, 'Project Update Failed',
					'status code: ' + response.status + ' ' + data.detail ) ;
			} else {
				let data: Project = await response.json() ;
				helpers.copy( this.projectEditor, data ) ;
				helpers.copy(this.project, this.projectEditor) ;
				this.rowNode.setData(this.project); // repaint
				let updatedNodes:RowNode[] = [this.rowNode] ;
				this.gridOptions.api.redrawRows( {rowNodes: updatedNodes} );
				this._ngEventHubService.ProjectChangedEvent.emit( this.project );
				helpers.showMessage( this, this.dialog, 'Project Update', 'Done' ) ;
			}
			//
			// provide user feedback either way...
			//
		} catch (error) {
			console.log( 'Update Project failed: ' + JSON.stringify(error) ) ;
			helpers.showMessage( this, this.dialog, 'Update Project Failed', JSON.stringify(error) ) ;
		} finally {
		}
	}

	async preCreateProject() {
		// just for safety...
		var options = [
			{
				'callback': this.createProject,
				'text': "Yes"
			},
			{
				'callback': function(){},
				'text': "No"
			}
		] ;
		let msg = "Confirm Creation of Project named: " + this.projectEditor.acronym;
		helpers.showMessage( this, this.dialog, "Confirmation", msg, options ) ;
	}

	async createProject() {
		try {
			let url = environment.apiUrl + '/projects' ;
			let obj: Project = new Project();
			obj = helpers.copy( obj, this.projectEditor ) ;
			obj.project_id = null;
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

			console.log("createProject status: " + response.status);
			if (response.status !== 201) {
				let data:Error = await response.json();
				console.log("createProject results: " + JSON.stringify(data));
				helpers.showMessage( this, this.dialog, 'CREATE Project Failed',
					'status code: ' + response.status + ' ' + data.detail );
			} else {
				let proj:Project = await response.json();
				this.gridOptions.api.applyTransaction({ add: [proj] });
				this._ngEventHubService.ProjectCreatedEvent.emit(proj);
				helpers.showMessage( this, this.dialog, 'Project CREATED', 'Done' );
			}
		} catch (error) {
			console.log('CREATE Project failed: ' + JSON.stringify(error));
			helpers.showMessage( this, this.dialog, 'CREATE Project Failed', JSON.stringify(error) );
		} finally {
		}
	}

	preDeleteProject() {
		var options = [
			{
				'callback': this.deleteProject,
				'text': "Yes"
			},
			{
				'callback': function(){},
				'text': "No"
			}
		] ;
		let msg = "<center>Confirm Deletion</center><br/>" + 
			"Deleting a Project will remove all Project specific data, including all Build information.<br/>" +
			"If you just want to reuse the name, consider renaming instead.<br/>" +
			"This operation is primarily intended for pruning long unused project data.<br/>";
			helpers.showMessage( this, this.dialog, "Confirmation", msg, options ) ;
	}

	async deleteProject() {
		try {
			let url = environment.apiUrl + '/projects/' + this.projectEditor.project_id;
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

			console.log("deleteProject status: " + response.status);
			if( response.status !== 200 ) {
				let data:Error = await response.json();
				console.log( "deleteProject results: " + JSON.stringify(data) );
				helpers.showMessage( this, this.dialog, 'Delete Failed',
					'status code: ' + response.status + ' ' + data.detail ) ;
			} else {
				this.gridOptions.api.applyTransaction({ remove: [this.rowNode.data] });
				//this.gridOptions.api.removeItems([this.rowNode], false );
				let thing: Project = new Project();
				helpers.copy( thing, this.project ) ;
				this._ngEventHubService.ProjectDeletedEvent.emit( thing );
				helpers.showMessage( this, this.dialog, 'Project Deleted', 'Done' ) ;
			}
			//
			// provide user feedback either way...
			//
		} catch (error) {
			console.log( 'Delete Project failed: ' + JSON.stringify(error) ) ;
			helpers.showMessage( this, this.dialog, 'Delete Project Failed', JSON.stringify(error) ) ;
		} finally {
		}
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
			if( response.status === 200 ) {
				let data: Project[] = await response.json();
				console.log("Retrieved data # of rows: " + data.length);
				//console.log(JSON.stringify(data));
				this.rowData = data;
				this.gridOptions.api.setRowData(data);
				this.gridOptions.api.sizeColumnsToFit();
			} else {
				let data: Error = await response.json();
				helpers.showMessage( this, this.dialog, 'Load Projects Failed',
					'status code: ' + response.status + ' ' + data.detail ) ;
			}
		} catch (error) {
			console.log( 'Load Projects failed: ' + JSON.stringify(error) ) ;
			helpers.showMessage( this, this.dialog, 'Load Projects Failed', JSON.stringify(error) ) ;
		}
		this.emptyProject();
	}
}
