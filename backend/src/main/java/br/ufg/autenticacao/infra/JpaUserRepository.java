package br.ufg.autenticacao.infra;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import br.ufg.autenticacao.domain.EmailJaCadastradoException;
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
				.map(this::paraDominio);
	}

	@Override
	public boolean existePorEmail(String email) {
		return usuarioJpaRepository.existsByEmail(email);
	}

	@Override
	public Usuario salvar(String nome, String email, String senhaHash) {
		try {
			return paraDominio(usuarioJpaRepository.saveAndFlush(new UsuarioEntity(nome, email, senhaHash)));
		}
		catch (DataIntegrityViolationException ex) {
			throw new EmailJaCadastradoException();
		}
	}

	private Usuario paraDominio(UsuarioEntity entidade) {
		return new Usuario(
				entidade.getId(),
				entidade.getNome(),
				entidade.getEmail(),
				entidade.getSenhaHash());
	}

}
