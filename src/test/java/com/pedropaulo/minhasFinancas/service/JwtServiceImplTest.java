package com.pedropaulo.minhasFinancas.service.impl;

import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class JwtServiceImplTest {

    private JwtServiceImpl buildService(String expiracaoMinutos, String base64Key) {
        JwtServiceImpl svc = new JwtServiceImpl();
        ReflectionTestUtils.setField(svc, "expiracao", expiracaoMinutos);
        ReflectionTestUtils.setField(svc, "chaveAssinatura", base64Key);
        return svc;
    }

    private String strongBase64Key() {
        byte[] key = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(key);
    }

    private Usuario usuario() {
        return Usuario.builder().id(42L).nome("Pedro").email("pedro@exemplo.com").build();
    }

    @Test
    public void deveGerarTokenComClaims() {
        JwtServiceImpl svc = buildService("5", strongBase64Key());
        String token = svc.gerarToken(usuario());
        Claims claims = svc.obterClaims(token);

        assertEquals("pedro@exemplo.com", claims.getSubject());
        assertEquals("Pedro", claims.get("nome"));
        assertEquals(42, ((Number) claims.get("idUsuario")).intValue());
        assertThat((String) claims.get("horaExpiracao"), matchesPattern("\\d{2}:\\d{2}"));
        assertThat(claims.getExpiration(), notNullValue());
    }

    @Test
    public void deveValidarTokenValido() {
        JwtServiceImpl svc = buildService("10", strongBase64Key());
        String token = svc.gerarToken(usuario());
        assertTrue(svc.isTokenValido(token));
    }

    @Test
    public void deveRetornarFalseParaTokenExpirado() {
        JwtServiceImpl svc = buildService("-1", strongBase64Key());
        String token = svc.gerarToken(usuario());
        assertFalse(svc.isTokenValido(token));
    }

    @Test
    public void deveObterLoginUsuario() {
        JwtServiceImpl svc = buildService("3", strongBase64Key());
        String token = svc.gerarToken(usuario());
        assertEquals("pedro@exemplo.com", svc.obterLoginUsuario(token));
    }

    @Test
    public void deveRetornarFalseParaTokenComAssinaturaInvalida() {
        JwtServiceImpl legit = buildService("5", strongBase64Key());
        String tokenAssinado = legit.gerarToken(usuario());

        byte[] otherKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".getBytes(StandardCharsets.UTF_8);
        String otherBase64 = Base64.getEncoder().encodeToString(otherKey);
        JwtServiceImpl verificadorComOutraChave = buildService("5", otherBase64);

        assertFalse(verificadorComOutraChave.isTokenValido(tokenAssinado));
    }

    @Test(expected = ExpiredJwtException.class)
    public void deveLancarExpiredAoObterClaimsDeTokenExpirado() {
        JwtServiceImpl svc = buildService("-2", strongBase64Key());
        String token = svc.gerarToken(usuario());
        svc.obterClaims(token);
    }

    @Test
    public void horaExpiracaoNoFormatoHHmm() {
        JwtServiceImpl svc = buildService("1", strongBase64Key());
        String token = svc.gerarToken(usuario());
        Claims claims = svc.obterClaims(token);

        String esperado = LocalDateTime.now().plusMinutes(1).format(DateTimeFormatter.ofPattern("HH:mm"));
        assertThat((String) claims.get("horaExpiracao"), hasLength(5));
        assertThat(((String) claims.get("horaExpiracao")).charAt(2), is(':'));
    }
}
