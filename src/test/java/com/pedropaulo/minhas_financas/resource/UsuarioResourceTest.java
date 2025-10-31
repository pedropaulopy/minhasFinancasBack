package com.pedropaulo.minhas_financas.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedropaulo.minhas_financas.api.dto.UsuarioDTO;
import com.pedropaulo.minhas_financas.api.resource.UsuarioResource;
import com.pedropaulo.minhas_financas.api.config.SecurityConfig;
import com.pedropaulo.minhas_financas.exception.AutenticacaoException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import com.pedropaulo.minhas_financas.service.impl.JwtServiceImpl;
import com.pedropaulo.minhas_financas.service.impl.SecurityUserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@WebMvcTest(controllers = UsuarioResource.class,
		excludeAutoConfiguration = { UserDetailsServiceAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class UsuarioResourceTest {

	private static final String API = "/api/usuarios";

	private static final MediaType JSON = MediaType.APPLICATION_JSON;

	private static final String EMAIL = "email@email.com";

	private static final String SENHA = "123";

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper om;

	@MockBean
	UsuarioService service;

	@MockBean
	LancamentoService lancamentoService;

	@MockBean
	JwtServiceImpl jwtService;

	@MockBean
	SecurityUserDetailsServiceImpl userDetailsService;

	private UsuarioDTO dto(String email, String senha) {
		return UsuarioDTO.builder().email(email).senha(senha).build();
	}

	private Usuario usuario(Long id, String nome, String email, String senha) {
		return Usuario.builder().id(id).nome(nome).email(email).senha(senha).build();
	}

	@Test
	void deveAutenticarUmUsuario() throws Exception {
		String token = "access-token-123";
		UsuarioDTO body = dto(EMAIL, SENHA);
		Usuario usuario = usuario(1L, "Usuário", EMAIL, SENHA);

		when(service.autenticar(EMAIL, SENHA)).thenReturn(usuario);
		when(jwtService.gerarToken(usuario)).thenReturn(token);

		mvc.perform(post(API + "/autenticar").contentType(JSON).accept(JSON).content(om.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(JSON))
			.andExpect(jsonPath("$.nome").value("Usuário"))
			.andExpect(jsonPath("$.token").value(token));
	}

	@Test
	void deveRetornarBadRequestAoAutenticarUsuarioInvalido() throws Exception {
		UsuarioDTO body = dto(EMAIL, SENHA);
		when(service.autenticar(EMAIL, SENHA)).thenThrow(new AutenticacaoException("Credenciais inválidas"));

		mvc.perform(post(API + "/autenticar").contentType(JSON).accept(JSON).content(om.writeValueAsString(body)))
			.andExpect(status().isBadRequest())
			.andExpect(content().string("Credenciais inválidas"));
	}

	@Test
	void deveCriarUmUsuario() throws Exception {
		UsuarioDTO body = dto(EMAIL, SENHA);
		Usuario salvo = usuario(10L, "Usuário", EMAIL, SENHA);

		when(service.salvarUsuario(any())).thenReturn(salvo);

		mvc.perform(post(API).contentType(JSON).accept(JSON).content(om.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(JSON))
			.andExpect(jsonPath("$.id").value(10))
			.andExpect(jsonPath("$.nome").value("Usuário"))
			.andExpect(jsonPath("$.email").value(EMAIL));
	}

	@Test
	void deveLancarBadRequestAoCriarUsuarioInvalido() throws Exception {
		UsuarioDTO body = dto(EMAIL, SENHA);
		when(service.salvarUsuario(any())).thenThrow(new RegraNegocioException("Dados inválidos"));

		mvc.perform(post(API).contentType(JSON).accept(JSON).content(om.writeValueAsString(body)))
			.andExpect(status().isBadRequest())
			.andExpect(content().string("Dados inválidos"));
	}

	@Test
	@WithMockUser(username = EMAIL)
	void deveRetornarSaldoDeUmUsuario() throws Exception {
		Long idUsuario = 1L;
		BigDecimal saldo = BigDecimal.valueOf(1000);
		when(service.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuario(idUsuario, "nome", EMAIL, "pwd"));
		when(lancamentoService.obterSaldoPorUsuario(idUsuario)).thenReturn(saldo);

		mvc.perform(get(API).accept(JSON)).andExpect(status().isOk()).andExpect(content().string("1000"));
	}

	@Test
	@WithMockUser(username = "outro@email.com")
	void naoDeveRetornarSaldoQuandoUsuarioNaoEncontrado() throws Exception {
		when(service.obterIdUsuarioPorEmail("outro@email.com"))
			.thenThrow(new RegraNegocioException("Usuário não encontrado"));

		mvc.perform(get(API).accept(JSON))
			.andExpect(status().isNotFound())
			.andExpect(content().string("Usuário não encontrado"));
	}

}
