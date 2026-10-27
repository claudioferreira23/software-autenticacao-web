import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { ToastrService } from '@openng/ngx-toastr';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { SignUpComponent } from './signup.component';

describe('SignUpComponent', () => {
  let component: SignUpComponent;
  let fixture: ComponentFixture<SignUpComponent>;
  let authService: { register: ReturnType<typeof vi.fn> };
  let toastService: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    authService = { register: vi.fn() };
    toastService = { success: vi.fn(), error: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [SignUpComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: ToastrService, useValue: toastService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SignUpComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('should require matching passwords', () => {
    component.signupForm.setValue({
      name: 'Maria',
      email: 'maria@exemplo.com',
      password: 'Senha-Correta-123',
      passwordConfirm: 'Senha-Diferente-123',
    });

    expect(component.signupForm.hasError('passwordsDoNotMatch')).toBe(true);
    component.submit();
    expect(authService.register).not.toHaveBeenCalled();
  });

  it('should register and navigate to login', () => {
    authService.register.mockReturnValue(
      of({
        id: '7f1c2a54-3b9e-4c1d-9a2f-5e6b7c8d9e0f',
        nome: 'Maria',
        email: 'maria@exemplo.com',
      }),
    );
    component.signupForm.setValue({
      name: 'Maria',
      email: 'maria@exemplo.com',
      password: 'Senha-Correta-123',
      passwordConfirm: 'Senha-Correta-123',
    });

    component.submit();

    expect(authService.register).toHaveBeenCalledWith(
      'Maria',
      'maria@exemplo.com',
      'Senha-Correta-123',
    );
    expect(toastService.success).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should present the conflict message without changing field validation', () => {
    authService.register.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: {
              erro: 'EMAIL_JA_CADASTRADO',
              mensagem: 'Já existe uma conta com este e-mail.',
            },
          }),
      ),
    );
    component.signupForm.setValue({
      name: 'Maria',
      email: 'maria@exemplo.com',
      password: 'Senha-Correta-123',
      passwordConfirm: 'Senha-Correta-123',
    });

    component.submit();
    fixture.detectChanges();

    expect(toastService.error).toHaveBeenCalledWith('Já existe uma conta com este e-mail.');
    expect(component.signupForm.valid).toBe(true);
    expect(fixture.nativeElement.querySelector('.input-wrapper.is-invalid')).toBeNull();
  });
});
