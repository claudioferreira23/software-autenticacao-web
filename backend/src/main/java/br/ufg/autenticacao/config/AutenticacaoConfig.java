package br.ufg.autenticacao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.ufg.autenticacao.domain.AuthService;
import br.ufg.autenticacao.domain.PasswordHasher;
import br.ufg.autenticacao.domain.UserRepository;

@Configuration
public class AutenticacaoConfig {

	@Bean
	AuthService authService(UserRepository userRepository, PasswordHasher passwordHasher) {
		return new AuthService(userRepository, passwordHasher);
	}

}
