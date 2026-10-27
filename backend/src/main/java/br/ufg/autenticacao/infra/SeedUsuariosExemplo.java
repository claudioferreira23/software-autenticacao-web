package br.ufg.autenticacao.infra;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.ufg.autenticacao.domain.PasswordHasher;

@Component
@Profile("dev")
public class SeedUsuariosExemplo implements CommandLineRunner {

	private record UsuarioExemplo(String nome, String email) {
	}

	private static final List<UsuarioExemplo> USUARIOS_EXEMPLO = List.of(
			new UsuarioExemplo("Maria Exemplo", "maria@exemplo.com"),
			new UsuarioExemplo("João Exemplo", "joao@exemplo.com"));

	private static final Logger log = LoggerFactory.getLogger(SeedUsuariosExemplo.class);

	private final UsuarioJpaRepository usuarioJpaRepository;
	private final PasswordHasher passwordHasher;
	private final String senhaPadrao;

	public SeedUsuariosExemplo(
			UsuarioJpaRepository usuarioJpaRepository,
			PasswordHasher passwordHasher,
			@Value("${SEED_SENHA_PADRAO}") String senhaPadrao) {
		this.usuarioJpaRepository = usuarioJpaRepository;
		this.passwordHasher = passwordHasher;
		this.senhaPadrao = senhaPadrao;
	}

	@Override
	@Transactional
	public void run(String... args) {
		int criados = 0;
		for (UsuarioExemplo exemplo : USUARIOS_EXEMPLO) {
			if (usuarioJpaRepository.findByEmail(exemplo.email()).isEmpty()) {
				usuarioJpaRepository.save(new UsuarioEntity(
						exemplo.nome(), exemplo.email(), passwordHasher.gerarHash(senhaPadrao)));
				criados++;
			}
		}
		log.info("Usuários de exemplo criados: {}", criados);
	}

}
