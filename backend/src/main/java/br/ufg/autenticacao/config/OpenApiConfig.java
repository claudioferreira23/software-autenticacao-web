package br.ufg.autenticacao.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI autenticacaoOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Portal DS — API de Autenticação")
						.version("1.0.0")
						.description("""
								API de cadastro e autenticação baseada em sessão HTTP.

								Para testar operações POST pelo Swagger:
								1. Execute GET /api/auth/csrf.
								2. Copie o valor `token` da resposta.
								3. Clique em **Authorize** e cole o valor no campo `csrf`.
								4. Execute cadastro ou login. O navegador manterá o cookie da sessão automaticamente.
								""")
						.license(new License().name("Uso acadêmico")))
				.components(new Components()
						.addSecuritySchemes("csrf", new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.HEADER)
								.name("X-XSRF-TOKEN")
								.description("Token retornado por GET /api/auth/csrf"))
						.addSecuritySchemes("sessao", new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.COOKIE)
								.name("JSESSIONID")
								.description("Cookie HttpOnly criado automaticamente após o login")));
	}

	@Bean
	OpenApiCustomizer documentarLogout() {
		return openApi -> openApi.path("/api/auth/logout", new PathItem().post(new Operation()
				.tags(List.of("Autenticação"))
				.summary("Encerra a sessão")
				.description("Invalida a sessão atual e remove o cookie JSESSIONID.")
				.addSecurityItem(new SecurityRequirement().addList("sessao").addList("csrf"))
				.responses(new ApiResponses()
						.addApiResponse("204", new ApiResponse().description("Logout realizado"))
						.addApiResponse("403", new ApiResponse().description("Token CSRF ausente ou inválido")))));
	}

}
