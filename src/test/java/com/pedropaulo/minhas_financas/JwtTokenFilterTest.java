package com.pedropaulo.minhas_financas;

import com.pedropaulo.minhas_financas.api.JwtTokenFilter;
import com.pedropaulo.minhas_financas.service.JwtService;
import com.pedropaulo.minhas_financas.service.impl.SecurityUserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class JwtTokenFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	private SecurityUserDetailsServiceImpl userDetailsService;

	private JwtTokenFilter filter;

	@BeforeEach
	void setup() {
		userDetailsService = new StubUserDetailsService();
		filter = new JwtTokenFilter(jwtService, userDetailsService);
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void deveAutenticarQuandoTokenValido() throws ServletException, IOException {
		given(request.getHeader("Authorization")).willReturn("Bearer token-valido");
		given(jwtService.isTokenValido("token-valido")).willReturn(true);
		given(jwtService.obterLoginUsuario("token-valido")).willReturn("usuario@teste.com");

		filter.doFilterInternal(request, response, filterChain);

		then(filterChain).should().doFilter(request, response);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("usuario@teste.com");
		assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
	}

	@Test
	void deveIgnorarQuandoNaoHaHeaderAuthorization() throws ServletException, IOException {
		given(request.getHeader("Authorization")).willReturn(null);

		filter.doFilterInternal(request, response, filterChain);

		then(jwtService).shouldHaveNoInteractions();
		then(filterChain).should().doFilter(request, response);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void deveIgnorarQuandoHeaderNaoComecaComBearer() throws ServletException, IOException {
		given(request.getHeader("Authorization")).willReturn("Token 123");

		filter.doFilterInternal(request, response, filterChain);

		then(jwtService).shouldHaveNoInteractions();
		then(filterChain).should().doFilter(request, response);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void deveIgnorarQuandoTokenInvalido() throws ServletException, IOException {
		given(request.getHeader("Authorization")).willReturn("Bearer token-invalido");
		given(jwtService.isTokenValido("token-invalido")).willReturn(false);

		filter.doFilterInternal(request, response, filterChain);

		then(jwtService).should().isTokenValido("token-invalido");
		then(filterChain).should().doFilter(request, response);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	private static class StubUserDetailsService extends SecurityUserDetailsServiceImpl {

		StubUserDetailsService() {
			super(null);
		}

		@Override
		public UserDetails loadUserByUsername(String username) {
			return User.withUsername(username).password("123").roles("USER").build();
		}

	}

}
