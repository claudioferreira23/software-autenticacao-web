package br.ufg.autenticacao.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class LimitadorTentativasLoginTest {

	private static final String IP = "203.0.113.10";
	private static final String EMAIL = "maria@exemplo.com";

	private static class RelogioAjustavel extends Clock {
		private Instant agora = Instant.parse("2026-01-01T00:00:00Z");

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return agora;
		}

		void avancar(Duration duracao) {
			agora = agora.plus(duracao);
		}
	}

	@Test
	void permiteTentativasDentroDoLimite() {
		RelogioAjustavel relogio = new RelogioAjustavel();
		LimitadorTentativasLogin limitador = new LimitadorTentativasLogin(relogio);

		for (int i = 0; i < 4; i++) {
			assertThat(limitador.verificarEmail(EMAIL)).isEmpty();
			limitador.registrarFalhaEmail(EMAIL);
		}

		assertThat(limitador.verificarEmail(EMAIL)).isEmpty();
	}

	@Test
	void bloqueiaPorEmailAoAtingirOLimiteEInformaTempoRestante() {
		RelogioAjustavel relogio = new RelogioAjustavel();
		LimitadorTentativasLogin limitador = new LimitadorTentativasLogin(relogio);

		for (int i = 0; i < 5; i++) {
			limitador.registrarFalhaEmail(EMAIL);
		}

		assertThat(limitador.verificarEmail(EMAIL)).hasValueSatisfying(
				restante -> assertThat(restante).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(1)));
	}

	@Test
	void bloqueiaPorIpIndependentementeDoEmail() {
		RelogioAjustavel relogio = new RelogioAjustavel();
		LimitadorTentativasLogin limitador = new LimitadorTentativasLogin(relogio);

		for (int i = 0; i < 20; i++) {
			limitador.registrarFalhaIp(IP);
		}

		assertThat(limitador.verificarIp(IP)).isPresent();
		assertThat(limitador.verificarEmail("outro@exemplo.com")).isEmpty();
	}

	@Test
	void liberaNovasTentativasAposAJanelaExpirar() {
		RelogioAjustavel relogio = new RelogioAjustavel();
		LimitadorTentativasLogin limitador = new LimitadorTentativasLogin(relogio);

		for (int i = 0; i < 5; i++) {
			limitador.registrarFalhaEmail(EMAIL);
		}
		assertThat(limitador.verificarEmail(EMAIL)).isPresent();

		relogio.avancar(Duration.ofMinutes(1).plusSeconds(1));

		assertThat(limitador.verificarEmail(EMAIL)).isEmpty();
	}

	@Test
	void sucessoLimpaOContadorDoEmailMasNaoODoIp() {
		RelogioAjustavel relogio = new RelogioAjustavel();
		LimitadorTentativasLogin limitador = new LimitadorTentativasLogin(relogio);

		for (int i = 0; i < 4; i++) {
			limitador.registrarFalhaEmail(EMAIL);
			limitador.registrarFalhaIp(IP);
		}
		limitador.registrarSucessoEmail(EMAIL);

		limitador.registrarFalhaEmail(EMAIL);
		assertThat(limitador.verificarEmail(EMAIL)).isEmpty();

		for (int i = 0; i < 16; i++) {
			limitador.registrarFalhaIp(IP);
		}
		assertThat(limitador.verificarIp(IP)).isPresent();
	}

	@Test
	void naoDistingueUsuarioInexistenteParaEfeitosDeLimite() {
		RelogioAjustavel relogio = new RelogioAjustavel();
		LimitadorTentativasLogin limitador = new LimitadorTentativasLogin(relogio);

		for (int i = 0; i < 5; i++) {
			limitador.registrarFalhaEmail("ninguem@exemplo.com");
		}

		assertThat(limitador.verificarEmail("ninguem@exemplo.com")).isPresent();
		assertThat(limitador.verificarEmail(EMAIL)).isEmpty();
	}

	@Test
	void registraFalhasConcorrentesSemPerderContagem() throws InterruptedException {
		RelogioAjustavel relogio = new RelogioAjustavel();
		LimitadorTentativasLogin limitador = new LimitadorTentativasLogin(relogio);

		int threads = 50;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch partida = new CountDownLatch(1);
		AtomicLong concluidas = new AtomicLong();
		for (int i = 0; i < threads; i++) {
			executor.submit(() -> {
				try {
					partida.await();
					limitador.registrarFalhaIp(IP);
				}
				catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
				}
				finally {
					concluidas.incrementAndGet();
				}
			});
		}
		partida.countDown();
		executor.shutdown();
		assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		assertThat(concluidas.get()).isEqualTo(threads);

		assertThat(limitador.tentativasAtuaisIp(IP)).isEqualTo(threads);
	}

}
