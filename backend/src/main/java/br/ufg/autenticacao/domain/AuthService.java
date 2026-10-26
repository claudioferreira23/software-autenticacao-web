package br.ufg.autenticacao.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class AuthService {

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;
	private final String hashFicticio;

	public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
		this.userRepository = Objects.requireNonNull(userRepository);
		this.passwordHasher = Objects.requireNonNull(passwordHasher);
		this.hashFicticio = passwordHasher.gerarHash(UUID.randomUUID().toString());
	}

	public ResultadoAutenticacao autenticar(String email, String senha) {
		Optional<Usuario> usuario = userRepository.buscarPorEmail(email);
		String hashParaVerificar = usuario.map(Usuario::senhaHash).orElse(hashFicticio);
		boolean senhaConfere = passwordHasher.verificar(senha, hashParaVerificar);

		if (usuario.isPresent() && senhaConfere) {
			Usuario encontrado = usuario.get();
			return ResultadoAutenticacao.sucesso(
					new IdentidadeUsuario(encontrado.id(), encontrado.nome(), encontrado.email()));
		}
		return ResultadoAutenticacao.falha();
	}

	public IdentidadeUsuario cadastrar(String nome, String email, String senha) {
		if (userRepository.existePorEmail(email)) {
			throw new EmailJaCadastradoException();
		}

		Usuario salvo = userRepository.salvar(nome, email, passwordHasher.gerarHash(senha));
		return new IdentidadeUsuario(salvo.id(), salvo.nome(), salvo.email());
	}

}
