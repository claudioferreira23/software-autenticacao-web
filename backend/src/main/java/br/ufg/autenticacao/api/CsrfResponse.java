package br.ufg.autenticacao.api;

public record CsrfResponse(String headerName, String token) {
}
