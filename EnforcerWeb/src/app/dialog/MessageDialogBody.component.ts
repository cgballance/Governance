import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

/**
 * @title Dialog with header, scrollable content and actions
 */
@Component({
  selector: 'message-dialog-body',
  templateUrl: './MessageDialogBody.component.html',
  styleUrls: ['./MessageDialogBody.component.scss']
})

export class MessageDialogBody {
  constructor(
      public dialogRef: MatDialogRef<MessageDialogBody>,
      @Inject(MAT_DIALOG_DATA) public data: any) {
  }

  close() {
    this.dialogRef.close();
  }
}
