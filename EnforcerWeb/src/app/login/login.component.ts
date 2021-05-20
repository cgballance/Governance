import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { NGEventHubService} from '../ngevent-hub.service' ;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})

export class LoginComponent implements OnInit, OnDestroy {
  form: FormGroup;
  public loginInvalid: boolean;
  private formSubmitAttempt: boolean;
  private returnUrl: string;
  public message: string;
  loginEventSub = null;

  constructor(
      private fb: FormBuilder,
      private _authService: AuthService,
      private _ngEventHubService: NGEventHubService  ) {
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });

		this.loginEventSub = this._ngEventHubService.LoginEvent.subscribe( response => {
			console.log( "login.component received LoginEvent " + JSON.stringify(response) ) ;
      this.message = response.message;
		}) ;
  }

	ngOnDestroy(): void {
		if( this.loginEventSub ) {
			this.loginEventSub.unsubscribe();
		}
	}

  async onSubmit() {
    this.loginInvalid = false;
    this.formSubmitAttempt = false;
    this.message = "";
    if (this.form.valid) {
      const username = this.form.get('username').value;
      const password = this.form.get('password').value;
      await this._authService.login(username, password)
          .catch( reason => {
            this.loginInvalid = true;
            this.message = reason;
          });
    } else {
      this.formSubmitAttempt = true;
    }
  }
}