package br.ufg.autenticacao.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(boolean autenticado, UsuarioResponse usuario, String erro, String mensagem) {

	public static LoginResponse sucesso(UsuarioResponse usuario) {
		return new LoginResponse(true, usuario, null, null);
	}

	public static LoginResponse entradaInvalida() {
		return new LoginResponse(false, null, "ENTRADA_INVALIDA", null);
	}

	public static LoginResponse credenciaisInvalidas() {
		return new LoginResponse(false, null, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.");
	}

	public static LoginResponse erroInterno() {
		return new LoginResponse(false, null, "ERRO_INTERNO", null);
	}

}
