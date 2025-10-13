package com.pedropaulo.minhasFinancas.exception;

public class Autenticacao extends RuntimeException {
    public Autenticacao(String message) {
        super(message);
    }
}
