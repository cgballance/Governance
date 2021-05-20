// app/app.component.ts
import { Component, ViewEncapsulation } from "@angular/core";
import { GridOptions } from "@ag-grid-community/all-modules";
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { CsvExportModule } from '@ag-grid-community/csv-export';
import { AuthService } from './auth.service';
import { NGEventHubService} from './ngevent-hub.service' ;

// or, if using Enterprise features
// import {GridOptions} from "@ag-grid-enterprise/all-modules";

@Component({
	selector: 'app-root',
	templateUrl: 'app.component.html',
	styleUrls: [ 'app.component.scss'],
	encapsulation: ViewEncapsulation.None
})

export class AppComponent {
	isAuthenticated : boolean = false;
	title : string = "Development Artifact Governance";
	loginEventSub = null;
	username: string = "";

	constructor( 
		private _authService: AuthService,
		private _ngEventHubService: NGEventHubService  ) {
		var comment = "Create a controller to orchestrate everything...it's a small application." ;
		console.log( comment ) ;
	}

	ngOnInit(): void {
		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "app.component received LoginEvent" ) ;
			let token = response.token;
			if( token !== "" ) {
				this.isAuthenticated = true;
				this.username = response.username;
			} else {
				this.isAuthenticated = false;
				this.username = "";
			}
		}) ;
  	}

	ngOnDestroy(): void {
		if( this.loginEventSub ) {
			this.loginEventSub.unsubscribe();
		}
	}

	login(): void {
		// nothing needs to be done here.  the LoginEvent will deal with it.
		console.log( "app.component: Login of service" ) ;
	}

	logout(): void {
		console.log( "app.component: Logout of service" ) ;
		this.isAuthenticated = false;
		this.username = '';
		this._authService.logout();
	}
}