package br.ufg.autenticacao.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class LoginRequestTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void criarValidador() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void fecharValidador() {
		validatorFactory.close();
	}

	@Test
	void normalizaEmailAntesDaValidacao() {
		LoginRequest requisicao = new LoginRequest("  Maria@Exemplo.COM ", "Senha-Correta-123");

		assertThat(requisicao.email()).isEqualTo("maria@exemplo.com");
		assertThat(validator.validate(requisicao)).isEmpty();
	}

	@Test
	void aceitaEmailNuloNoConstrutorERejeitaNaValidacao() {
		LoginRequest requisicao = new LoginRequest(null, "Senha-Correta-123");

		assertThat(requisicao.email()).isNull();
		assertThat(validator.validate(requisicao))
				.extracting(violacao -> violacao.getPropertyPath().toString())
				.contains("email");
	}

	@Test
	void naoExpoeASenhaNoToString() {
		LoginRequest requisicao = new LoginRequest("maria@exemplo.com", "Senha-Correta-123");

		assertThat(requisicao.toString()).doesNotContain("Senha-Correta-123");
	}

}
