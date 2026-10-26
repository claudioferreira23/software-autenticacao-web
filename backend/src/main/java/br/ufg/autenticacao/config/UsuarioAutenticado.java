package br.ufg.autenticacao.config;

import java.io.Serializable;
import java.util.UUID;

import br.ufg.autenticacao.domain.IdentidadeUsuario;

public record UsuarioAutenticado(UUID id, String nome, String email) implements Serializable {

	public static UsuarioAutenticado de(IdentidadeUsuario identidade) {
		return new UsuarioAutenticado(identidade.id(), identidade.nome(), identidade.email());
	}

}
