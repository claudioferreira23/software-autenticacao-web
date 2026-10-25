package br.ufg.autenticacao.api;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufg.autenticacao.domain.AuthService;
import br.ufg.autenticacao.domain.IdentidadeUsuario;
import br.ufg.autenticacao.domain.ResultadoAutenticacao;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	static final String WWW_AUTHENTICATE = "FormLogin realm=\"autenticacao\"";

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest requisicao, HttpServletRequest http) {
		ResultadoAutenticacao resultado = authService.autenticar(requisicao.email(), requisicao.senha());

		log.info("Tentativa de autenticação [horario={}, origem={}, resultado={}]",
				Instant.now(), http.getRemoteAddr(), resultado.autenticado() ? "SUCESSO" : "FALHA");

		if (resultado.autenticado()) {
			IdentidadeUsuario identidade = resultado.identidade();
			return ResponseEntity.ok(LoginResponse.sucesso(
					new UsuarioResponse(identidade.id(), identidade.nome(), identidade.email())));
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, WWW_AUTHENTICATE)
				.body(LoginResponse.credenciaisInvalidas());
	}

}
