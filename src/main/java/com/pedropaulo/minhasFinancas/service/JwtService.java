package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.stereotype.Service;

@Service
public interface JwtService {
    String gerarToken(Usuario usuario);

    Claims obterClaims(String token) throws ExpiredJwtException;
    boolean isTokenValido(String token);
    String obterLoginUsuario(String token) throws ExpiredJwtException;
}
