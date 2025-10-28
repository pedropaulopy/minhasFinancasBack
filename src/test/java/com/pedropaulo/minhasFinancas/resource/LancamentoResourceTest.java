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

    @Test
    public void deveAtualizarUmLancamentoERetornarOk() throws Exception {
        Long id = 1L;
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();
        Lancamento existente = new Lancamento();
        existente.setId(id);
        existente.setDescricao("Antigo");
        existente.setUsuario(usuarioMock);
        existente.setTipoLancamento(TipoLancamento.RECEITA);
        existente.setStatusLancamento(StatusLancamento.PENDENTE);
        existente.setValor(BigDecimal.valueOf(100));

        when(lancamentoService.obterPorId(id)).thenReturn(Optional.of(existente));
        when(usuarioService.obterPorId(1L)).thenReturn(Optional.of(usuarioMock));

        LancamentoDTO dto = LancamentoDTO.builder()
                .usuario(1L)
                .descricao("Atualizado")
                .valor(BigDecimal.valueOf(5500))
                .mes(10)
                .ano(2025)
                .tipoLancamento("RECEITA")
                .statusLancamento("EFETIVADO")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        mvc.perform(put(API + "/{id}/atualizar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.descricao").value("Atualizado"))
                .andExpect(jsonPath("$.tipoLancamento").value("RECEITA"))
                .andExpect(jsonPath("$.statusLancamento").value("EFETIVADO"))
                .andExpect(jsonPath("$.valor").value(5500));
    }

    @Test
    public void naoDeveAtualizarQuandoLancamentoNaoEncontrado() throws Exception {
        Long id = 123L;
        when(lancamentoService.obterPorId(id)).thenReturn(Optional.empty());
        LancamentoDTO dto = criarDtoValido();
        String json = objectMapper.writeValueAsString(dto);

        mvc.perform(put(API + "/{id}/atualizar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Lançamento não encontrado."));
    }

    @Test
    public void naoDeveAtualizarQuandoRegraDeNegocioInvalida() throws Exception {
        Long id = 7L;
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();
        Lancamento existente = new Lancamento();
        existente.setId(id);
        existente.setUsuario(usuarioMock);

        when(lancamentoService.obterPorId(id)).thenReturn(Optional.of(existente));
        when(usuarioService.obterPorId(1L)).thenReturn(Optional.of(usuarioMock));
        when(lancamentoService.atualizar(any(Lancamento.class)))
                .thenThrow(new RegraNegocioException("Dados inválidos para atualização."));

        LancamentoDTO dto = criarDtoValido();
        String json = objectMapper.writeValueAsString(dto);

        mvc.perform(put(API + "/{id}/atualizar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Dados inválidos para atualização."));
    }

    @Test
    public void deveAtualizarStatusComSucesso() throws Exception {
        Long id = 5L;
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();
        Lancamento existente = new Lancamento();
        existente.setId(id);
        existente.setUsuario(usuarioMock);
        existente.setDescricao("Conta");
        existente.setTipoLancamento(TipoLancamento.DESPESA);
        existente.setStatusLancamento(StatusLancamento.PENDENTE);
        existente.setValor(BigDecimal.valueOf(200));

        when(lancamentoService.obterPorId(id)).thenReturn(Optional.of(existente));

        String body = objectMapper.writeValueAsString(
                new com.pedropaulo.minhasFinancas.api.dto.LancamentoStatusDTO("EFETIVADO")
        );

        mvc.perform(put(API + "/{id}/atualizar_status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.statusLancamento").value("EFETIVADO"))
                .andExpect(jsonPath("$.descricao").value("Conta"))
                .andExpect(jsonPath("$.tipoLancamento").value("DESPESA"))
                .andExpect(jsonPath("$.valor").value(200));
    }

    @Test
    public void naoDeveAtualizarStatusQuandoLancamentoNaoEncontrado() throws Exception {
        Long id = 999L;
        when(lancamentoService.obterPorId(id)).thenReturn(Optional.empty());

        String body = objectMapper.writeValueAsString(
                new com.pedropaulo.minhasFinancas.api.dto.LancamentoStatusDTO("EFETIVADO")
        );

        mvc.perform(put(API + "/{id}/atualizar_status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Lançamento não encontrado."));
    }

    @Test
    public void deveDeletarComSucesso() throws Exception {
        Long id = 3L;
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();
        Lancamento existente = new Lancamento();
        existente.setId(id);
        existente.setUsuario(usuarioMock);

        when(lancamentoService.obterPorId(id)).thenReturn(Optional.of(existente));

        mvc.perform(delete(API + "/{id}/deletar", id))
                .andExpect(status().isNoContent());
    }

    @Test
    public void naoDeveDeletarQuandoLancamentoNaoEncontrado() throws Exception {
        Long id = 44L;
        when(lancamentoService.obterPorId(id)).thenReturn(Optional.empty());

        mvc.perform(delete(API + "/{id}/deletar", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Lançamento não encontrado."));
    }

    @Test
    public void deveBuscarComFiltrosERetornarLista() throws Exception {
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();
        Lancamento l = new Lancamento();
        l.setId(10L);
        l.setUsuario(usuarioMock);
        l.setDescricao("Aluguel");
        l.setMes(10);
        l.setAno(2025);
        l.setValor(BigDecimal.valueOf(1200));
        l.setTipoLancamento(TipoLancamento.DESPESA);
        l.setStatusLancamento(StatusLancamento.PENDENTE);

        when(usuarioService.obterPorId(1L)).thenReturn(Optional.of(usuarioMock));
        when(lancamentoService.buscar(any(Lancamento.class))).thenReturn(java.util.Arrays.asList(l));

        mvc.perform(get(API + "/buscar")
                        .param("descricao", "Aluguel")
                        .param("mes", "10")
                        .param("ano", "2025")
                        .param("valor", "1200")
                        .param("tipo_lancamento", "DESPESA")
                        .param("status_lancamento", "PENDENTE")
                        .param("usuario", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].descricao").value("Aluguel"))
                .andExpect(jsonPath("$[0].tipoLancamento").value("DESPESA"))
                .andExpect(jsonPath("$[0].statusLancamento").value("PENDENTE"))
                .andExpect(jsonPath("$[0].valor").value(1200));
    }

    @Test
    public void naoDeveBuscarQuandoUsuarioNaoEncontrado() throws Exception {
        when(usuarioService.obterPorId(1L)).thenReturn(Optional.empty());

        mvc.perform(get(API + "/buscar").param("usuario", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Usuário não encontrado para o ID informado."));
    }

    @Test
    public void deveObterLancamentoPorId() throws Exception {
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();
        Lancamento l = new Lancamento();
        l.setId(77L);
        l.setUsuario(usuarioMock);
        l.setDescricao("Internet");
        l.setMes(9);
        l.setAno(2025);
        l.setValor(BigDecimal.valueOf(99.9));
        l.setTipoLancamento(TipoLancamento.DESPESA);
        l.setStatusLancamento(StatusLancamento.EFETIVADO);

        when(lancamentoService.obterPorId(77L)).thenReturn(Optional.of(l));

        mvc.perform(get(API + "/{id}/buscar", 77L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(77L))
                .andExpect(jsonPath("$.descricao").value("Internet"))
                .andExpect(jsonPath("$.tipoLancamento").value("DESPESA"))
                .andExpect(jsonPath("$.statusLancamento").value("EFETIVADO"))
                .andExpect(jsonPath("$.valor").value(99.9));
    }

    @Test
    public void deveRetornarNotFoundQuandoLancamentoNaoExiste() throws Exception {
        when(lancamentoService.obterPorId(321L)).thenReturn(Optional.empty());

        mvc.perform(get(API + "/{id}/buscar", 321L))
                .andExpect(status().isNotFound());
    }
}
