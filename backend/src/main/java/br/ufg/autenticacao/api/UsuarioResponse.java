package br.ufg.autenticacao.api;

import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String email) {
}
