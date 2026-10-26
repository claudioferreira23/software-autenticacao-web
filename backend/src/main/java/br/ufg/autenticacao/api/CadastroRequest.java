package br.ufg.autenticacao.api;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
		@NotBlank @Size(max = 120) String nome,
		@NotBlank @Email @Size(max = 254) String email,
		@NotEmpty @Size(min = 8, max = 128) String senha) {

	public CadastroRequest {
		if (nome != null) {
			nome = nome.trim();
		}
		if (email != null) {
			email = email.trim().toLowerCase(Locale.ROOT);
		}
	}

	@Override
	public String toString() {
		return "CadastroRequest[nome=" + nome + ", email=" + email + ", senha=***]";
	}

}
