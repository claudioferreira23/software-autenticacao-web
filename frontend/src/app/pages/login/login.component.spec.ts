import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ToastrService } from '@openng/ngx-toastr';

import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';
import { LoginSuccessResponse } from '../../types/auth.types';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: { login: ReturnType<typeof vi.fn> };
  let router: Router;
  let toastService: {
    success: ReturnType<typeof vi.fn>;
    error: ReturnType<typeof vi.fn>;
  };

  const successResponse: LoginSuccessResponse = {
    autenticado: true,
    usuario: {
      id: '7f1c2a54-3b9e-4c1d-9a2f-5e6b7c8d9e0f',
      nome: 'Maria',
      email: 'maria@exemplo.com',
    },
  };

  beforeEach(async () => {
    authService = { login: vi.fn() };
    toastService = { success: vi.fn(), error: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: ToastrService, useValue: toastService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not submit an invalid form', () => {
    component.submit();

    expect(authService.login).not.toHaveBeenCalled();
    expect(component.loginForm.controls.email.touched).toBe(true);
    expect(component.loginForm.controls.password.touched).toBe(true);
  });

  it('should synchronize native fields with the reactive form', () => {
    const emailInput = fixture.nativeElement.querySelector(
      'input[name="email"]',
    ) as HTMLInputElement;
    const passwordInput = fixture.nativeElement.querySelector(
      'input[name="password"]',
    ) as HTMLInputElement;

    emailInput.value = 'maria@exemplo.com';
    emailInput.dispatchEvent(new Event('input'));
    passwordInput.value = 'Senha-Correta-123';
    passwordInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.loginForm.getRawValue()).toEqual({
      email: 'maria@exemplo.com',
      password: 'Senha-Correta-123',
    });
    expect(emailInput.value).toBe('maria@exemplo.com');
    expect(passwordInput.value).toBe('Senha-Correta-123');
    expect(component.loginForm.valid).toBe(true);
  });

  it('should send valid credentials and navigate to the protected page', () => {
    authService.login.mockReturnValue(of(successResponse));
    component.loginForm.setValue({
      email: 'maria@exemplo.com',
      password: 'Senha-Correta-123',
    });

    component.submit();

    expect(authService.login).toHaveBeenCalledWith('maria@exemplo.com', 'Senha-Correta-123');
    expect(toastService.success).toHaveBeenCalledWith('Login realizado com sucesso para Maria.');
    expect(router.navigate).toHaveBeenCalledWith(['/perfil']);
    expect(component.isSubmitting()).toBe(false);
  });

  it('should prevent duplicate submissions while the request is pending', () => {
    const response = new Subject<LoginSuccessResponse>();
    authService.login.mockReturnValue(response);
    component.loginForm.setValue({
      email: 'maria@exemplo.com',
      password: 'Senha-Correta-123',
    });

    component.submit();
    component.submit();

    expect(authService.login).toHaveBeenCalledTimes(1);
    expect(component.isSubmitting()).toBe(true);

    response.next(successResponse);
    response.complete();
    expect(component.isSubmitting()).toBe(false);
  });

  it('should present the backend message without changing field validation', () => {
    authService.login.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: {
              autenticado: false,
              erro: 'CREDENCIAIS_INVALIDAS',
              mensagem: 'E-mail ou senha inválidos.',
            },
          }),
      ),
    );
    component.loginForm.setValue({
      email: 'maria@exemplo.com',
      password: 'senha-errada',
    });

    component.submit();
    fixture.detectChanges();

    expect(toastService.error).toHaveBeenCalledWith('E-mail ou senha inválidos.');
    expect(component.loginForm.valid).toBe(true);
    expect(fixture.nativeElement.querySelectorAll('.input-wrapper.is-invalid')).toHaveLength(0);
    expect(component.isSubmitting()).toBe(false);
  });

  it('should distinguish an unavailable backend', () => {
    authService.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 502 })));
    component.loginForm.setValue({
      email: 'maria@exemplo.com',
      password: 'senha-teste',
    });

    component.submit();

    expect(toastService.error).toHaveBeenCalledWith(
      'Não foi possível conectar ao servidor. Verifique se o backend está em execução.',
    );
    expect(component.isSubmitting()).toBe(false);
  });
});
