package br.ufg.autenticacao.infra;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.ufg.autenticacao.domain.UserRepository;
import br.ufg.autenticacao.domain.Usuario;

@Repository
public class JpaUserRepository implements UserRepository {

	private final UsuarioJpaRepository usuarioJpaRepository;

	public JpaUserRepository(UsuarioJpaRepository usuarioJpaRepository) {
		this.usuarioJpaRepository = usuarioJpaRepository;
	}

	@Override
	public Optional<Usuario> buscarPorEmail(String email) {
		return usuarioJpaRepository.findByEmail(email)
				.map(entidade -> new Usuario(
						entidade.getId(),
						entidade.getNome(),
						entidade.getEmail(),
						entidade.getSenhaHash()));
	}

}
