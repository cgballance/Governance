// app/app.module.ts
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";

import { AgGridModule } from "@ag-grid-community/angular";
import { MatExpansionModule } from '@angular/material/expansion';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatRadioModule} from '@angular/material/radio';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core' ;
import { NgxMatDatetimePickerModule, NgxMatNativeDateModule, NgxMatTimepickerModule } from '@angular-material-components/datetime-picker';
import { MatTabsModule } from '@angular/material/tabs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule } from '@angular/material/dialog';
import {MatSelectModule} from '@angular/material/select';

// application
import { AppComponent } from "./app.component";
import { LoginComponent } from './login/login.component';

import { ProjectProjectsComponent } from './project/project-projects/project-projects.component';
import { ProjectArtifactsComponent } from './project/project-artifacts/project-artifacts.component';
import { ProjectLicensedComponent } from './project/project-licensed/project-licensed.component';
import { ProjectAllowedComponent } from './project/project-allowed/project-allowed.component';

import { BuildProjectsComponent } from "./build/build-projects/build-projects.component";
import { BuildComponentsComponent } from "./build/build-components/build-components.component";
import { BuildBuildsComponent } from "./build/build-builds/build-builds.component";
import { BuildItemsComponent } from "./build/build-items/build-items.component";

import { ArtifactArtifactsComponent } from './artifact/artifacts/artifact-artifacts.component';
import { ArtifactUsageComponent } from './artifact/usage/artifact-usage.component';
import { ArtifactVersionsComponent } from './artifact/versions/artifact-versions.component';

import { OptionedMessageDialogBody } from './dialog/OptionedMessageDialogBody.component';

import { AuthService } from './auth.service' ;
import { NGEventHubService } from './ngevent-hub.service' ;

@NgModule({
	imports: [
		BrowserModule,
		AgGridModule.withComponents([]),
		MatExpansionModule,
		MatInputModule,
		MatButtonModule,
		MatRadioModule,
		MatCheckboxModule,
		MatTabsModule,
		MatDatepickerModule, MatNativeDateModule,
		NgxMatDatetimePickerModule, NgxMatTimepickerModule, NgxMatNativeDateModule,
		BrowserAnimationsModule,
		FormsModule,
		MatIconModule,
		MatCardModule,
		MatToolbarModule,
		ReactiveFormsModule,
		MatDialogModule,
		MatSelectModule
	],
	declarations: [
		AppComponent,
		LoginComponent,
		ProjectProjectsComponent,
		ProjectArtifactsComponent,
		ProjectLicensedComponent,
		ProjectAllowedComponent,
		BuildProjectsComponent,
		BuildComponentsComponent,
		BuildBuildsComponent,
		BuildItemsComponent,
		ArtifactArtifactsComponent,
		ArtifactUsageComponent,
		ArtifactVersionsComponent,
		OptionedMessageDialogBody
	],
	entryComponents: [OptionedMessageDialogBody],
	providers: [NGEventHubService, AuthService],
	bootstrap: [AppComponent]
})
export class AppModule {
}
