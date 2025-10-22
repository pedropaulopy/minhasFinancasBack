package com.pedropaulo.minhasFinancas.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

public interface JwtService {
    String gerarToken(String usuario);
    Claims obterClaims(String token) throws ExpiredJwtException;
    boolean isTokenValido(String token);
    String obterLoginUsuario(String token) throws ExpiredJwtException;
}
