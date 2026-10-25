package br.ufg.autenticacao.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import br.ufg.autenticacao.TestcontainersConfiguration;
import br.ufg.autenticacao.domain.Usuario;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, JpaUserRepository.class })
class JpaUserRepositoryTest {

	@Autowired
	private UsuarioJpaRepository usuarioJpaRepository;

	@Autowired
	private JpaUserRepository jpaUserRepository;

	@Test
	void encontraUsuarioExistentePeloEmail() {
		UsuarioEntity salvo = usuarioJpaRepository.saveAndFlush(
				new UsuarioEntity("Maria", "maria@exemplo.com", "hash-da-maria"));

		Optional<Usuario> encontrado = jpaUserRepository.buscarPorEmail("maria@exemplo.com");

		assertThat(encontrado).hasValueSatisfying(usuario -> {
			assertThat(usuario.id()).isEqualTo(salvo.getId()).isNotNull();
			assertThat(usuario.nome()).isEqualTo("Maria");
			assertThat(usuario.email()).isEqualTo("maria@exemplo.com");
			assertThat(usuario.senhaHash()).isEqualTo("hash-da-maria");
		});
	}

	@Test
	void retornaVazioParaEmailInexistente() {
		usuarioJpaRepository.saveAndFlush(new UsuarioEntity("Maria", "maria@exemplo.com", "hash-da-maria"));

		assertThat(jpaUserRepository.buscarPorEmail("ninguem@exemplo.com")).isEmpty();
	}

	@Test
	void trataEmailComTentativaDeInjecaoSqlComoValorLiteral() {
		usuarioJpaRepository.saveAndFlush(new UsuarioEntity("Maria", "maria@exemplo.com", "hash-da-maria"));

		assertThat(jpaUserRepository.buscarPorEmail("x' OR '1'='1")).isEmpty();
	}

}
