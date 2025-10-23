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

import javax.crypto.SecretKey;
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

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(chaveAssinatura);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String gerarToken(Usuario usuario) {
        long expLong = Long.parseLong(expiracao);
        LocalDateTime dataHoraExpiracao = LocalDateTime.now().plusMinutes(expLong);
        Instant instant = dataHoraExpiracao.atZone(ZoneId.systemDefault()).toInstant();
        Date data = Date.from(instant);

        String token = Jwts.builder()
                .subject(usuario.getEmail())
                .claim("nome", usuario.getNome())
                .claim("idUsuario", usuario.getId())
                .claim("horaExpiracao", dataHoraExpiracao.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .expiration(data)
                .signWith(getSigningKey())
                .compact();

        return token;
    }

    @Override
    public Claims obterClaims(String token) throws ExpiredJwtException {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public boolean isTokenValido(String token) {
        try {
            Claims claims = obterClaims(token);
            Date exp = claims.getExpiration();
            LocalDateTime dataExpiracao = exp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

            return LocalDateTime.now().isBefore(dataExpiracao);

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String obterLoginUsuario(String token) throws ExpiredJwtException {
        Claims claims = obterClaims(token);
        return claims.getSubject();
    }
}