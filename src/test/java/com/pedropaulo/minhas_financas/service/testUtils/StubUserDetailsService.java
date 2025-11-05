package com.pedropaulo.minhas_financas.service.testUtils;

import com.pedropaulo.minhas_financas.service.impl.SecurityUserDetailsServiceImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class StubUserDetailsService extends SecurityUserDetailsServiceImpl {

    public StubUserDetailsService() {
        super(null);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return User.withUsername(username).password("123").roles("USER").build();
    }

}