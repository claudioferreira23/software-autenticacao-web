package br.ufg.autenticacao.infra;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

import br.ufg.autenticacao.domain.PasswordHasher;

@Component
public class Argon2PasswordHasher implements PasswordHasher {

	private static final int TAMANHO_SAL_BYTES = 16;
	private static final int TAMANHO_HASH_BYTES = 32;
	private static final int PARALELISMO = 1;
	private static final int MEMORIA_KIB = 19456;
	private static final int ITERACOES = 2;

	private static final Logger log = LoggerFactory.getLogger(Argon2PasswordHasher.class);

	private static final Pattern FORMATO_ARGON2ID = Pattern.compile(
			"^\\$argon2id\\$v=\\d+\\$m=\\d+,t=\\d+,p=\\d+\\$[A-Za-z0-9+/]+={0,2}\\$[A-Za-z0-9+/]+={0,2}$");

	private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
			TAMANHO_SAL_BYTES, TAMANHO_HASH_BYTES, PARALELISMO, MEMORIA_KIB, ITERACOES);

	@Override
	public boolean verificar(String senha, String hashArmazenado) {
		if (hashArmazenado == null || !FORMATO_ARGON2ID.matcher(hashArmazenado).matches()) {
			log.warn("Hash de senha armazenado em formato inválido");
			return false;
		}
		try {
			return encoder.matches(senha, hashArmazenado);
		}
		catch (RuntimeException ex) {
			log.warn("Falha ao verificar hash de senha armazenado: {}", ex.getClass().getSimpleName());
			return false;
		}
	}

	@Override
	public String gerarHash(String senha) {
		return encoder.encode(senha);
	}

}
