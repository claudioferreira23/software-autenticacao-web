package br.ufg.autenticacao.domain;

import java.util.UUID;

public record IdentidadeUsuario(UUID id, String nome, String email) {
}
