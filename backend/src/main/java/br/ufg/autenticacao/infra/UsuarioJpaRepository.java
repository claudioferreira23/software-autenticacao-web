package br.ufg.autenticacao.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

	Optional<UsuarioEntity> findByEmail(String email);

}
