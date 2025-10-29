package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.api.dto.LancamentoDTO;
import com.pedropaulo.minhasFinancas.api.dto.LancamentoDTOFactory;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import com.pedropaulo.minhasFinancas.model.enums.TipoLancamento;
import com.pedropaulo.minhasFinancas.model.repository.LancamentoRepository;
import com.pedropaulo.minhasFinancas.service.impl.LancamentoServiceImpl;
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
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class LancamentoServiceTest {

    @Mock
    LancamentoRepository repository;

    @Mock
    UsuarioService usuarioService;

    LancamentoServiceImpl service;

    @Mock
    Authentication authentication;

    @BeforeEach
    public void setUp() {
        // cria spy manual injetando os mocks
        service = Mockito.spy(new LancamentoServiceImpl(repository, usuarioService));
    }

    @Test
    public void deveSalvarUmLancamento() throws RegraNegocioException {
        Lancamento lancamentoASalvar = Lancamento.builder()
                .ano(2025)
                .mes(11)
                .descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now()).build();

        Mockito.doNothing().when(service).validar(lancamentoASalvar);
        Lancamento lancamentoSalvo = Lancamento.builder()
                .ano(2025)
                .mes(11)
                .descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now()).build();
        lancamentoSalvo.setId(1L);
        lancamentoSalvo.setStatusLancamento(StatusLancamento.PENDENTE);

        Mockito.when(repository.save(lancamentoASalvar)).thenReturn(lancamentoSalvo);

        Lancamento lancamento = service.salvar(lancamentoASalvar);

        Assertions.assertThat(lancamento.getId()).isEqualTo(lancamentoSalvo.getId());
        Assertions.assertThat(lancamento.getStatusLancamento()).isEqualTo(StatusLancamento.PENDENTE);
    }

    @Test
    public void deveLancarErroAoTentarSalvarUmLancamentoInvalido() throws RegraNegocioException {
        Lancamento lancamentoASalvar = Lancamento.builder()
                .ano(2025)
                .mes(11)
                .descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now()).build();

        Mockito.doThrow(RegraNegocioException.class).when(service).validar(lancamentoASalvar);
        Assertions.catchThrowableOfType(() -> service.salvar(lancamentoASalvar), RegraNegocioException.class);
        Mockito.verify(repository, Mockito.never()).save(lancamentoASalvar);
    }

    @Test
    public void deveAtualizarUmLancamento() throws RegraNegocioException {
        Lancamento lancamentoSalvo = Lancamento.builder()
                .id(1L)
                .ano(2025)
                .mes(11)
                .descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now()).build();

        LancamentoDTO dto = LancamentoDTOFactory.create(1L, "Lançamento teste", 11, 2025, BigDecimal.valueOf(100),
                TipoLancamento.DESPESA.name(), StatusLancamento.PENDENTE.name());

        Mockito.doReturn(lancamentoSalvo).when(service).obterPorIdLancamento(1L, authentication);
        Mockito.when(repository.save(lancamentoSalvo)).thenReturn(lancamentoSalvo);

        Lancamento atualizado = service.atualizar(1L, authentication, dto);

        Mockito.verify(repository, Mockito.times(1)).save(lancamentoSalvo);
        Assertions.assertThat(atualizado).isEqualTo(lancamentoSalvo);
    }

    @Test
    public void deveLancarErroAoTentarAtualizarUmLancamentoQueAindaNaoFoiSalvo() throws RegraNegocioException {
        LancamentoDTO dto = LancamentoDTOFactory.create(1L, "Lançamento teste", 11, 2025, BigDecimal.valueOf(100),
                TipoLancamento.DESPESA.name(), StatusLancamento.PENDENTE.name());

        Mockito.doThrow(new RegraNegocioException("Lançamento não encontrado para o ID informado.")).when(service).obterPorIdLancamento(1L, authentication);

        Assertions.catchThrowableOfType(() -> service.atualizar(1L, authentication, dto), RegraNegocioException.class);
        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void deveDeletarUmLancamento() throws RegraNegocioException {
        Lancamento lancamento = Lancamento.builder()
                .id(1L)
                .ano(2025)
                .mes(11)
                .descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now()).build();

        Mockito.doReturn(lancamento).when(service).obterPorIdLancamento(1L, authentication);
        service.deletar(1L, authentication);
        Mockito.verify(repository).delete(lancamento);
    }

    @Test
    public void deveLancarErroAoTentarDeletarUmLancamentoQueAindaNaoFoiSalvo() throws RegraNegocioException {
        Mockito.doThrow(new RegraNegocioException("Lançamento não encontrado para o ID informado.")).when(service).obterPorIdLancamento(1L, authentication);

        Assertions.catchThrowableOfType(() -> service.deletar(1L, authentication), RegraNegocioException.class);
        Mockito.verify(repository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    public void deveFiltrarLancamentos() {
        Lancamento lancamento = Lancamento.builder()
                .id(1L)
                .ano(2025)
                .mes(11)
                .descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now()).build();

        List<Lancamento> lista = Arrays.asList(lancamento);
        Mockito.when(repository.findAll(Mockito.any(org.springframework.data.domain.Example.class))).thenReturn(lista);
        List<Lancamento> resultado = service.buscar(lancamento);
        Assertions.assertThat(resultado).isNotEmpty().hasSize(1).contains(lancamento);
    }

    @Test
    public void deveAtualizarOStatusDeUmLancamento() throws RegraNegocioException {
        Lancamento lancamento = Lancamento.builder()
                .id(1L)
                .ano(2025)
                .mes(11)
                .descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now()).build();

        StatusLancamento novoStatus = StatusLancamento.EFETIVADO;
        Mockito.doReturn(lancamento).when(service).obterPorIdLancamento(1L, authentication);
        service.atualizarStatus(1L, authentication, novoStatus);
        Assertions.assertThat(lancamento.getStatusLancamento()).isEqualTo(novoStatus);
        Mockito.verify(service).obterPorIdLancamento(1L, authentication);
    }

    @Test
    public void deveObterUmLancamentoPorId() throws RegraNegocioException {
        Long idLancamento = 1L;
        Long idUsuario = 1L;
        String emailUsuario = "usuario@teste.com";

        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        usuario.setEmail(emailUsuario);

        Lancamento lancamento = Lancamento.builder()
                .id(idLancamento)
                .ano(2025)
                .mes(11)
                .descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now())
                .usuario(usuario)
                .build();

        Mockito.when(authentication.getName()).thenReturn(emailUsuario);

        Mockito.when(usuarioService.obterIdUsuarioPorEmail(emailUsuario)).thenReturn(usuario);

        Mockito.when(repository.findLancamentoByUsuario_IdAndId(idUsuario, idLancamento))
                .thenReturn(Optional.of(lancamento));

        Lancamento resultado = service.obterPorIdLancamento(idLancamento, authentication);

        Assertions.assertThat(resultado).isNotNull();
        Assertions.assertThat(resultado.getId()).isEqualTo(idLancamento);
    }

    @Test
    public void deveRetornarErroQuandoOLancamentoNaoExistir() throws RegraNegocioException {
        Long idLancamento = 1L;
        Long idUsuario = 1L;
        String emailUsuario = "usuario@teste.com";

        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        usuario.setEmail(emailUsuario);

        Mockito.when(authentication.getName()).thenReturn(emailUsuario);

        Mockito.when(usuarioService.obterIdUsuarioPorEmail(emailUsuario)).thenReturn(usuario);

        Mockito.when(repository.findLancamentoByUsuario_IdAndId(idUsuario, idLancamento))
                .thenReturn(Optional.empty());

        Assertions.catchThrowableOfType(
                () -> service.obterPorIdLancamento(idLancamento, authentication),
                RegraNegocioException.class
        );
    }

    @Test
    public void deveLancarErrosAoValidarUmLancamento() {
        Lancamento lancamento = new Lancamento();

        Throwable erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira uma descrição válida.");

        lancamento.setDescricao("");
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira uma descrição válida.");

        lancamento.setDescricao("Salário");
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um mês válido.");

        lancamento.setMes(0);
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um mês válido.");

        lancamento.setMes(13);
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um mês válido.");

        lancamento.setMes(1);
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um ano válido.");

        lancamento.setAno(202);
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um ano válido.");

        lancamento.setAno(22025);
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um ano válido.");

        lancamento.setAno(2025);
        lancamento.setValor(BigDecimal.ZERO);
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um valor válido.");

        lancamento.setValor(BigDecimal.valueOf(-1));
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um valor válido.");

        lancamento.setValor(BigDecimal.valueOf(1));
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um tipo de transação válido.");

        lancamento.setTipoLancamento(TipoLancamento.RECEITA);
        erro = Assertions.catchThrowable(() -> service.validar(lancamento));
        Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Informe um usuário válido.");
    }

    @Test
    public void deveObterSaldoDeUmUsuario() throws RegraNegocioException {
        Long idUsuario = 1L;
        // usuarioService.obterPorId retorna Optional<Usuario> em outras partes do código
        Mockito.when(usuarioService.obterPorId(idUsuario)).thenReturn(Optional.of(new Usuario()));
        Mockito.when(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.RECEITA, StatusLancamento.EFETIVADO))
                .thenReturn(BigDecimal.valueOf(100));
        Mockito.when(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.DESPESA, StatusLancamento.EFETIVADO))
                .thenReturn(BigDecimal.valueOf(50));
        BigDecimal saldo = service.obterSaldoPorUsuario(idUsuario);
        Assertions.assertThat(saldo).isEqualTo(BigDecimal.valueOf(50));
    }
}
