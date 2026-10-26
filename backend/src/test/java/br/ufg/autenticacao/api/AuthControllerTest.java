package br.ufg.autenticacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import br.ufg.autenticacao.config.CorsConfig;
import br.ufg.autenticacao.config.SecurityConfig;
import br.ufg.autenticacao.domain.AuthService;
import br.ufg.autenticacao.domain.EmailJaCadastradoException;
import br.ufg.autenticacao.domain.IdentidadeUsuario;
import br.ufg.autenticacao.domain.ResultadoAutenticacao;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, CorsConfig.class })
@ExtendWith(OutputCaptureExtension.class)
class AuthControllerTest {

	private static final String URL = "/api/auth/login";
	private static final UUID ID_MARIA = UUID.fromString("7f1c2a54-3b9e-4c1d-9a2f-5e6b7c8d9e0f");
	private static final String EMAIL_MARIA = "maria@exemplo.com";
	private static final String SENHA_MARIA = "Senha-Correta-123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	private static String corpo(String email, String senha) {
		return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
	}

	private void autenticacaoComSucesso() {
		given(authService.autenticar(anyString(), anyString())).willReturn(
				ResultadoAutenticacao.sucesso(new IdentidadeUsuario(ID_MARIA, "Maria", EMAIL_MARIA)));
	}

	@Test
	void devolve200ComAIdentidadeDoUsuario() throws Exception {
		autenticacaoComSucesso();

		mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo(EMAIL_MARIA, SENHA_MARIA)))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"autenticado":true,"usuario":{"id":"7f1c2a54-3b9e-4c1d-9a2f-5e6b7c8d9e0f","nome":"Maria","email":"maria@exemplo.com"}}
						""", JsonCompareMode.STRICT))
				.andExpect(jsonPath("$.senhaHash").doesNotExist())
				.andExpect(jsonPath("$.erro").doesNotExist());
	}

	@Test
	void devolve401ComCorpoECabecalhoDoContrato() throws Exception {
		given(authService.autenticar(anyString(), anyString())).willReturn(ResultadoAutenticacao.falha());

		mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo(EMAIL_MARIA, "senha-errada")))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "FormLogin realm=\"autenticacao\""))
				.andExpect(content().json("""
						{"autenticado":false,"erro":"CREDENCIAIS_INVALIDAS","mensagem":"E-mail ou senha inválidos."}
						""", JsonCompareMode.STRICT));
	}

	static Stream<Arguments> entradasInvalidas() {
		return Stream.of(
				Arguments.of("e-mail ausente", MediaType.APPLICATION_JSON_VALUE, "{\"senha\":\"Senha-Correta-123\"}"),
				Arguments.of("e-mail sem arroba", MediaType.APPLICATION_JSON_VALUE, corpo("maria.exemplo.com", SENHA_MARIA)),
				Arguments.of("senha vazia", MediaType.APPLICATION_JSON_VALUE, corpo(EMAIL_MARIA, "")),
				Arguments.of("senha com 129 caracteres", MediaType.APPLICATION_JSON_VALUE, corpo(EMAIL_MARIA, "a".repeat(129))),
				Arguments.of("e-mail com 255 caracteres", MediaType.APPLICATION_JSON_VALUE, corpo(emailComTamanho(255), SENHA_MARIA)),
				Arguments.of("JSON malformado", MediaType.APPLICATION_JSON_VALUE, "{\"email\":"),
				Arguments.of("corpo vazio", MediaType.APPLICATION_JSON_VALUE, ""),
				Arguments.of("Content-Type text/plain", MediaType.TEXT_PLAIN_VALUE, corpo(EMAIL_MARIA, SENHA_MARIA)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("entradasInvalidas")
	void devolve400ParaEntradaInvalida(String caso, String contentType, String conteudo) throws Exception {
		mockMvc.perform(post(URL).with(csrf()).contentType(contentType).content(conteudo))
				.andExpect(status().isBadRequest())
				.andExpect(content().json("""
						{"erro":"ENTRADA_INVALIDA"}
						""", JsonCompareMode.STRICT));
	}

	@Test
	void aceitaSenhaComExatamente128Caracteres() throws Exception {
		autenticacaoComSucesso();

		mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo(EMAIL_MARIA, "a".repeat(128))))
				.andExpect(status().isOk());
	}

	@Test
	void aceitaEmailComExatamente254Caracteres() throws Exception {
		autenticacaoComSucesso();

		mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo(emailComTamanho(254), SENHA_MARIA)))
				.andExpect(status().isOk());
	}

	@Test
	void repassaOEmailNormalizadoAoAuthService() throws Exception {
		autenticacaoComSucesso();

		mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo("  Maria@Exemplo.COM ", SENHA_MARIA)))
				.andExpect(status().isOk());

		verify(authService).autenticar(EMAIL_MARIA, SENHA_MARIA);
	}

	@Test
	void devolve500SemDetalhesTecnicosERegistraIdDeCorrelacao(CapturedOutput saida) throws Exception {
		given(authService.autenticar(anyString(), anyString()))
				.willThrow(new IllegalStateException("detalhe-interno-do-servidor"));

		String resposta = mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo(EMAIL_MARIA, SENHA_MARIA)))
				.andExpect(status().isInternalServerError())
				.andExpect(content().json("""
						{"erro":"ERRO_INTERNO"}
						""", JsonCompareMode.STRICT))
				.andReturn().getResponse().getContentAsString();

		assertThat(resposta).doesNotContain("detalhe-interno-do-servidor", "IllegalStateException", "id");
		assertThat(saida.getOut()).containsPattern("Erro ao processar autenticação \\[id=[0-9a-f-]{36}\\]");
	}

	@Test
	void liberaPreflightCorsParaAOrigemDoFrontend() throws Exception {
		mockMvc.perform(options(URL)
						.header(HttpHeaders.ORIGIN, "http://localhost:4200")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
	}

	@Test
	void recusaPreflightCorsDeOrigemNaoListada() throws Exception {
		mockMvc.perform(options(URL)
						.header(HttpHeaders.ORIGIN, "http://outra-origem.exemplo")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void registraUmaLinhaPorTentativaSemEmailESenha(CapturedOutput saida) throws Exception {
		autenticacaoComSucesso();
		mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo(EMAIL_MARIA, SENHA_MARIA)));

		given(authService.autenticar(anyString(), anyString())).willReturn(ResultadoAutenticacao.falha());
		mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo(EMAIL_MARIA, "senha-errada")));

		String log = saida.getOut();
		assertThat(log.lines().filter(linha -> linha.contains("Tentativa de autenticação"))).hasSize(2);
		assertThat(log).contains("resultado=SUCESSO", "resultado=FALHA")
				.doesNotContain(EMAIL_MARIA, SENHA_MARIA, "senha-errada");
	}

	@Test
	void criaSessaoRecuperaIdentidadeEEncerraSessao() throws Exception {
		autenticacaoComSucesso();

		MvcResult login = mockMvc.perform(post(URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo(EMAIL_MARIA, SENHA_MARIA)))
				.andExpect(status().isOk())
				.andReturn();

		mockMvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) login.getRequest().getSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL_MARIA));

		mockMvc.perform(post("/api/auth/logout")
				.session((org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false))
				.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
	}

	@Test
	void cadastraUsuarioNormalizado() throws Exception {
		given(authService.cadastrar("Maria", EMAIL_MARIA, SENHA_MARIA))
				.willReturn(new IdentidadeUsuario(ID_MARIA, "Maria", EMAIL_MARIA));

		mockMvc.perform(post("/api/auth/cadastro").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"  Maria  ","email":"  Maria@Exemplo.COM ","senha":"Senha-Correta-123"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(EMAIL_MARIA))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void devolve409ParaEmailJaCadastrado() throws Exception {
		given(authService.cadastrar(anyString(), anyString(), anyString()))
				.willThrow(new EmailJaCadastradoException());

		mockMvc.perform(post("/api/auth/cadastro").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Maria","email":"maria@exemplo.com","senha":"Senha-Correta-123"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.erro").value("EMAIL_JA_CADASTRADO"));
	}

	@Test
	void exigeCsrfEmOperacoesQueAlteramEstado() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpo(EMAIL_MARIA, SENHA_MARIA)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"));
	}

	@Test
	void disponibilizaTokenCsrf() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName").isNotEmpty())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	private static String emailComTamanho(int tamanho) {
		String local = "u".repeat(64);
		String rotulos = (("d".repeat(50)) + ".").repeat(3);
		int restante = tamanho - local.length() - 1 - rotulos.length() - ".com".length();
		return local + "@" + rotulos + "e".repeat(restante) + ".com";
	}

}
