import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-default-login-layout',
  standalone: true,
  imports: [],
  templateUrl: './default-login-layout.component.html',
  styleUrl: './default-login-layout.component.scss',
})
export class DefaultLoginLayoutComponent {
  @Input() title = '';
  @Input() primaryBtnText = '';
  @Input() secondaryBtnText = '';
  @Input() disablePrimaryBtn = true;
  @Input() loading = false;
  @Output('submit') onSubmit = new EventEmitter<void>();

  @Output('navigate') onNavigate = new EventEmitter<void>();

  submit() {
    this.onSubmit.emit();
  }

  navigate() {
    this.onNavigate.emit();
  }
}
