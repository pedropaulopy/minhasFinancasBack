package com.pedropaulo.minhasFinancas;

import com.pedropaulo.minhasFinancas.api.JwtTokenFilter;
import com.pedropaulo.minhasFinancas.service.JwtService;
import com.pedropaulo.minhasFinancas.service.impl.SecurityUserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JwtTokenFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityUserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtTokenFilter filter;

    @Before
    public void setup() {
        filter = new JwtTokenFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void deveAutenticarQuandoTokenValido() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.isTokenValido("token-valido")).thenReturn(true);
        when(jwtService.obterLoginUsuario("token-valido")).thenReturn("usuario@teste.com");

        UserDetails userDetails = User.withUsername("usuario@teste.com")
                .password("123")
                .roles("USER")
                .build();

        when(userDetailsService.loadUserByUsername("usuario@teste.com")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService).loadUserByUsername("usuario@teste.com");
        verify(filterChain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() != null;
        assert SecurityContextHolder.getContext().getAuthentication().getName().equals("usuario@teste.com");
    }

    @Test
    public void deveIgnorarQuandoNaoHaHeaderAuthorization() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).isTokenValido(any());
        verify(filterChain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    public void deveIgnorarQuandoHeaderNaoComecaComBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("TokenInvalido 123");

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).isTokenValido(any());
        verify(filterChain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    public void deveIgnorarQuandoTokenInvalido() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(jwtService.isTokenValido("token-invalido")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtService).isTokenValido(eq("token-invalido"));
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }
}
