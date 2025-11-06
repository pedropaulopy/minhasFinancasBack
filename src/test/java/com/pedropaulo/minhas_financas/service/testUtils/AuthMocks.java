package com.pedropaulo.minhas_financas.service.testUtils;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

public final class AuthMocks {

	private AuthMocks() {
	}

	public static Authentication auth(String email) {
		return new TestingAuthenticationToken(email, null);
	}

	public static Authentication auth(String principal, Object credentials) {
		return new TestingAuthenticationToken(principal, credentials);
	}

}
