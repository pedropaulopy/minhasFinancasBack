package com.pedropaulo.minhasFinancas.service.impl;

import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey; // Import this
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.expiracao}")
    private String expiracao;

    @Value("${jwt.chave-assinatura}")
    private String chaveAssinatura;

    // Helper method to create the secure key
    private SecretKey getSigningKey() {
        // Your key in application.properties MUST be Base64 encoded
        byte[] keyBytes = Decoders.BASE64.decode(chaveAssinatura);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String gerarToken(Usuario usuario) {
        long expLong = Long.parseLong(expiracao); // Use parseLong
        LocalDateTime dataHoraExpiracao = LocalDateTime.now().plusMinutes(expLong);
        Instant instant = dataHoraExpiracao.atZone(ZoneId.systemDefault()).toInstant();
        Date data = Date.from(instant);

        // This is the new, fluent API for building the token
        String token = Jwts.builder()
                .subject(usuario.getEmail())
                .claim("nome", usuario.getNome())
                .claim("horaExpiracao", dataHoraExpiracao.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .expiration(data)
                .signWith(getSigningKey()) // New signWith method
                .compact();

        return token; // <-- This was returning null before
    }

    @Override
    public Claims obterClaims(String token) throws ExpiredJwtException {
        // This is the new API for parsing the token
        return Jwts.parser()
                .verifyWith(getSigningKey()) // New verifyWith method
                .build()
                .parseSignedClaims(token) // This replaces parseClaimsJws
                .getPayload();
    }

    @Override
    public boolean isTokenValido(String token) {
        try {
            Claims claims = obterClaims(token);
            Date exp = claims.getExpiration();
            LocalDateTime dataExpiracao = exp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

            // Simplified logic
            return LocalDateTime.now().isBefore(dataExpiracao);

        } catch (Exception e) { // Catch all parsing/validation errors
            return false;
        }
    }

    @Override
    public String obterLoginUsuario(String token) throws ExpiredJwtException {
        Claims claims = obterClaims(token);
        return claims.getSubject();
    }
}