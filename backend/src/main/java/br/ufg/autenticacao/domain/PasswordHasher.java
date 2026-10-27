package br.ufg.autenticacao.domain;

public interface PasswordHasher {

	boolean verificar(String senha, String hashArmazenado);

	String gerarHash(String senha);

}
