package com.pedropaulo.minhas_financas.service.testUtils;

import com.pedropaulo.minhas_financas.service.SecurityUserDetailsService;
import com.pedropaulo.minhas_financas.service.impl.SecurityUserDetailsServiceImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public class StubUserDetailsService implements SecurityUserDetailsService {

	@Override
	public UserDetails loadUserByUsername(String username) {
		return User.withUsername(username).password("123").roles("USER").build();
	}

}