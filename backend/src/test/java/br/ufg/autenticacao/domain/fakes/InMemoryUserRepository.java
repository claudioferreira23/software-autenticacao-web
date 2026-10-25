package br.ufg.autenticacao.domain.fakes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

}
