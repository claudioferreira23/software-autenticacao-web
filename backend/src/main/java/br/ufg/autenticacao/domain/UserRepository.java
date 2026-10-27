package br.ufg.autenticacao.domain;

import java.util.Optional;

public interface UserRepository {

	Optional<Usuario> buscarPorEmail(String email);

	boolean existePorEmail(String email);

	Usuario salvar(String nome, String email, String senhaHash);

}
