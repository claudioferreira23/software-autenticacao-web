package br.ufg.autenticacao.api;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufg.autenticacao.domain.AuthService;
import br.ufg.autenticacao.domain.IdentidadeUsuario;
import br.ufg.autenticacao.domain.ResultadoAutenticacao;
import br.ufg.autenticacao.config.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, login e consulta da sessão do usuário")
public class AuthController {

	private static final String WWW_AUTHENTICATE = "FormLogin realm=\"autenticacao\"";

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthService authService;
	private final SecurityContextRepository securityContextRepository;

	public AuthController(AuthService authService, SecurityContextRepository securityContextRepository) {
		this.authService = authService;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/login")
	@Operation(
			summary = "Realiza login",
			description = "Valida as credenciais e cria uma sessão HTTP. Obtenha antes o token em GET /api/auth/csrf.",
			security = @SecurityRequirement(name = "csrf"))
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login realizado e sessão criada"),
			@ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
			@ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos"),
			@ApiResponse(responseCode = "403", description = "Token CSRF ausente ou inválido") })
	public ResponseEntity<LoginResponse> login(
			@Valid @RequestBody LoginRequest requisicao,
			HttpServletRequest request,
			HttpServletResponse response) {
		ResultadoAutenticacao resultado = authService.autenticar(requisicao.email(), requisicao.senha());

		log.info("Tentativa de autenticação [horario={}, origem={}, resultado={}]",
				Instant.now(), request.getRemoteAddr(), resultado.autenticado() ? "SUCESSO" : "FALHA");

		if (resultado.autenticado()) {
			IdentidadeUsuario identidade = resultado.identidade();
			registrarAutenticacao(identidade, request, response);
			return ResponseEntity.ok(LoginResponse.sucesso(paraResponse(identidade)));
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, WWW_AUTHENTICATE)
				.body(LoginResponse.credenciaisInvalidas());
	}

	@PostMapping("/cadastro")
	@Operation(
			summary = "Cadastra um usuário",
			description = "Cria uma conta usando hash Argon2id para a senha. Obtenha antes o token em GET /api/auth/csrf.",
			security = @SecurityRequirement(name = "csrf"))
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Usuário cadastrado"),
			@ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
			@ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
			@ApiResponse(responseCode = "403", description = "Token CSRF ausente ou inválido") })
	public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
		IdentidadeUsuario identidade = authService.cadastrar(
				requisicao.nome(), requisicao.email(), requisicao.senha());
		UsuarioResponse usuario = paraResponse(identidade);
		return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
	}

	@GetMapping("/me")
	@Operation(
			summary = "Consulta o usuário autenticado",
			description = "Retorna o usuário associado ao cookie de sessão atual.",
			security = @SecurityRequirement(name = "sessao"))
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Usuário autenticado"),
			@ApiResponse(responseCode = "401", description = "Sessão inexistente ou expirada") })
	public UsuarioResponse usuarioAtual(@AuthenticationPrincipal UsuarioAutenticado usuario) {
		return new UsuarioResponse(usuario.id(), usuario.nome(), usuario.email());
	}

	@GetMapping("/csrf")
	@Operation(
			summary = "Obtém um token CSRF",
			description = "Inicia ou reutiliza uma sessão e retorna o token que deve ser enviado no cabeçalho X-XSRF-TOKEN nas operações POST.")
	@ApiResponse(responseCode = "200", description = "Token CSRF emitido")
	public CsrfResponse csrf(CsrfToken token) {
		return new CsrfResponse(token.getHeaderName(), token.getToken());
	}

	private void registrarAutenticacao(
			IdentidadeUsuario identidade,
			HttpServletRequest request,
			HttpServletResponse response) {
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		UsernamePasswordAuthenticationToken autenticacao = UsernamePasswordAuthenticationToken.authenticated(
				UsuarioAutenticado.de(identidade), null, List.of());
		SecurityContext contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(autenticacao);
		SecurityContextHolder.setContext(contexto);
		securityContextRepository.saveContext(contexto, request, response);
	}

	private UsuarioResponse paraResponse(IdentidadeUsuario identidade) {
		return new UsuarioResponse(identidade.id(), identidade.nome(), identidade.email());
	}

}
