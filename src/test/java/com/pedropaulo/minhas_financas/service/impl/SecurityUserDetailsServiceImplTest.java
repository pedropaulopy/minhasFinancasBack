package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class SecurityUserDetailsServiceImplTest {

	private static final String EMAIL = "pedro@exemplo.com";

	private static final String SENHA = "senha-secreta";

	@Mock
	UsuarioRepository usuarioRepository;

	@InjectMocks
	SecurityUserDetailsServiceImpl service;

	@Test
	void deveCarregarUsuarioPorEmail() {
		Usuario usuario = Usuario.builder().id(1L).nome("Pedro").email(EMAIL).senha(SENHA).build();

		given(usuarioRepository.findByEmail(anyString())).willReturn(Optional.of(usuario));

		UserDetails details = service.loadUserByUsername(EMAIL);

		assertThat(details.getUsername()).isEqualTo(EMAIL);
		assertThat(details.getPassword()).isEqualTo(SENHA);
		assertThat(details.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet()))
			.contains("ROLE_USER");
	}

	@Test
	void deveLancarExcecaoQuandoEmailNaoCadastrado() {
		given(usuarioRepository.findByEmail(anyString())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.loadUserByUsername("naoexiste@exemplo.com"))
			.isInstanceOf(UsernameNotFoundException.class)
			.hasMessage("Email não cadastrado.");
	}

}
