package br.ufg.autenticacao.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class LimitadorTentativasLogin {

	private static final int MAX_TENTATIVAS_POR_EMAIL = 5;
	private static final int MAX_TENTATIVAS_POR_IP = 20;
	private static final Duration JANELA = Duration.ofMinutes(1);
	private static final int CAPACIDADE_MAXIMA_POR_MAPA = 10_000;

	private record Contador(int quantidade, Instant inicioJanela) {
	}

	private final Clock clock;
	private final Map<String, Contador> tentativasPorIp = new ConcurrentHashMap<>();
	private final Map<String, Contador> tentativasPorEmail = new ConcurrentHashMap<>();

	public LimitadorTentativasLogin(Clock clock) {
		this.clock = clock;
	}

	public Optional<Duration> verificarIp(String ip) {
		return bloqueioRestante(tentativasPorIp, ip, MAX_TENTATIVAS_POR_IP);
	}

	public Optional<Duration> verificarEmail(String email) {
		return bloqueioRestante(tentativasPorEmail, email, MAX_TENTATIVAS_POR_EMAIL);
	}

	public void registrarFalhaIp(String ip) {
		registrarFalha(tentativasPorIp, ip);
	}

	public void registrarFalhaEmail(String email) {
		registrarFalha(tentativasPorEmail, email);
	}

	public void registrarSucessoEmail(String email) {
		tentativasPorEmail.remove(email);
	}

	int tentativasAtuaisIp(String ip) {
		Contador atual = tentativasPorIp.get(ip);
		return atual == null ? 0 : atual.quantidade();
	}

	private Optional<Duration> bloqueioRestante(Map<String, Contador> mapa, String chave, int maximo) {
		Contador atual = mapa.get(chave);
		if (atual == null) {
			return Optional.empty();
		}
		Instant agora = clock.instant();
		Instant fimDaJanela = atual.inicioJanela().plus(JANELA);
		if (!agora.isBefore(fimDaJanela)) {
			return Optional.empty();
		}
		if (atual.quantidade() < maximo) {
			return Optional.empty();
		}
		return Optional.of(Duration.between(agora, fimDaJanela));
	}

	private void registrarFalha(Map<String, Contador> mapa, String chave) {
		Instant agora = clock.instant();
		limparExpiradosSeNecessario(mapa, agora);
		mapa.compute(chave, (k, atual) -> {
			if (atual == null || !agora.isBefore(atual.inicioJanela().plus(JANELA))) {
				return new Contador(1, agora);
			}
			return new Contador(atual.quantidade() + 1, atual.inicioJanela());
		});
	}

	private void limparExpiradosSeNecessario(Map<String, Contador> mapa, Instant agora) {
		if (mapa.size() < CAPACIDADE_MAXIMA_POR_MAPA) {
			return;
		}
		mapa.entrySet().removeIf(entrada -> !agora.isBefore(entrada.getValue().inicioJanela().plus(JANELA)));
	}

}
