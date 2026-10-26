package br.ufg.autenticacao.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(String erro, String mensagem) {

	public static ErroResponse entradaInvalida() {
		return new ErroResponse("ENTRADA_INVALIDA", null);
	}

	public static ErroResponse emailJaCadastrado() {
		return new ErroResponse("EMAIL_JA_CADASTRADO", "Já existe uma conta com este e-mail.");
	}

	public static ErroResponse naoAutenticado() {
		return new ErroResponse("NAO_AUTENTICADO", "É necessário realizar login.");
	}

	public static ErroResponse acessoNegado() {
		return new ErroResponse("ACESSO_NEGADO", "Acesso negado.");
	}

	public static ErroResponse erroInterno() {
		return new ErroResponse("ERRO_INTERNO", null);
	}

}
