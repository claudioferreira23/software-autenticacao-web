import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';
import { LoginSuccessResponse, UsuarioResponse } from '../types/auth.types';

describe('AuthService', () => {
  let service: AuthService;
  let httpController: HttpTestingController;

  const user: UsuarioResponse = {
    id: '7f1c2a54-3b9e-4c1d-9a2f-5e6b7c8d9e0f',
    nome: 'Maria',
    email: 'maria@exemplo.com',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpController.verify());

  it('should restore the authenticated user', () => {
    service.initialize().subscribe((result) => expect(result).toEqual(user));

    const meRequest = httpController.expectOne('/api/auth/me');
    expect(meRequest.request.method).toBe('GET');
    expect(meRequest.request.withCredentials).toBe(true);
    meRequest.flush(user);

    expect(service.currentUser()).toEqual(user);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should treat an unauthenticated initialization as a guest session', () => {
    service.initialize().subscribe((result) => expect(result).toBeNull());
    httpController
      .expectOne('/api/auth/me')
      .flush({ erro: 'NAO_AUTENTICADO' }, { status: 401, statusText: 'Unauthorized' });

    expect(service.currentUser()).toBeNull();
  });

  it('should send the login contract and retain the user', () => {
    const response: LoginSuccessResponse = { autenticado: true, usuario: user };
    service.login('maria@exemplo.com', 'Senha-Correta-123').subscribe();

    httpController
      .expectOne('/api/auth/csrf')
      .flush({ headerName: 'X-XSRF-TOKEN', token: 'csrf-token' });
    const request = httpController.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toEqual({
      email: 'maria@exemplo.com',
      senha: 'Senha-Correta-123',
    });
    request.flush(response);

    expect(service.currentUser()).toEqual(user);
  });

  it('should use the CSRF header name returned by the backend', () => {
    service.login('maria@exemplo.com', 'Senha-Correta-123').subscribe();
    httpController.expectOne('/api/auth/csrf').flush({
      headerName: 'X-CUSTOM-CSRF',
      token: 'fresh-token',
    });
    const request = httpController.expectOne('/api/auth/login');
    expect(request.request.headers.get('X-CUSTOM-CSRF')).toBe('fresh-token');
    request.flush({ autenticado: true, usuario: user });
  });

  it('should send the registration contract', () => {
    service.register('Maria', 'maria@exemplo.com', 'Senha-Correta-123').subscribe();

    httpController
      .expectOne('/api/auth/csrf')
      .flush({ headerName: 'X-XSRF-TOKEN', token: 'csrf-token' });
    const request = httpController.expectOne('/api/auth/cadastro');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      nome: 'Maria',
      email: 'maria@exemplo.com',
      senha: 'Senha-Correta-123',
    });
    request.flush(user);
  });

  it('should clear the user after logout', () => {
    service.login('maria@exemplo.com', 'Senha-Correta-123').subscribe();
    httpController
      .expectOne('/api/auth/csrf')
      .flush({ headerName: 'X-XSRF-TOKEN', token: 'login-token' });
    httpController.expectOne('/api/auth/login').flush({ autenticado: true, usuario: user });

    service.logout().subscribe();
    httpController
      .expectOne('/api/auth/csrf')
      .flush({ headerName: 'X-XSRF-TOKEN', token: 'logout-token' });
    const logoutRequest = httpController.expectOne('/api/auth/logout');
    expect(logoutRequest.request.method).toBe('POST');
    expect(logoutRequest.request.headers.get('X-XSRF-TOKEN')).toBe('logout-token');
    logoutRequest.flush(null);

    expect(service.currentUser()).toBeNull();
  });
});
