package br.ufg.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import br.ufg.autenticacao.domain.PasswordHasher;
import br.ufg.autenticacao.infra.UsuarioEntity;
import br.ufg.autenticacao.infra.UsuarioJpaRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AutenticacaoIntegrationTest {

	private static final String EMAIL_MARIA = "maria@exemplo.com";
	private static final String SENHA_MARIA = "Senha-Correta-123";

	private record Resposta(int status, String wwwAuthenticate, String corpo) {
	}

	@LocalServerPort
	private int porta;

	@Autowired
	private UsuarioJpaRepository usuarioJpaRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	private UsuarioEntity maria;

	@BeforeEach
	void prepararUsuario() {
		usuarioJpaRepository.deleteAll();
		maria = usuarioJpaRepository.save(
				new UsuarioEntity("Maria", EMAIL_MARIA, passwordHasher.gerarHash(SENHA_MARIA)));
	}

	private Resposta login(String email, String senha) {
		return RestClient.create("http://localhost:" + porta)
				.post()
				.uri("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")
				.exchange((requisicao, resposta) -> new Resposta(
						resposta.getStatusCode().value(),
						resposta.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE),
						new String(resposta.getBody().readAllBytes(), StandardCharsets.UTF_8)));
	}

	@Test
	void autenticaComCredenciaisCorretas() {
		Resposta resposta = login(EMAIL_MARIA, SENHA_MARIA);

		assertThat(resposta.status()).isEqualTo(200);
		assertThat(resposta.corpo())
				.contains("\"autenticado\":true")
				.contains("\"id\":\"" + maria.getId() + "\"")
				.contains("\"nome\":\"Maria\"")
				.contains("\"email\":\"" + EMAIL_MARIA + "\"");
	}

	@Test
	void autenticaComEmailEmMaiusculasEComEspacos() {
		Resposta resposta = login("  Maria@Exemplo.COM ", SENHA_MARIA);

		assertThat(resposta.status()).isEqualTo(200);
	}

	@Test
	void respondeIgualParaSenhaErradaEEmailInexistente() {
		Resposta senhaErrada = login(EMAIL_MARIA, "senha-errada");
		Resposta emailInexistente = login("ninguem@exemplo.com", "senha-errada");

		assertThat(senhaErrada.status()).isEqualTo(401);
		assertThat(emailInexistente.status()).isEqualTo(401);
		assertThat(senhaErrada.wwwAuthenticate()).isEqualTo("FormLogin realm=\"autenticacao\"");
		assertThat(emailInexistente.wwwAuthenticate()).isEqualTo(senhaErrada.wwwAuthenticate());
		assertThat(emailInexistente.corpo()).isEqualTo(senhaErrada.corpo());
	}

	@Test
	void respostaDeSucessoNaoContemHash() {
		Resposta resposta = login(EMAIL_MARIA, SENHA_MARIA);

		assertThat(resposta.corpo()).doesNotContain("$argon2id$").doesNotContain("senhaHash");
	}

}
