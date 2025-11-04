package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class JwtServiceImplTest {

	private static final String EMAIL = "pedro@exemplo.com";

	private static final String NOME = "Pedro";

	private static final long ID = 42L;

	private static final String EXP_5_MIN = "5";

	private static final String EXP_10_MIN = "10";

	private static final String EXP_PAST_1_MIN = "-1";

	private static final String EXP_PAST_2_MIN = "-2";

	private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

	@Test
	void deveGerarTokenComClaims() {
		JwtServiceImpl svc = buildService(EXP_5_MIN, strongBase64Key());
		String token = svc.gerarToken(usuario());
		Claims claims = svc.obterClaims(token);

		assertThat(claims.getSubject()).isEqualTo(EMAIL);
		assertThat(claims.get("nome")).isEqualTo(NOME);
		assertThat(((Number) claims.get("idUsuario")).longValue()).isEqualTo(ID);
		assertThat((String) claims.get("horaExpiracao")).matches("\\d{2}:\\d{2}");
		assertThat(claims.getExpiration()).isNotNull();
	}

	@Test
	void deveValidarTokenValido() {
		JwtServiceImpl svc = buildService(EXP_10_MIN, strongBase64Key());
		String token = svc.gerarToken(usuario());
		assertThat(svc.isTokenValido(token)).isTrue();
	}

	@Test
	void deveRetornarFalseParaTokenExpirado() {
		JwtServiceImpl svc = buildService(EXP_PAST_1_MIN, strongBase64Key());
		String token = svc.gerarToken(usuario());
		assertThat(svc.isTokenValido(token)).isFalse();
	}

	@Test
	void deveObterLoginUsuario() {
		JwtServiceImpl svc = buildService(EXP_5_MIN, strongBase64Key());
		String token = svc.gerarToken(usuario());
		assertThat(svc.obterLoginUsuario(token)).isEqualTo(EMAIL);
	}

	@Test
	void deveRetornarFalseParaTokenComAssinaturaInvalida() {
		JwtServiceImpl legit = buildService(EXP_5_MIN, strongBase64Key());
		String tokenAssinado = legit.gerarToken(usuario());

		byte[] otherKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".getBytes(StandardCharsets.UTF_8);
		String otherBase64 = Base64.getEncoder().encodeToString(otherKey);
		JwtServiceImpl verificadorOutraChave = buildService(EXP_5_MIN, otherBase64);

		assertThat(verificadorOutraChave.isTokenValido(tokenAssinado)).isFalse();
	}

	@Test
	void deveLancarExpiredAoObterClaimsDeTokenExpirado() {
		JwtServiceImpl svc = buildService(EXP_PAST_2_MIN, strongBase64Key());
		String token = svc.gerarToken(usuario());
		assertThatThrownBy(() -> svc.obterClaims(token)).isInstanceOf(ExpiredJwtException.class);
	}

	@Test
	void horaExpiracaoNoFormatoHHmm() {
		JwtServiceImpl svc = buildService("1", strongBase64Key());
		String token = svc.gerarToken(usuario());
		Claims claims = svc.obterClaims(token);

		String hora = (String) claims.get("horaExpiracao");
		assertThat(hora).hasSize(5);
		assertThat(hora.charAt(2)).isEqualTo(':');
		assertThat(hora).matches("\\d{2}:\\d{2}");
		assertThatCode(() -> LocalDateTime.now()
			.withHour(Integer.parseInt(hora.substring(0, 2)))
			.withMinute(Integer.parseInt(hora.substring(3, 5)))).doesNotThrowAnyException();
	}

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
		return Usuario.builder().id(ID).nome(NOME).email(EMAIL).build();
	}

}
