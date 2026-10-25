package br.ufg.autenticacao.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class Argon2PasswordHasherTest {

	private static final String SENHA = "Senha-Correta-123";

	private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

	@Test
	void verificaSenhaCorreta() {
		String hash = hasher.gerarHash(SENHA);

		assertThat(hasher.verificar(SENHA, hash)).isTrue();
	}

	@Test
	void rejeitaSenhaErrada() {
		String hash = hasher.gerarHash(SENHA);

		assertThat(hasher.verificar("senha-errada", hash)).isFalse();
	}

	@Test
	void geraHashesDiferentesParaAMesmaSenha() {
		assertThat(hasher.gerarHash(SENHA)).isNotEqualTo(hasher.gerarHash(SENHA));
	}

	@Test
	void geraHashArgon2idComOsParametrosDoProjeto() {
		assertThat(hasher.gerarHash(SENHA)).startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
	}

	@Test
	void rejeitaHashMalformadoSemRegistrarHashNemSenha(CapturedOutput saida) {
		String hashMalformado = "hash-malformado-xyz";

		assertThat(hasher.verificar(SENHA, hashMalformado)).isFalse();
		assertThat(saida.getOut()).contains("WARN")
				.doesNotContain(hashMalformado)
				.doesNotContain(SENHA);
	}

}
