import { Injectable } from '@angular/core';
import { EventEmitter } from '@angular/core';
import { NGEventHubService} from './ngevent-hub.service' ;
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})

export class AuthService {
  public isAuthenticated = false;
  private authClient = {
    username: '',
    password: ''
  };
  private token: '';

  constructor(
		private _ngEventHubService: NGEventHubService) { }

  async login(username: string, password: string) {
    let url = environment.apiUrl + '/authenticate';
		console.log( "Calling " + url ) ;
    this.authClient.username = username;
    this.authClient.password = password;
		let response = await fetch(url, {
			headers: {
				Accept: 'application/json',
        'Content-Type': 'application/json'
			},
			method: 'POST',
			body: JSON.stringify(this.authClient),
			mode: 'cors',
			referrer: 'about:client',
			referrerPolicy: 'strict-origin-when-cross-origin',
			keepalive: false
		} );
    if( response.status === 200 ) {
		  let data = await response.json();
      console.log( "RECEIVED : " + JSON.stringify( data ) ) ;
      this.token = data.token ;
      //
      // when this happens, then the entire ui needs updating with real data from the backend.
      //
      this.isAuthenticated = true;
      this._ngEventHubService.LoginEvent.emit( { status: "Success", token: this.token, username: this.authClient.username } ) ;
    } else {
      console.log( "Login failed " + response.status ) ;
      this.isAuthenticated = false;
      this._ngEventHubService.LoginEvent.emit( { status: "Error", token: "", message: "login failed"} ) ;
    }
  }

  getToken() :string {
    return this.token;
  }

  async logout() {
    try {
      this.authClient = {
        username: '',
        password: ''
      };
      this.isAuthenticated = false;
      //
      // TODO - emit event?
      //
    } catch (err) {
      console.error(err);
    }
  }
}
