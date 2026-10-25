package br.ufg.autenticacao.domain;

import java.util.Optional;

public interface UserRepository {

	Optional<Usuario> buscarPorEmail(String email);

}
