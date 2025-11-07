package com.pedropaulo.minhas_financas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.io.IOException;

@ControllerAdvice
class GlobalExceptionHandlerTest {

	@ExceptionHandler(IOException.class)
	public ResponseEntity<String> handleIOException(IOException e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body("Erro ao gerar exportação: " + e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleGenericException(Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro inesperado: " + e.getMessage());
	}

}
