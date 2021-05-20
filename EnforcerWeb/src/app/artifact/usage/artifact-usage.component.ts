import { Component, OnInit, OnDestroy, ViewEncapsulation } from "@angular/core";
import { GridApi, GridOptions, RowNode } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { MatDialog } from '@angular/material/dialog' ;
import { AuthService } from '../../auth.service';
import { NGEventHubService} from '../../ngevent-hub.service' ;
import { FocusKeyManager } from "@angular/cdk/a11y";
import { AllowedArtifact } from '../../model/AllowedArtifact';
import { LicensedArtifact } from '../../model/LicensedArtifact';
import { Artifact } from '../../model/Artifact';
import { Project } from '../../model/Project';
import { ProjectComponentBuild } from '../../model/ProjectComponentBuild' ;
import { Error } from '../../model/Error' ;
import { environment } from '../../../environments/environment';
import * as helpers from '../../helpers';

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-artifact-usage',
	templateUrl: './artifact-usage.component.html',
	styleUrls: [ './artifact-usage.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class ArtifactUsageComponent implements OnInit, OnDestroy {
	public gridOptions:GridOptions;
	public rowData:any[];
	public columnDefs:any[];
	public modules: any[] = [ClientSideRowModelModule, CsvExportModule];
	public projectComponentBuild: ProjectComponentBuild;
	public projectComponentBuildEditor: ProjectComponentBuild ;  // TODO - CURRENTLY NOT USED
	public artifact: Artifact;
	public rowNode: RowNode ;
	mayPermit: boolean;
	mayRevoke: boolean;
	isShowForm = true;
	loginEventSub = null;
	artifactEventSub = null;
	statusToGet = null;
	permittedCache = new Set([-1]);

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
		let ctx = this;
		this.gridOptions.getRowStyle = function(params) {
			if( params.context !== undefined && params.context.artifact !== undefined && params.context.artifact.status === 'LIMITED'  ) {
				//console.log(Object.keys(params.node));
				if( params.context.cacheContains( params.node.data.project.project_id ) === false ) {
					if( params.node.selected ) {
						return { background: '#DDDDAA' }
					} else {
						return { background: '#FFFF99' }
					}
				}
			}
		}
		this.columnDefs = [
			{headerName: "Project Acronym", field: "project.acronym"},
			{headerName: "Component Name", field: "component.name"},
			{headerName: "Build Version", field: "build.component_version"},
			{headerName: "Build Timestamp", field: "build.build_ts"},
			{headerName: "Business Owner", field: "project.business_owner"},
			{headerName: "IT Owner", field: "project.it_owner"},
			{headerName: "Begin Date", field: "project.begin_date"},
			{headerName: "End Date", field: "project.end_date"},
			{headerName: "Project Id", field: "project.project_id", hide: true},
			{headerName: "Component Id", field: "component.component_id", hide: true},
		];
		this.rowData = [];
		this.emptyArtifact();
		this.emptyProjectComponentBuild();
	}

  	ngOnInit(): void {
		this.mayPermit = false;
		this.mayRevoke = false;
		console.log( "Ok, artifact-usage.component will subscribe to LoginEvent" ) ;
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "artifact-usage.component received LoginEvent" ) ;
		}) ;
		console.log( "Ok, artifact-usage.component will subscribe to ArtifactArtifactSelectionEvent" ) ;
		let ctx = this;
		this.artifactEventSub = this._ngEventHubService.ArtifactArtifactSelectionEvent.subscribe( response => {
			console.log( "artifact-usage.component received ArtifactArtifactSelectionEvent" ) ;
			this.artifact = response;
			this.gridOptions.context = ctx;
			this.mayPermit = false; // check back on this....may call updateButtonState();
			this.mayRevoke = false; // check back on this....may call updateButtonState();
			if( this.artifact !== undefined && this.artifact.artifact_id !== undefined ) {
				console.log( "CALLING FOR LOAD OF ARTIFACT USERS" ) ;
				this.loadComponentsUsingArtifact(this.artifact) ;
				this.gridOptions.api.setRowData(this.rowData);
				if( this.artifact.status === 'LIMITED' ) {
					//
					// Find out if the projects are in the 'allowed' or 'licensed' tables
					//
					this.findPermittedProjectsForArtifactSoon(ctx);
				} else {
					this.permittedCache = new Set([-1]) ;
				}
			} else {
				this.rowData = [];
				this.gridOptions.api.setRowData(this.rowData);
			}
			this.emptyProjectComponentBuild();
		}) ;
  	}

  	ngOnDestroy(): void {
		  if( this.loginEventSub ) {
			this.loginEventSub.unsubscribe();
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
			this.projectComponentBuild = event.node.data;
			this.rowNode = event.node;
			this.updateButtonState();
		} else {
			if( event.node.data.component.component_id == this.projectComponentBuild.component.component_id ) {
				this.emptyProjectComponentBuild();
				this.updateButtonState();
			}
		}
		//
		// refresh row ui
		//
		this.gridOptions.api.redrawRows() ;
		
		// Need to Emit an event.
		this._ngEventHubService.ArtifactUsageSelectionEvent.emit( this.artifact ) ;
	}

	private emptyArtifact() {
		this.artifact = new Artifact() ;
	}

	private emptyProjectComponentBuild() {
		this.projectComponentBuild = new ProjectComponentBuild() ;
	}

	private cacheContains( projectId ) {
		return this.permittedCache.has( projectId ) ;
	}

	private updateButtonState() {
		if( this.artifact.status === "LIMITED" ) {
			if( this.cacheContains(this.projectComponentBuild.project.project_id) ) {
				this.mayPermit = false;
				this.mayRevoke = true;
			} else {
				this.mayPermit = true;
				this.mayRevoke = false;
			}
		} else {
			this.mayPermit = false;
			this.mayRevoke = false;
		}
	}

	private async loadComponentsUsingArtifact( artifact: Artifact ) {
		try {
			let url = environment.apiUrl + '/experimental/findProjectComponentBuildsByArtifactId?artifactId=' + artifact.artifact_id;
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
			if (response.status == 200) {
				let data:ProjectComponentBuild[] = await response.json();
				console.log("artifact-usage.component::Retrieved data # of rows: " + data.length);
				//console.log(JSON.stringify(data));
				this.rowData = data;
			} else {
				this.rowData = [];
				// set error message....
				let data:Error = await response.json();
				console.log("GOT an http error " + response.status + " with " + JSON.stringify(data));
				helpers.showMessage( this, this.dialog, 'loadComponentsUsingArtifact Failed', 'status code: ' +
						response.status + ' ' + JSON.stringify(data) ) ;
			}
		} catch( error ) {
			helpers.showMessage( this, this.dialog, 'loadComponentsUsingArtifact Failed', 'Exception: ' +  JSON.stringify(error) ) ;
		}
	}

	private async findPermittedProjectsForArtifactSoon( ctx ) {
		let callCtx = ctx;
		setTimeout( function() {
			callCtx.findPermittedProjectsForArtifact(callCtx).then( resp => {
				callCtx.gridOptions.api.redrawRows();
				// reselect what was the selection....due to this reload of data
				setTimeout( function() {
					ctx.gridOptions.api.forEachNode( node => {
						if( node.data.project.project_id === ctx.projectComponentBuild.project.project_id &&
							node.data.component.component_id === ctx.projectComponentBuild.component.component_id ) {
							node.setSelected(true);
							// no event for selection event generated...wtf ;
							ctx.updateButtonState();
						}
					} ) ;
				}, 25 );
			} ) ;
		}, 50 );
	}

	private async findPermittedProjectsForArtifact( ctx ) {
		let artifact = ctx.artifact;
		try {
			console.log( "OK, look for PERMITTED projects for this artifact" );
			let url = environment.apiUrl + '/projects/findPermittedProjectsByArtifactId?artifactId=' + artifact.artifact_id;
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
			try {
				ctx.permittedCache = new Set([-1]);
			} catch( thing ) {
				console.log( "creation of cache issue " + thing ) ;
				throw thing;
			} ;
			if (response.status === 200) {
				let data:Project[] = await response.json();
				console.log("artifact-usage.component::PERMITTED CACHE Retrieved data # of rows: " + data.length);
				data.forEach( thing => {
					ctx.permittedCache.add( thing.project_id ) ;
				}) ;
			} else {
				// set error message....
				let data:Error = await response.json();
				console.log("GOT an http error " + response.status + " with " + JSON.stringify(data));
				helpers.showMessage( this, this.dialog, 'findPermittedProjectsForArtifact Failed', 'status code: ' +
						response.status + ' ' + JSON.stringify(data) ) ;
			}
		} catch( error ) {
			helpers.showMessage( this, this.dialog, 'findPermittedProjectsForArtifact Failed', 'Exception: ' +  JSON.stringify(error) ) ;
		}
	}

	/**
	 * Adding of a project to be permitted will require one of two calls.  Either we're adding to allowedartifacts or
	 * we're adding to licensedartifacts depending upon whether or not the artifact 'is_vendor_licensed'.
	 */
	async addPermittedProject() {
		if( this.artifact.is_vendor_licensed == false ) {
			let allowedArtifact = new AllowedArtifact() ;
			allowedArtifact.artifact_id = this.artifact.artifact_id;
			allowedArtifact.project_id = this.projectComponentBuild.project.project_id;
			allowedArtifact.approval_architect = 'chas';
			allowedArtifact.approval_ts = new Date();

			try {
				let url = environment.apiUrl + '/allowedartifacts' ;
				console.log("Calling " + url + ' with data: ' + JSON.stringify(allowedArtifact) );
				let bearerToken = this._authService.getToken();
				let response = await fetch(url, {
					headers: {
						Accept: 'application/json',
						'Content-Type': 'application/json',
						Authorization: 'Bearer ' + bearerToken
					},
					method: 'POST',
					body: JSON.stringify(allowedArtifact),
					mode: 'cors',
					credentials: "include"
				});

				console.log("ok to get the data? " + response.status);
				if (response.status === 201) { // CREATED
					let data:AllowedArtifact = await response.json();
					//
					// Reload from backend, which will cause re-rendering with this new thing.
					// in the future, maybe a successful create and direct cache inclusion?
					//
					this.findPermittedProjectsForArtifactSoon( this ) ;
					helpers.showMessage( this, this.dialog, 'Add Allowed Permit', 'Done' ) ;
				} else {
					// set error message....
					let data:Error = await response.json();
					console.log("GOT an http error " + response.status + " with " + data.detail );
					helpers.showMessage( this, this.dialog, 'Insert of AllowedArtifact Failed', 'status code: ' +
						response.status + ' ' + data.detail ) ;
				}
			} catch( error ) {
				helpers.showMessage( this, this.dialog, 'Insert of AllowedArtifact Failed',
					'Exception: ' +  JSON.stringify(error) ) ;
			}
		} else {
			let licensedArtifact = new LicensedArtifact();
			licensedArtifact.artifact_id = this.artifact.artifact_id;
			licensedArtifact.project_id = this.projectComponentBuild.project.project_id;
			licensedArtifact.contract = 'TODO';
			licensedArtifact.vendor = 'TODO';
			licensedArtifact.approval_architect = 'TODO';
			licensedArtifact.approval_ts =  new Date() ;

			try {
				let url = environment.apiUrl + '/licensedartifacts' ;
				console.log("Calling " + url + ' with data: ' + JSON.stringify(licensedArtifact) );
				let bearerToken = this._authService.getToken();
				let response:Response = await fetch(url, {
					headers: {
						Accept: 'application/json',
						'Content-Type': 'application/json',
						Authorization: 'Bearer ' + bearerToken
					},
					method: 'POST',
					body: JSON.stringify(licensedArtifact),
					mode: 'cors',
					credentials: "include"
				});

				console.log("ok to get the data? " + response.status);
				if (response.status === 201) { // OK
					let data:LicensedArtifact = await response.json();
					//
					// Reload from backend, which will cause re-rendering with this new thing.
					// in the future, maybe a successful create and direct cache inclusion?
					//
					this.findPermittedProjectsForArtifactSoon( this ) ;
					helpers.showMessage( this, this.dialog, 'Add Licensed Permit', 'Done' ) ;
				} else {
					// set error message....
					let data:Error = await response.json();
					console.log("GOT an http error " + response.status + " with " + data.detail );
					helpers.showMessage( this, this.dialog, 'Insert of LicensedArtifact Failed', 'status code: ' +
						response.status + ' ' + data.detail ) ;
				}
			} catch( error ) {
				helpers.showMessage( this, this.dialog, 'Insert of LicensedArtifact Failed',
					'Exception: ' +  JSON.stringify(error) ) ;
			}	
		}
	}

	/**
	 * Revocation of a project requires one of two calls.  Either we're removing an allowedartifacts or
	 * we're removing an licensedartifacts depending upon whether or not the artifact 'is_vendor_licensed'.
	 */
	async removePermittedProject() {
		if( this.artifact.is_vendor_licensed == false ) {
			try {
				let url = environment.apiUrl + '/allowedartifacts/deleteAllowedArtifactByProjectIdAndArtifactId' ;
				let data = {
					project_id: this.projectComponentBuild.project.project_id,
					artifact_id: this.artifact.artifact_id
				}
				console.log("artifact-usage removePermittedProject Calling " + url + " with data " + JSON.stringify(data) );
				let bearerToken = this._authService.getToken();
				let response:Response = await fetch(url, {
					headers: {
						Accept: 'application/json',
						'Content-Type': 'application/json',
						Authorization: 'Bearer ' + bearerToken
					},
					method: 'POST',
					body: JSON.stringify(data),
					mode: 'cors',
					credentials: "include",
				});

				console.log("ok to get the data? " + response.status);
				if (response.status === 200) { // OK
					//
					// Reload from backend, which will cause re-rendering with this new thing.
					// in the future, maybe a successful create and direct cache inclusion?
					//
					this.findPermittedProjectsForArtifactSoon( this ) ;
					helpers.showMessage( this, this.dialog, 'Remove Permit', 'Done' ) ;
				} else {
					// set error message....
					let data:Error = await response.json();
					console.log("GOT an http error " + response.status + " with " + data.detail );
					helpers.showMessage( this, this.dialog, 'Delete of AllowedArtifact Failed', 'status code: ' +
						response.status + ' ' + data.detail ) ;
				}
			} catch( error ) {
				helpers.showMessage( this, this.dialog, 'Delete of AllowedArtifact Failed', 'Exception: ' +  JSON.stringify(error) ) ;
			}
		} else {
			try {				
				let url = environment.apiUrl + '/licensedartifacts/deleteLicensedArtifactByProjectIdAndArtifactId' ;
				let data = {
					project_id: this.projectComponentBuild.project.project_id,
					artifact_id: this.artifact.artifact_id
				}
				console.log("artifact-usage removePermittedProject Calling " + url + " with data " + JSON.stringify(data) );
				let bearerToken = this._authService.getToken();
				let response:Response = await fetch(url, {
					headers: {
						Accept: 'application/json',
						'Content-Type': 'application/json',
						Authorization: 'Bearer ' + bearerToken
					},
					method: 'POST',
					body: JSON.stringify(data),
					mode: 'cors',
					credentials: "include"
				});

				console.log("ok to get the data? " + response.status);
				if (response.status === 200) { // OK
					//
					// Reload from backend, which will cause re-rendering with this new thing.
					// in the future, maybe a successful create and direct cache inclusion?
					//
					this.findPermittedProjectsForArtifactSoon( this ) ;
					
					helpers.showMessage( this, this.dialog, 'Project Update', 'Done' ) ;
				} else {
					// set error message....
					let data:Error = await response.json();
					console.log("GOT an http error " + response.status + " with " + data.detail );
					helpers.showMessage( this, this.dialog, 'Delete of LicensedArtifact Failed', 'status code: ' +
						response.status + ' ' + data.detail ) ;
				}
			} catch( error ) {
				helpers.showMessage( this, this.dialog, 'Delete of LicensedArtifact Failed',
					'Exception: ' +  JSON.stringify(error) ) ;
			}
		}
	}
}
