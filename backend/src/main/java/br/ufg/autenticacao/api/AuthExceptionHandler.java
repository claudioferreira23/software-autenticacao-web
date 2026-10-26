package br.ufg.autenticacao.api;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.ufg.autenticacao.domain.EmailJaCadastradoException;

@RestControllerAdvice(basePackageClasses = AuthExceptionHandler.class)
public class AuthExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			HttpMessageNotReadableException.class,
			HttpMediaTypeNotSupportedException.class })
	public ResponseEntity<ErroResponse> entradaInvalida() {
		return ResponseEntity.badRequest().body(ErroResponse.entradaInvalida());
	}

	@ExceptionHandler(EmailJaCadastradoException.class)
	public ResponseEntity<ErroResponse> emailJaCadastrado() {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ErroResponse.emailJaCadastrado());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroResponse> erroInterno(Exception ex) {
		UUID id = UUID.randomUUID();
		log.error("Erro ao processar autenticação [id={}]", id, ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErroResponse.erroInterno());
	}

}
