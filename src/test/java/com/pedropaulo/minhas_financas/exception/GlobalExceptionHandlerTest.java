package com.pedropaulo.minhas_financas.exception;

import com.pedropaulo.minhas_financas.api.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleIOException_retornaInternalServerError_comMensagem() {
		IOException excecao = new IOException("falha de IO");
		ResponseEntity<String> resposta = handler.handleIOException(excecao);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resposta.getBody()).isEqualTo("Erro ao gerar exportação: falha de IO");
	}

	@Test
	void handleGenericException_retornaInternalServerError_comMensagem() {
		Exception excecao = new RuntimeException("erro genérico");
		ResponseEntity<String> resposta = handler.handleGenericException(excecao);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resposta.getBody()).isEqualTo("Erro inesperado: erro genérico");
	}

}
