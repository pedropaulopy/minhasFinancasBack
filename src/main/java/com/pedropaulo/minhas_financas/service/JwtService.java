package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.stereotype.Service;

public interface JwtService {

	String gerarToken(Usuario usuario);

	Claims obterClaims(String token) throws ExpiredJwtException;

	boolean isTokenValido(String token);

	String obterLoginUsuario(String token) throws ExpiredJwtException;

}
