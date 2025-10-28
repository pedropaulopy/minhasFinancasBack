package com.pedropaulo.minhasFinancas.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class SecurityUserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private SecurityUserDetailsServiceImpl service;

    @Test
    public void deveCarregarUsuarioPorEmail() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Pedro")
                .email("pedro@exemplo.com")
                .senha("senha-secreta")
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuario));

        UserDetails details = service.loadUserByUsername("pedro@exemplo.com");

        assertThat(details.getUsername(), is("pedro@exemplo.com"));
        assertThat(details.getPassword(), is("senha-secreta"));
        assertThat(details.getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet()),
                hasItem("ROLE_USER"));
    }

    @Test
    public void deveLancarExcecaoQuandoEmailNaoCadastrado() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        try {
            service.loadUserByUsername("naoexiste@exemplo.com");
        } catch (UsernameNotFoundException e) {
            assertThat(e.getMessage(), is("Email não cadastrado."));
            return;
        }
        throw new AssertionError("Era esperado UsernameNotFoundException");
    }
}
