import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from '@openng/ngx-toastr';
import { finalize } from 'rxjs';

import { DefaultLoginLayoutComponent } from '../../components/default-login-layout/default-login-layout.component';
import { PrimaryInputComponent } from '../../components/primary-input/primary-input.component';
import { AuthService } from '../../services/auth.service';

interface SignupForm {
  name: FormControl<string>;
  email: FormControl<string>;
  password: FormControl<string>;
  passwordConfirm: FormControl<string>;
}

const matchingPasswords: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmation = control.get('passwordConfirm')?.value;
  return password === confirmation ? null : { passwordsDoNotMatch: true };
};

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [DefaultLoginLayoutComponent, ReactiveFormsModule, PrimaryInputComponent],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss',
})
export class SignUpComponent {
  readonly signupForm = new FormGroup<SignupForm>(
    {
      name: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(120)],
      }),
      email: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.email, Validators.maxLength(254)],
      }),
      password: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(8), Validators.maxLength(128)],
      }),
      passwordConfirm: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
    },
    { validators: matchingPasswords },
  );

  readonly isSubmitting = signal(false);

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly toastService: ToastrService,
  ) {}

  submit() {
    if (this.signupForm.invalid || this.isSubmitting()) {
      this.signupForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const { name, email, password } = this.signupForm.getRawValue();

    this.authService
      .register(name, email, password)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.toastService.success('Conta criada com sucesso. Agora faça seu login.');
          this.router.navigate(['/login']);
        },
        error: (error: HttpErrorResponse) => this.toastService.error(this.getErrorMessage(error)),
      });
  }

  navigateToLogin() {
    this.router.navigate(['/login']);
  }

  nameErrorMessage(): string {
    const control = this.signupForm.controls.name;
    if (!control.touched) {
      return '';
    }
    if (control.hasError('required')) {
      return 'Informe seu nome.';
    }
    if (control.hasError('maxlength')) {
      return 'O nome deve ter no máximo 120 caracteres.';
    }
    return '';
  }

  emailErrorMessage(): string {
    const control = this.signupForm.controls.email;
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
    const control = this.signupForm.controls.password;
    if (!control.touched) {
      return '';
    }
    if (control.hasError('required')) {
      return 'Informe uma senha.';
    }
    if (control.hasError('minlength')) {
      return 'A senha deve ter pelo menos 8 caracteres.';
    }
    if (control.hasError('maxlength')) {
      return 'A senha deve ter no máximo 128 caracteres.';
    }
    return '';
  }

  passwordConfirmationErrorMessage(): string {
    const control = this.signupForm.controls.passwordConfirm;
    if (!control.touched) {
      return '';
    }
    if (control.hasError('required')) {
      return 'Confirme sua senha.';
    }
    if (this.signupForm.hasError('passwordsDoNotMatch')) {
      return 'As senhas precisam ser iguais.';
    }
    return '';
  }

  passwordConfirmationInvalid(): boolean {
    const control = this.signupForm.controls.passwordConfirm;
    return control.touched && (control.invalid || this.signupForm.hasError('passwordsDoNotMatch'));
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0 || [502, 503, 504].includes(error.status)) {
      return 'Não foi possível conectar ao servidor. Verifique se o backend está em execução.';
    }

    if (error.status >= 500) {
      return 'O servidor não conseguiu criar a conta. Tente novamente mais tarde.';
    }

    return typeof error.error?.mensagem === 'string'
      ? error.error.mensagem
      : 'Não foi possível criar a conta.';
  }
}
