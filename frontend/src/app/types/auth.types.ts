export interface UsuarioResponse {
  id: string;
  nome: string;
  email: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface CadastroRequest {
  nome: string;
  email: string;
  senha: string;
}

export interface LoginSuccessResponse {
  autenticado: true;
  usuario: UsuarioResponse;
}

export interface CsrfResponse {
  headerName: string;
  token: string;
}
