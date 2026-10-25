package br.ufg.autenticacao.domain;

import java.util.Objects;

public record ResultadoAutenticacao(boolean autenticado, IdentidadeUsuario identidade) {

	public static ResultadoAutenticacao sucesso(IdentidadeUsuario identidade) {
		return new ResultadoAutenticacao(true, Objects.requireNonNull(identidade));
	}

	public static ResultadoAutenticacao falha() {
		return new ResultadoAutenticacao(false, null);
	}

}
