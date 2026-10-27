package br.ufg.autenticacao.domain.fakes;

import java.util.ArrayList;
import java.util.List;

import br.ufg.autenticacao.domain.PasswordHasher;

public class FakePasswordHasher implements PasswordHasher {

	private final List<String> hashesGerados = new ArrayList<>();
	private final List<String> hashesVerificados = new ArrayList<>();

	public static String hashDe(String senha) {
		return "fake-hash:" + senha;
	}

	@Override
	public boolean verificar(String senha, String hashArmazenado) {
		hashesVerificados.add(hashArmazenado);
		return hashDe(senha).equals(hashArmazenado);
	}

	@Override
	public String gerarHash(String senha) {
		String hash = hashDe(senha);
		hashesGerados.add(hash);
		return hash;
	}

	public List<String> hashesGerados() {
		return List.copyOf(hashesGerados);
	}

	public List<String> hashesVerificados() {
		return List.copyOf(hashesVerificados);
	}

}
