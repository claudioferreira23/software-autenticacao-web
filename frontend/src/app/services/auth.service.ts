import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';

import {
  CadastroRequest,
  CsrfResponse,
  LoginRequest,
  LoginSuccessResponse,
  UsuarioResponse,
} from '../types/auth.types';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly currentUserState = signal<UsuarioResponse | null>(null);
  private readonly requestOptions = { withCredentials: true } as const;

  readonly currentUser = this.currentUserState.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserState() !== null);

  constructor(private readonly httpClient: HttpClient) {}

  initialize(): Observable<UsuarioResponse | null> {
    return this.getCurrentUser().pipe(
      catchError(() => {
        this.currentUserState.set(null);
        return of(null);
      }),
    );
  }

  login(email: string, senha: string): Observable<LoginSuccessResponse> {
    const request: LoginRequest = { email, senha };
    return this.getCsrfRequestOptions().pipe(
      switchMap((options) =>
        this.httpClient.post<LoginSuccessResponse>('/api/auth/login', request, options),
      ),
      tap((response) => this.currentUserState.set(response.usuario)),
    );
  }

  register(nome: string, email: string, senha: string): Observable<UsuarioResponse> {
    const request: CadastroRequest = { nome, email, senha };
    return this.getCsrfRequestOptions().pipe(
      switchMap((options) =>
        this.httpClient.post<UsuarioResponse>('/api/auth/cadastro', request, options),
      ),
    );
  }

  logout(): Observable<void> {
    return this.getCsrfRequestOptions().pipe(
      switchMap((options) => this.httpClient.post<void>('/api/auth/logout', null, options)),
      tap(() => this.currentUserState.set(null)),
    );
  }

  private getCurrentUser(): Observable<UsuarioResponse> {
    return this.httpClient
      .get<UsuarioResponse>('/api/auth/me', this.requestOptions)
      .pipe(tap((user) => this.currentUserState.set(user)));
  }

  private getCsrfRequestOptions() {
    return this.httpClient.get<CsrfResponse>('/api/auth/csrf', this.requestOptions).pipe(
      map(({ headerName, token }) => ({
        ...this.requestOptions,
        headers: { [headerName]: token },
      })),
    );
  }
}
