package com.pedropaulo.minhasFinancas.service.impl;

import com.pedropaulo.minhasFinancas.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

public class JwtServiceImpl implements JwtService {
    @Override
    public String gerarToken(String usuario) {
        return "";
    }

    @Override
    public Claims obterClaims(String token) throws ExpiredJwtException {
        return null;
    }

    @Override
    public boolean isTokenValido(String token) {
        return false;
    }

    @Override
    public String obterLoginUsuario(String token) throws ExpiredJwtException {
        return "";
    }
}
