package com.pedropaulo.minhasFinancas.resource;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedropaulo.minhasFinancas.api.dto.UsuarioDTO;
import com.pedropaulo.minhasFinancas.api.resource.UsuarioResource;
import com.pedropaulo.minhasFinancas.exception.AutenticacaoException;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.service.LancamentoService;
import com.pedropaulo.minhasFinancas.service.UsuarioService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@WebMvcTest(controllers = UsuarioResource.class)
@AutoConfigureMockMvc
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

    @Test
    public void deveAutenticarUmUsuario() throws Exception {
        String email = "email@email.com";
        String senha = "123";

        UsuarioDTO dto = UsuarioDTO.builder().email(email).senha(senha).build();
        Usuario usuario = Usuario.builder().email(email).senha(senha).build();

        Mockito.when(service.autenticar(email, senha)).thenReturn(usuario);

        String json = new ObjectMapper().writeValueAsString(dto);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post((API.concat("/autenticar")))
                .contentType(JSON)
                .accept(JSON)
                .content(json);

        mvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(usuario.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("nome").value(usuario.getNome()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(usuario.getEmail()));
    }

    @Test
    public void deveRetornarBadRequestAoAutenticarUmUsuarioErrado() throws Exception {
        String email = "email@email.com";
        String senha = "123";

        UsuarioDTO dto = UsuarioDTO.builder().email(email).senha(senha).build();
        Usuario usuario = Usuario.builder().email(email).senha(senha).build();

        Mockito.when(service.autenticar(email, senha)).thenThrow(AutenticacaoException.class);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post((API.concat("/autenticar")))
                .contentType(JSON)
                .accept(JSON);

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
    public void deveRetornarSaldoDeUmUsuario() throws Exception {
        Long idUsuario = 1L;
        BigDecimal saldo = BigDecimal.valueOf(1000);

        Usuario usuario = Usuario.builder().id(idUsuario).nome("nome").email("email@email.com").build();
        Mockito.when(service.obterPorId(idUsuario)).thenReturn(java.util.Optional.of(usuario));

        Mockito.when(lancamentoService.obterSaldoPorUsuario(idUsuario)).thenReturn(saldo);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get((API.concat("/saldo/").concat(idUsuario.toString())))
                .contentType(JSON)
                .accept(JSON);

        mvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("1000"));
    }

    @Test
    public void naoDeveRetornarSaldoDeUmUsuario() throws Exception {
        Long idUsuario = 1L;

        Mockito.when(service.obterPorId(idUsuario)).thenReturn(Optional.empty());

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get((API.concat("/saldo/").concat(idUsuario.toString())))
                .contentType(JSON)
                .accept(JSON);

        mvc.perform(request).andExpect(MockMvcResultMatchers.status().isNotFound());
    }


}
