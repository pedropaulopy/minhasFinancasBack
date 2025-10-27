package com.pedropaulo.minhasFinancas.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedropaulo.minhasFinancas.api.dto.LancamentoDTO;
import com.pedropaulo.minhasFinancas.api.resource.LancamentoResource;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import com.pedropaulo.minhasFinancas.model.enums.TipoLancamento;
import com.pedropaulo.minhasFinancas.service.LancamentoService;
import com.pedropaulo.minhasFinancas.service.UsuarioService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@WebMvcTest(controllers = LancamentoResource.class, excludeAutoConfiguration =  { SecurityAutoConfiguration.class })
@AutoConfigureMockMvc
public class LancamentoResourceTest {

    static final String API = "/api/lancamentos";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LancamentoService lancamentoService;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void deveSalvarUmLancamentoERetornarCreated() throws Exception {

        LancamentoDTO dto = LancamentoDTO.builder()
                .usuario(1L)
                .descricao("Salário")
                .valor(BigDecimal.valueOf(5000))
                .mes(10)
                .ano(2025)
                .tipoLancamento("RECEITA")
                .statusLancamento("PENDENTE")
                .build();

        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();

        Lancamento lancamentoSalvoMock = new Lancamento();
        lancamentoSalvoMock.setId(99L);
        lancamentoSalvoMock.setDescricao("Salário");
        lancamentoSalvoMock.setUsuario(usuarioMock);
        lancamentoSalvoMock.setStatusLancamento(StatusLancamento.PENDENTE);
        lancamentoSalvoMock.setTipoLancamento(TipoLancamento.RECEITA);
        lancamentoSalvoMock.setValor(BigDecimal.valueOf(5000));

        when(usuarioService.obterPorId(1L)).thenReturn(Optional.of(usuarioMock));

        when(lancamentoService.salvar(any(Lancamento.class))).thenReturn(lancamentoSalvoMock);

        String jsonRequest = objectMapper.writeValueAsString(dto);

        mvc.perform(
                        post("/api/lancamentos/salvar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.id").value(99L)
                )
                .andExpect(
                        jsonPath("$.descricao").value("Salário")
                )
                .andExpect(
                        jsonPath("$.statusLancamento").value("PENDENTE")
                )
                .andExpect(
                        jsonPath(("$.tipoLancamento")).value("RECEITA")
                )
                .andExpect(
                        jsonPath("$.valor").value(5000)
                );
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoUsuarioNaoEncontrado() throws Exception {
        LancamentoDTO dto = criarDtoValido();
        String jsonRequest = objectMapper.writeValueAsString(dto);
        String mensagemErro = "Usuário não encontrado com o ID informado.";

        when(usuarioService.obterPorId(1L)).thenReturn(Optional.empty());

        mvc.perform(
                        post(API + "/salvar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value(mensagemErro));
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoDescricaoInvalida() throws Exception {
        String mensagemErro = "Insira uma descrição válida.";
        executarTesteDeValidacaoDoServico(mensagemErro);
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoMesInvalido() throws Exception {
        String mensagemErro = "Insira um mês válido.";
        executarTesteDeValidacaoDoServico(mensagemErro);
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoAnoInvalido() throws Exception {
        String mensagemErro = "Insira um ano válido.";
        executarTesteDeValidacaoDoServico(mensagemErro);
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoValorInvalido() throws Exception {
        String mensagemErro = "Insira um valor válido.";
        executarTesteDeValidacaoDoServico(mensagemErro);
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoTipoTransacaoInvalido() throws Exception {
        String mensagemErro = "Insira um tipo de transação válido.";
        executarTesteDeValidacaoDoServico(mensagemErro);
    }


    private void executarTesteDeValidacaoDoServico(String mensagemErro) throws Exception {
        LancamentoDTO dto = criarDtoValido();
        String jsonRequest = objectMapper.writeValueAsString(dto);
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();

        when(usuarioService.obterPorId(1L)).thenReturn(Optional.of(usuarioMock));

        when(lancamentoService.salvar(any(Lancamento.class)))
                .thenThrow(new RegraNegocioException(mensagemErro));

        mvc.perform(
                        post(API +"/salvar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value(mensagemErro));
    }

    private LancamentoDTO criarDtoValido() {
        return LancamentoDTO.builder()
                .usuario(1L)
                .descricao("Salário")
                .valor(BigDecimal.valueOf(5000))
                .mes(10)
                .ano(2025)
                .tipoLancamento("RECEITA")
                .statusLancamento("PENDENTE")
                .build();
    }


}
