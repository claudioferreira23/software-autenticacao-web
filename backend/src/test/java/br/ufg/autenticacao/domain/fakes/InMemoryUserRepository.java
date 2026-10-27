package br.ufg.autenticacao.domain.fakes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.ufg.autenticacao.domain.UserRepository;
import br.ufg.autenticacao.domain.Usuario;

public class InMemoryUserRepository implements UserRepository {

	private final Map<String, Usuario> usuariosPorEmail = new HashMap<>();

	public void adicionar(Usuario usuario) {
		usuariosPorEmail.put(usuario.email(), usuario);
	}

	@Override
	public Optional<Usuario> buscarPorEmail(String email) {
		return Optional.ofNullable(usuariosPorEmail.get(email));
	}

	@Override
	public boolean existePorEmail(String email) {
		return usuariosPorEmail.containsKey(email);
	}

	@Override
	public Usuario salvar(String nome, String email, String senhaHash) {
		Usuario usuario = new Usuario(UUID.randomUUID(), nome, email, senhaHash);
		adicionar(usuario);
		return usuario;
	}

}
