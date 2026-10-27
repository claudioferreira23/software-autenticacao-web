package br.ufg.autenticacao.domain;

import java.util.UUID;

public record Usuario(UUID id, String nome, String email, String senhaHash) {

	@Override
	public String toString() {
		return "Usuario[id=" + id + ", nome=" + nome + ", email=" + email + "]";
	}

}
