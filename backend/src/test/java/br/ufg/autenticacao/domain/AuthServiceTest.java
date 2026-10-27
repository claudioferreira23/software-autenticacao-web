package br.ufg.autenticacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.ufg.autenticacao.domain.fakes.FakePasswordHasher;
import br.ufg.autenticacao.domain.fakes.InMemoryUserRepository;

class AuthServiceTest {

	private static final UUID ID_MARIA = UUID.fromString("7f1c2a54-3b9e-4c1d-9a2f-5e6b7c8d9e0f");
	private static final String EMAIL_MARIA = "maria@exemplo.com";
	private static final String SENHA_MARIA = "Senha-Correta-123";

	private InMemoryUserRepository userRepository;
	private FakePasswordHasher passwordHasher;
	private AuthService authService;

	@BeforeEach
	void configurar() {
		userRepository = new InMemoryUserRepository();
		userRepository.adicionar(new Usuario(ID_MARIA, "Maria", EMAIL_MARIA, FakePasswordHasher.hashDe(SENHA_MARIA)));
		passwordHasher = new FakePasswordHasher();
		authService = new AuthService(userRepository, passwordHasher);
	}

	@Test
	void autenticaComCredenciaisCorretas() {
		ResultadoAutenticacao resultado = authService.autenticar(EMAIL_MARIA, SENHA_MARIA);

		assertThat(resultado.autenticado()).isTrue();
		assertThat(resultado.identidade()).isEqualTo(new IdentidadeUsuario(ID_MARIA, "Maria", EMAIL_MARIA));
	}

	@Test
	void recusaSenhaErrada() {
		ResultadoAutenticacao resultado = authService.autenticar(EMAIL_MARIA, "senha-errada");

		assertThat(resultado.autenticado()).isFalse();
		assertThat(resultado.identidade()).isNull();
	}

	@Test
	void recusaEmailInexistenteVerificandoContraOHashFicticio() {
		ResultadoAutenticacao resultado = authService.autenticar("ninguem@exemplo.com", SENHA_MARIA);

		assertThat(resultado.autenticado()).isFalse();
		assertThat(resultado.identidade()).isNull();
		assertThat(passwordHasher.hashesGerados()).hasSize(1);
		assertThat(passwordHasher.hashesVerificados()).containsExactly(passwordHasher.hashesGerados().get(0));
	}

	@Test
	void verificaASenhaExatamenteUmaVezComUsuarioExistenteOuInexistente() {
		authService.autenticar(EMAIL_MARIA, "senha-errada");
		assertThat(passwordHasher.hashesVerificados()).hasSize(1);

		authService.autenticar("ninguem@exemplo.com", "senha-errada");
		assertThat(passwordHasher.hashesVerificados()).hasSize(2);
	}

	@Test
	void identidadeDevolvidaNaoTemCampoDeHash() {
		assertThat(Arrays.stream(IdentidadeUsuario.class.getRecordComponents()).map(componente -> componente.getName()))
				.containsExactly("id", "nome", "email");
	}

	@Test
	void cadastraUsuarioComHashSemExporSenha() {
		IdentidadeUsuario identidade = authService.cadastrar("João", "joao@exemplo.com", "Senha-Segura-123");

		Usuario salvo = userRepository.buscarPorEmail("joao@exemplo.com").orElseThrow();
		assertThat(identidade).isEqualTo(new IdentidadeUsuario(salvo.id(), "João", "joao@exemplo.com"));
		assertThat(salvo.senhaHash()).isEqualTo(FakePasswordHasher.hashDe("Senha-Segura-123"));
	}

	@Test
	void recusaEmailJaCadastradoSemGerarNovoHash() {
		int hashesAntes = passwordHasher.hashesGerados().size();

		assertThatThrownBy(() -> authService.cadastrar("Outra Maria", EMAIL_MARIA, "Outra-Senha-123"))
				.isInstanceOf(EmailJaCadastradoException.class);
		assertThat(passwordHasher.hashesGerados()).hasSize(hashesAntes);
	}

}
