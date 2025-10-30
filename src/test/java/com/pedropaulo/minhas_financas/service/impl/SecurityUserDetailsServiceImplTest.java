package com.pedropaulo.minhas_financas.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.UsuarioRepository;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
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
