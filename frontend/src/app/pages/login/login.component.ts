import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from '@openng/ngx-toastr';
import { finalize } from 'rxjs';

import { DefaultLoginLayoutComponent } from '../../components/default-login-layout/default-login-layout.component';
import { PrimaryInputComponent } from '../../components/primary-input/primary-input.component';
import { AuthService } from '../../services/auth.service';

interface LoginForm {
  email: FormControl<string>;
  password: FormControl<string>;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [DefaultLoginLayoutComponent, ReactiveFormsModule, PrimaryInputComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  readonly loginForm = new FormGroup<LoginForm>({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(128)],
    }),
  });

  readonly isSubmitting = signal(false);

  constructor(
    private readonly authService: AuthService,
    private readonly toastService: ToastrService,
    private readonly router: Router,
  ) {}

  submit() {
    if (this.loginForm.invalid || this.isSubmitting()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const { email, password } = this.loginForm.getRawValue();

    this.authService
      .login(email, password)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.toastService.success(`Login realizado com sucesso para ${response.usuario.nome}.`);
          this.router.navigate(['/perfil']);
        },
        error: (error: HttpErrorResponse) => this.toastService.error(this.getErrorMessage(error)),
      });
  }

  navigateToRegister() {
    this.router.navigate(['/cadastro']);
  }

  emailErrorMessage(): string {
    const control = this.loginForm.controls.email;
    if (!control.touched) {
      return '';
    }
    if (control.hasError('required')) {
      return 'Informe seu e-mail.';
    }
    if (control.hasError('email')) {
      return 'Informe um endereço de e-mail válido.';
    }
    if (control.hasError('maxlength')) {
      return 'O e-mail deve ter no máximo 254 caracteres.';
    }
    return '';
  }

  passwordErrorMessage(): string {
    const control = this.loginForm.controls.password;
    if (!control.touched) {
      return '';
    }
    if (control.hasError('required')) {
      return 'Informe sua senha.';
    }
    if (control.hasError('maxlength')) {
      return 'A senha deve ter no máximo 128 caracteres.';
    }
    return '';
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0 || [502, 503, 504].includes(error.status)) {
      return 'Não foi possível conectar ao servidor. Verifique se o backend está em execução.';
    }

    if (error.status >= 500) {
      return 'O servidor não conseguiu concluir o login. Tente novamente mais tarde.';
    }

    return typeof error.error?.mensagem === 'string'
      ? error.error.mensagem
      : 'Não foi possível realizar o login.';
  }
}
