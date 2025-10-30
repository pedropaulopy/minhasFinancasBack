package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.UsuarioRepository;
import com.pedropaulo.minhas_financas.service.SecurityUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SecurityUserDetailsServiceImpl implements SecurityUserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public SecurityUserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Usuario usuarioEncontrado = usuarioRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("Email não cadastrado."));

		return User.builder()
			.username(usuarioEncontrado.getEmail())
			.password(usuarioEncontrado.getSenha())
			.roles("USER")
			.build();
	}

}
