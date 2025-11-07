package com.pedropaulo.minhas_financas;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Disabled
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = MinhasFinancasApplicationTest.DummyController.class)
@Import(MinhasFinancasApplicationTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class MinhasFinancasApplicationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void contextoDeveCarregarSemErros() {
	}

	@Test
	void devePermitirCorsParaLocalhost3000() throws Exception {
		mvc.perform(options("/api/test").header("Origin", "http://localhost:3000")
			.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
			.andExpect(header().string("Access-Control-Allow-Methods", is("GET,POST,PUT,DELETE,OPTIONS")));
	}

	@Test
	void deveNegarCorsParaOrigemNaoPermitida() throws Exception {
		mvc.perform(options("/api/test").header("Origin", "http://malicioso.com")
			.header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
	}

	@RestController
	static class DummyController {

		@GetMapping("/api/test")
		public String test() {
			return "ok";
		}

	}

	@TestConfiguration
	@EnableWebSecurity
	static class TestSecurityConfig {

		@Bean
		SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
				throws Exception {
			http.cors(c -> c.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
			return http.build();
		}

		@Bean
		CorsConfigurationSource corsConfigurationSource() {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOrigins(List.of("http://localhost:3000"));
			config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
			config.setAllowedHeaders(List.of("*"));
			config.setAllowCredentials(true);

			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			source.registerCorsConfiguration("/**", config);
			return source;
		}

	}

}
