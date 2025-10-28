package com.pedropaulo.minhasFinancas.api;

import com.pedropaulo.minhasFinancas.service.JwtService;
import com.pedropaulo.minhasFinancas.service.impl.SecurityUserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtTokenFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final SecurityUserDetailsServiceImpl userDetailsService;

  public JwtTokenFilter(JwtService jwtService, SecurityUserDetailsServiceImpl userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");

    if (authorization != null && authorization.startsWith("Bearer ")) {
      String token = authorization.split(" ")[1];
      boolean isValid = jwtService.isTokenValido(token);

      if (isValid) {
        String login = jwtService.obterLoginUsuario(token);
        UserDetails usuarioAutenticado = userDetailsService.loadUserByUsername(login);
        UsernamePasswordAuthenticationToken user =
            new UsernamePasswordAuthenticationToken(
                usuarioAutenticado, null, usuarioAutenticado.getAuthorities());
        user.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(user);
      }
    }
    filterChain.doFilter(request, response);
  }
}
