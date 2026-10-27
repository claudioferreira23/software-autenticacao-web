package br.ufg.autenticacao.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Hidden;

@Controller
@Hidden
public class DocumentacaoController {

	@GetMapping("/")
	public String inicio() {
		return "redirect:/swagger-ui.html";
	}

}
