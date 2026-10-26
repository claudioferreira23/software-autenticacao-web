package br.ufg.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import br.ufg.autenticacao.domain.PasswordHasher;
import br.ufg.autenticacao.infra.UsuarioEntity;
import br.ufg.autenticacao.infra.UsuarioJpaRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class AutenticacaoIntegrationTest {

	private static final String EMAIL_MARIA = "maria@exemplo.com";
	private static final String SENHA_MARIA = "Senha-Correta-123";

	private static final Pattern TOKEN_CSRF = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"");

	private record Resposta(int status, String wwwAuthenticate, String corpo, List<String> setCookies) {
	}

	private record SessaoCsrf(String token, String cookie) {
	}

	private record LoginComSessao(Resposta resposta, String cookies, String csrfToken) {
	}

	@LocalServerPort
	private int porta;

	@Autowired
	private UsuarioJpaRepository usuarioJpaRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@Autowired
	private Environment environment;

	private UsuarioEntity maria;

	@BeforeEach
	void prepararUsuario() {
		usuarioJpaRepository.deleteAll();
		maria = usuarioJpaRepository.save(
				new UsuarioEntity("Maria", EMAIL_MARIA, passwordHasher.gerarHash(SENHA_MARIA)));
	}

	private Resposta login(String email, String senha) {
		return loginComSessao(email, senha).resposta();
	}

	private LoginComSessao loginComSessao(String email, String senha) {
		SessaoCsrf csrf = obterCsrf();
		Resposta resposta = RestClient.create("http://localhost:" + porta)
				.post()
				.uri("/api/auth/login")
				.header(HttpHeaders.COOKIE, csrf.cookie())
				.header("X-XSRF-TOKEN", csrf.token())
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")
				.exchange((requisicao, httpResponse) -> resposta(httpResponse));
		String cookies = resposta.status() == 200
				? cookie(resposta.setCookies(), "JSESSIONID")
				: csrf.cookie();
		return new LoginComSessao(resposta, cookies, csrf.token());
	}

	private SessaoCsrf obterCsrf() {
		Resposta resposta = RestClient.create("http://localhost:" + porta)
				.get()
				.uri("/api/auth/csrf")
				.exchange((requisicao, httpResponse) -> resposta(httpResponse));
		assertThat(resposta.corpo()).contains("\"headerName\":\"X-XSRF-TOKEN\"");
		Matcher matcher = TOKEN_CSRF.matcher(resposta.corpo());
		assertThat(matcher.find()).isTrue();
		return new SessaoCsrf(matcher.group(1), cookie(resposta.setCookies(), "JSESSIONID"));
	}

	private Resposta resposta(org.springframework.http.client.ClientHttpResponse resposta) throws java.io.IOException {
		return new Resposta(
				resposta.getStatusCode().value(),
				resposta.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE),
				new String(resposta.getBody().readAllBytes(), StandardCharsets.UTF_8),
				resposta.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE));
	}

	private String cookie(List<String> setCookies, String nome) {
		return setCookies.stream()
				.filter(valor -> valor.startsWith(nome + "="))
				.map(valor -> valor.substring(0, valor.indexOf(';')))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Cookie ausente: " + nome));
	}

	private Resposta cadastrar(String nome, String email, String senha) {
		SessaoCsrf csrf = obterCsrf();
		return RestClient.create("http://localhost:" + porta)
				.post()
				.uri("/api/auth/cadastro")
				.header(HttpHeaders.COOKIE, csrf.cookie())
				.header("X-XSRF-TOKEN", csrf.token())
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")
				.exchange((requisicao, httpResponse) -> resposta(httpResponse));
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

	@Test
	void naoRegistraValoresRejeitadosNoLogQuandoAEntradaEInvalida(CapturedOutput saida) {
		String senhaRejeitada = "senha-rejeitada-" + "x".repeat(120);

		Resposta resposta = login("email-invalido", senhaRejeitada);

		assertThat(resposta.status()).isEqualTo(400);
		assertThat(environment.getProperty("spring.mvc.log-resolved-exception", Boolean.class)).isFalse();
		assertThat(saida.getOut()).doesNotContain(senhaRejeitada).doesNotContain("email-invalido");
	}

	@Test
	void executaCadastroLoginIdentidadeProtegidaELogout() {
		usuarioJpaRepository.deleteAll();

		Resposta cadastro = cadastrar("  Ana Silva  ", "  ANA@EXEMPLO.COM  ", "Senha-Segura-123");
		assertThat(cadastro.status()).isEqualTo(201);
		assertThat(cadastro.corpo())
				.contains("\"nome\":\"Ana Silva\"")
				.contains("\"email\":\"ana@exemplo.com\"")
				.doesNotContain("Senha-Segura-123", "senhaHash");

		LoginComSessao login = loginComSessao("ana@exemplo.com", "Senha-Segura-123");
		assertThat(login.resposta().status()).isEqualTo(200);

		Resposta identidade = RestClient.create("http://localhost:" + porta)
				.get()
				.uri("/api/auth/me")
				.header(HttpHeaders.COOKIE, login.cookies())
				.exchange((requisicao, httpResponse) -> resposta(httpResponse));
		assertThat(identidade.status()).isEqualTo(200);
		assertThat(identidade.corpo()).contains("\"email\":\"ana@exemplo.com\"");

		Resposta logout = RestClient.create("http://localhost:" + porta)
				.post()
				.uri("/api/auth/logout")
				.header(HttpHeaders.COOKIE, login.cookies())
				.header("X-XSRF-TOKEN", login.csrfToken())
				.exchange((requisicao, httpResponse) -> resposta(httpResponse));
		assertThat(logout.status()).isEqualTo(204);

		Resposta depoisDoLogout = RestClient.create("http://localhost:" + porta)
				.get()
				.uri("/api/auth/me")
				.header(HttpHeaders.COOKIE, login.cookies())
				.exchange((requisicao, httpResponse) -> resposta(httpResponse));
		assertThat(depoisDoLogout.status()).isEqualTo(401);
	}

	@Test
	void publicaContratoOpenApiComTodosOsEndpointsDeAutenticacao() {
		Resposta resposta = RestClient.create("http://localhost:" + porta)
				.get()
				.uri("/v3/api-docs")
				.exchange((requisicao, httpResponse) -> resposta(httpResponse));

		assertThat(resposta.status()).isEqualTo(200);
		assertThat(resposta.corpo())
				.contains("\"title\":\"Portal DS — API de Autenticação\"")
				.contains("/api/auth/csrf")
				.contains("/api/auth/cadastro")
				.contains("/api/auth/login")
				.contains("/api/auth/me")
				.contains("/api/auth/logout")
				.contains("X-XSRF-TOKEN")
				.contains("JSESSIONID");
	}

}
