package br.ufg.autenticacao.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import tools.jackson.databind.ObjectMapper;

import br.ufg.autenticacao.api.ErroResponse;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			SecurityContextRepository securityContextRepository,
			ObjectMapper objectMapper) throws Exception {
		HttpSessionCsrfTokenRepository csrfRepository = new HttpSessionCsrfTokenRepository();
		csrfRepository.setHeaderName("X-XSRF-TOKEN");
		CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

		http
				.cors(cors -> { })
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfRepository)
						.csrfTokenRequestHandler(csrfHandler))
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository)
						.requireExplicitSave(true))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/cadastro").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								escreverErro(response, HttpStatus.UNAUTHORIZED, ErroResponse.naoAutenticado(), objectMapper))
						.accessDeniedHandler((request, response, exception) ->
								escreverErro(response, HttpStatus.FORBIDDEN, ErroResponse.acessoNegado(), objectMapper)))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.deleteCookies("JSESSIONID")
						.logoutSuccessHandler((request, response, authentication) ->
								response.setStatus(HttpStatus.NO_CONTENT.value())));

		return http.build();
	}

	private static void escreverErro(
			HttpServletResponse response,
			HttpStatus status,
			ErroResponse erro,
			ObjectMapper objectMapper) throws IOException {
		response.setStatus(status.value());
		response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getOutputStream(), erro);
	}

}
