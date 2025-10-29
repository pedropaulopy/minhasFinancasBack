package com.pedropaulo.minhasFinancas.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedropaulo.minhasFinancas.api.dto.LancamentoDTO;
import com.pedropaulo.minhasFinancas.api.dto.LancamentoStatusDTO;
import com.pedropaulo.minhasFinancas.api.resource.LancamentoResource;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import com.pedropaulo.minhasFinancas.model.enums.TipoLancamento;
import com.pedropaulo.minhasFinancas.service.LancamentoService;
import com.pedropaulo.minhasFinancas.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LancamentoResourceTest {

    @Mock
    private LancamentoService lancamentoService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private Authentication authentication;

    private LancamentoResource resource;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        resource = new LancamentoResource(lancamentoService, usuarioService);
    }

    @Test
    public void salvar_deveRetornarCreated_quandoSucesso() throws Exception {
        LancamentoDTO dto = criarDtoValido();
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();

        Lancamento entidade = Lancamento.builder()
                .ano(2025)
                .mes(10)
                .descricao("Salário")
                .valor(BigDecimal.valueOf(5000))
                .tipoLancamento(TipoLancamento.RECEITA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now()).build();

        Lancamento salvo = Lancamento.builder()
                .id(99L)
                .ano(2025)
                .mes(10)
                .descricao("Salário")
                .valor(BigDecimal.valueOf(5000))
                .tipoLancamento(TipoLancamento.RECEITA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now()).build();

        Mockito.when(usuarioService.obterPorId(1L)).thenReturn(Optional.of(usuarioMock));
        Mockito.when(lancamentoService.converterDTO(dto, authentication)).thenReturn(entidade);
        Mockito.when(lancamentoService.salvar(entidade)).thenReturn(salvo);

        ResponseEntity response = resource.salvar(dto, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(response.getBody()).isEqualTo(salvo);
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoUsuarioNaoEncontrado() throws Exception {
        LancamentoDTO dto = criarDtoValido();
        Mockito.when(usuarioService.obterPorId(1L)).thenReturn(Optional.empty());

        Mockito.lenient().when(lancamentoService.converterDTO(Mockito.any(LancamentoDTO.class), Mockito.any(Authentication.class))).thenReturn(new Lancamento());
        Mockito.doThrow(new RegraNegocioException("Usuário não encontrado com o ID informado."))
                .when(lancamentoService).salvar(Mockito.any(Lancamento.class));

        ResponseEntity response = resource.salvar(dto, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("Usuário não encontrado com o ID informado.");
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoDescricaoInvalida() throws Exception {
        executarTesteDeValidacaoDoServico("Insira uma descrição válida.");
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoMesInvalido() throws Exception {
        executarTesteDeValidacaoDoServico("Insira um mês válido.");
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoAnoInvalido() throws Exception {
        executarTesteDeValidacaoDoServico("Insira um ano válido.");
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoValorInvalido() throws Exception {
        executarTesteDeValidacaoDoServico("Insira um valor válido.");
    }

    @Test
    public void naoDeveSalvarLancamentoQuandoTipoTransacaoInvalido() throws Exception {
        executarTesteDeValidacaoDoServico("Insira um tipo de transação válido.");
    }

    private void executarTesteDeValidacaoDoServico(String mensagemErro) throws Exception {
        LancamentoDTO dto = criarDtoValido();
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();

        Mockito.when(usuarioService.obterPorId(1L)).thenReturn(Optional.of(usuarioMock));
        Mockito.when(lancamentoService.converterDTO(Mockito.any(LancamentoDTO.class), Mockito.any(Authentication.class))).thenReturn(new Lancamento());
        Mockito.when(lancamentoService.salvar(Mockito.any(Lancamento.class)))
                .thenThrow(new RegraNegocioException(mensagemErro));

        ResponseEntity response = resource.salvar(dto, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo(mensagemErro);
    }

    private LancamentoDTO criarDtoValido() {
        LancamentoDTO dto = new LancamentoDTO();
        dto.setUsuario(1L);
        dto.setDescricao("Salário");
        dto.setValor(BigDecimal.valueOf(5000));
        dto.setMes(10);
        dto.setAno(2025);
        dto.setTipoLancamento("RECEITA");
        dto.setStatusLancamento("PENDENTE");
        return dto;
    }

    @Test
    public void deveAtualizarUmLancamentoERetornarOk() throws Exception {
        Long id = 1L;
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();

        Lancamento atualizado = Lancamento.builder()
                .id(id)
                .descricao("Atualizado")
                .mes(10)
                .ano(2025)
                .valor(BigDecimal.valueOf(5500))
                .tipoLancamento(TipoLancamento.RECEITA)
                .statusLancamento(StatusLancamento.EFETIVADO)
                .dataCadastro(LocalDate.now()).build();

        LancamentoDTO dto = criarDtoValido();
        dto.setDescricao("Atualizado");
        dto.setValor(BigDecimal.valueOf(5500));
        dto.setStatusLancamento("EFETIVADO");

        Mockito.when(lancamentoService.atualizar(id, authentication, dto)).thenReturn(atualizado);
        Mockito.when(usuarioService.obterPorId(1L)).thenReturn(Optional.of(usuarioMock));

        ResponseEntity response = resource.atualizar(id, dto, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isEqualTo(atualizado);
    }

    @Test
    public void naoDeveAtualizarQuandoLancamentoNaoEncontrado() throws Exception {
        Long id = 123L;
        LancamentoDTO dto = criarDtoValido();

        Mockito.when(lancamentoService.atualizar(id, authentication, dto)).thenThrow(new RegraNegocioException("Lançamento não encontrado."));

        ResponseEntity response = resource.atualizar(id, dto, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("Lançamento não encontrado.");
    }

    @Test
    public void naoDeveAtualizarQuandoRegraDeNegocioInvalida() throws Exception {
        Long id = 7L;
        LancamentoDTO dto = criarDtoValido();

        Mockito.when(lancamentoService.atualizar(id, authentication, dto)).thenThrow(new RegraNegocioException("Dados inválidos para atualização."));

        ResponseEntity response = resource.atualizar(id, dto, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("Dados inválidos para atualização.");
    }

    @Test
    public void deveAtualizarStatusComSucesso() throws Exception {
        Long id = 5L;
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();
        Lancamento existente = Lancamento.builder()
                .id(id)
                .usuario(usuarioMock)
                .descricao("Conta")
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .valor(BigDecimal.valueOf(200))
                .dataCadastro(LocalDate.now()).build();

        Mockito.when(lancamentoService.obterPorIdLancamento(id, authentication)).thenReturn(existente);
        Mockito.doNothing().when(lancamentoService).atualizarStatus(id, authentication, StatusLancamento.EFETIVADO);

        LancamentoStatusDTO dto = new LancamentoStatusDTO();
        dto.setStatus("EFETIVADO");

        ResponseEntity response = resource.atualizarStatus(id, dto, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    public void naoDeveAtualizarStatusQuandoLancamentoNaoEncontrado() throws Exception {
        Long id = 999L;
        Mockito.doThrow(new RegraNegocioException("Lançamento não encontrado."))
                .when(lancamentoService).atualizarStatus(Mockito.eq(id), Mockito.any(Authentication.class), Mockito.any(StatusLancamento.class));

        LancamentoStatusDTO dto = new LancamentoStatusDTO();
        dto.setStatus("EFETIVADO");

        ResponseEntity response = resource.atualizarStatus(id, dto, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isEqualTo("Lançamento não encontrado.");
    }

    @Test
    public void deveDeletarComSucesso() throws Exception {
        Long id = 3L;
        Usuario usuarioMock = Usuario.builder().id(1L).nome("Pedro").build();
        Lancamento existente = new Lancamento();
        existente.setId(id);
        existente.setUsuario(usuarioMock);

        Mockito.when(lancamentoService.obterPorIdLancamento(id, authentication)).thenReturn(existente);
        Mockito.doNothing().when(lancamentoService).deletar(id, authentication);

        ResponseEntity response = resource.deletar(id, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    public void naoDeveDeletarQuandoLancamentoNaoEncontrado() throws Exception {
        Long id = 44L;
        Mockito.doThrow(new RegraNegocioException("Lançamento não encontrado."))
                .when(lancamentoService).deletar(id, authentication);

        ResponseEntity response = resource.deletar(id, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(response.getBody()).isEqualTo("Lançamento não encontrado.");
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

        Mockito.when(usuarioService.obterPorId(1L)).thenReturn(Optional.of(usuarioMock));
        Mockito.when(lancamentoService.buscar(Mockito.any(Lancamento.class))).thenReturn(Arrays.asList(l));

        List<Lancamento> resultado = resource.buscar("Aluguel", 10, 2025, BigDecimal.valueOf(1200),
                TipoLancamento.DESPESA, StatusLancamento.PENDENTE, 1L);

        Assertions.assertThat(resultado).isNotEmpty().hasSize(1).contains(l);
    }

    @Test
    public void naoDeveBuscarQuandoUsuarioNaoEncontrado() throws Exception {
        Mockito.when(usuarioService.obterPorId(1L)).thenReturn(Optional.empty());

        List<Lancamento> resultado = resource.buscar(null, null, null, null, null, null, 1L);
        Assertions.assertThat(resultado).isEmpty();
    }

    @Test
    public void deveObterLancamentoPorId() throws Exception {
        Lancamento l = new Lancamento();
        l.setId(77L);
        l.setDescricao("Internet");
        l.setMes(9);
        l.setAno(2025);
        l.setValor(BigDecimal.valueOf(99.9));
        l.setTipoLancamento(TipoLancamento.DESPESA);
        l.setStatusLancamento(StatusLancamento.EFETIVADO);

        Mockito.when(lancamentoService.obterPorIdLancamento(77L, authentication)).thenReturn(l);

        ResponseEntity<?> response = resource.obterLancamento(77L, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isEqualTo(l);
    }

    @Test
    public void deveRetornarNotFoundQuandoLancamentoNaoExiste() throws Exception {
        Mockito.when(lancamentoService.obterPorIdLancamento(321L, authentication)).thenThrow(new RegraNegocioException("Lançamento não encontrado."));

        ResponseEntity<?> response = resource.obterLancamento(321L, authentication);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(response.getBody()).isEqualTo("Lançamento não encontrado.");
    }
}