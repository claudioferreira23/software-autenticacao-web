package br.ufg.autenticacao.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import br.ufg.autenticacao.TestcontainersConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UsuariosMigrationTest {

	private static final String INSERE_USUARIO =
			"INSERT INTO usuarios (nome, email, senha_hash) VALUES (?, ?, ?)";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void criaTabelaUsuariosComAsColunasEsperadas() {
		List<Map<String, Object>> colunas = jdbcTemplate.queryForList("""
				SELECT column_name, data_type, character_maximum_length, is_nullable
				FROM information_schema.columns
				WHERE table_schema = 'public' AND table_name = 'usuarios'
				ORDER BY ordinal_position
				""");

		assertThat(colunas)
				.extracting(
						coluna -> coluna.get("column_name"),
						coluna -> coluna.get("data_type"),
						coluna -> coluna.get("character_maximum_length"),
						coluna -> coluna.get("is_nullable"))
				.containsExactly(
						tuple("id", "uuid", null, "NO"),
						tuple("nome", "character varying", 120, "NO"),
						tuple("email", "character varying", 254, "NO"),
						tuple("senha_hash", "character varying", 255, "NO"));
	}

	@Test
	void rejeitaEmailDuplicado() {
		jdbcTemplate.update(INSERE_USUARIO, "Maria", "maria@exemplo.com", "hash");

		assertThatThrownBy(() -> jdbcTemplate.update(INSERE_USUARIO, "Outra Maria", "maria@exemplo.com", "hash"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uk_usuarios_email");
	}

	@Test
	void rejeitaEmailComLetraMaiuscula() {
		assertThatThrownBy(() -> jdbcTemplate.update(INSERE_USUARIO, "Maria", "Maria@exemplo.com", "hash"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_usuarios_email_minusculo");
	}

}
