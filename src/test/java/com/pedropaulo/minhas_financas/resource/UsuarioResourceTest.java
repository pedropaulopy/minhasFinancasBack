package com.pedropaulo.minhas_financas.resource;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedropaulo.minhas_financas.api.dto.UsuarioDTO;
import com.pedropaulo.minhas_financas.api.resource.UsuarioResource;
import com.pedropaulo.minhas_financas.exception.AutenticacaoException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import com.pedropaulo.minhas_financas.service.impl.JwtServiceImpl;
import java.math.BigDecimal;

import com.pedropaulo.minhas_financas.api.config.SecurityConfig;
import com.pedropaulo.minhas_financas.service.impl.SecurityUserDetailsServiceImpl;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@WebMvcTest(controllers = UsuarioResource.class, excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
public class UsuarioResourceTest {
    static final String API = "/api/usuarios";
    static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    @Autowired
    MockMvc mvc;

    @SuppressWarnings("removal")
    @MockBean
    UsuarioService service;

    @SuppressWarnings("removal")
    @MockBean
    LancamentoService lancamentoService;

    @SuppressWarnings("removal")
    @MockBean
    JwtServiceImpl jwtService;

    @SuppressWarnings("removal")
    @MockBean
    private SecurityUserDetailsServiceImpl userDetailsService;

    @Test
    public void deveAutenticarUmUsuario() throws Exception {
        String email = "email@email.com";
        String senha = "123";
        String tokenFalso = "access-token-123";

        UsuarioDTO dto = UsuarioDTO.builder().email(email).senha(senha).build();
        Usuario usuario = Usuario.builder().nome("Usuário").email(email).senha(senha).id(1L).build();

        Mockito.when(service.autenticar(email, senha)).thenReturn(usuario);
        Mockito.when(jwtService.gerarToken(usuario)).thenReturn(tokenFalso);

        String json = new ObjectMapper().writeValueAsString(dto);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post((API.concat("/autenticar")))
                .contentType(JSON)
                .accept(JSON)
                .content(json);

        mvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("nome").value(usuario.getNome()))
                .andExpect(MockMvcResultMatchers.jsonPath("token").value(tokenFalso));
    }

    @Test
    public void deveRetornarBadRequestAoAutenticarUmUsuarioErrado() throws Exception {
        String email = "email@email.com";
        String senha = "123";

        UsuarioDTO dto = UsuarioDTO.builder().email(email).senha(senha).build();
        Usuario usuario = Usuario.builder().email(email).senha(senha).build();

        Mockito.when(service.autenticar(email, senha)).thenThrow(AutenticacaoException.class);

        String json = new ObjectMapper().writeValueAsString(dto);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post((API.concat("/autenticar")))
                .contentType(JSON)
                .accept(JSON)
                .content(json);

        mvc.perform(request).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void deveCriarUmUsuario() throws Exception {
        String email = "email@email.com";
        String senha = "123";

        UsuarioDTO dto = UsuarioDTO.builder().email(email).senha(senha).build();
        Usuario usuario = Usuario.builder().email(email).senha(senha).build();

        Mockito.when(service.salvarUsuario(Mockito.any())).thenReturn(usuario);

        String json = new ObjectMapper().writeValueAsString(dto);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post((API))
                .contentType(JSON)
                .accept(JSON)
                .content(json);

        mvc.perform(request).andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(usuario.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("nome").value(usuario.getNome()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(usuario.getEmail()));
    }

    @Test
    public void deveLancarBadRequestAoCriarUmUsuarioErrado() throws Exception {
        String email = "email@email.com";
        String senha = "123";

        UsuarioDTO dto = UsuarioDTO.builder().email(email).senha(senha).build();
        Usuario usuario = Usuario.builder().email(email).senha(senha).build();

        Mockito.when(service.salvarUsuario(Mockito.any())).thenThrow(RegraNegocioException.class);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post((API))
                .contentType(JSON)
                .accept(JSON);

        mvc.perform(request).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "email@email.com")
    public void deveRetornarSaldoDeUmUsuario() throws Exception {
        Long idUsuario = 1L;
        String email = "email@email.com";
        BigDecimal saldo = BigDecimal.valueOf(1000);

        Usuario usuario = Usuario.builder().id(idUsuario).nome("nome").email(email).build();

        Mockito.when(service.obterIdUsuarioPorEmail(email)).thenReturn(usuario);

        Mockito.when(service.obterPorId(idUsuario)).thenReturn(java.util.Optional.of(usuario));

        Mockito.when(lancamentoService.obterSaldoPorUsuario(idUsuario)).thenReturn(saldo);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(API.concat("/saldo"))
                .contentType(JSON)
                .accept(JSON);

        mvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("1000"));
    }

    @Test
    @WithMockUser(username = "outro@email.com") // 1. Add mocked user to avoid NPE
    public void naoDeveRetornarSaldoDeUmUsuario() throws Exception {
        Long idUsuario = 1L;
        String email = "outro@email.com";

        Mockito.when(service.obterIdUsuarioPorEmail(email))
                .thenThrow(new RegraNegocioException("Usuário não encontrado"));

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(API.concat("/saldo"))
                .contentType(JSON)
                .accept(JSON);

        mvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }


}
