package br.ufg.autenticacao.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CadastroRequestTest {

	private static Validator validator;

	@BeforeAll
	static void configurarValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void normalizaNomeEEmail() {
		CadastroRequest request = new CadastroRequest(
				"  Maria da Silva  ", "  Maria@Exemplo.COM ", "Senha-Correta-123");

		assertThat(request.nome()).isEqualTo("Maria da Silva");
		assertThat(request.email()).isEqualTo("maria@exemplo.com");
	}

	@Test
	void exigeSenhaComPeloMenosOitoCaracteres() {
		CadastroRequest request = new CadastroRequest("Maria", "maria@exemplo.com", "1234567");

		Set<ConstraintViolation<CadastroRequest>> violations = validator.validate(request);

		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
				.contains("senha");
	}

	@Test
	void aceitaDadosValidosNosLimitesDoContrato() {
		CadastroRequest request = new CadastroRequest("M".repeat(120), "maria@exemplo.com", "a".repeat(128));

		assertThat(validator.validate(request)).isEmpty();
	}

}
