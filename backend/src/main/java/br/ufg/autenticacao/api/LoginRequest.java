package br.ufg.autenticacao.api;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotEmpty @Size(max = 128) String senha) {

	public LoginRequest {
		if (email != null) {
			email = email.trim().toLowerCase(Locale.ROOT);
		}
	}

	@Override
	public String toString() {
		return "LoginRequest[email=" + email + ", senha=***]";
	}

}
