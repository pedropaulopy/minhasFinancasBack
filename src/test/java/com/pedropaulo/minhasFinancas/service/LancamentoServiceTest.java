package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import com.pedropaulo.minhasFinancas.model.enums.TipoLancamento;
import com.pedropaulo.minhasFinancas.model.repository.LancamentoRepository;
import com.pedropaulo.minhasFinancas.model.repository.LancamentoRepositoryTest;
import com.pedropaulo.minhasFinancas.service.impl.LancamentoServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Example;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class LancamentoServiceTest {
    @SuppressWarnings("removal")
    @SpyBean
    LancamentoServiceImpl service;

    @SuppressWarnings("removal")
    @MockBean
    LancamentoRepository repository;

    @Test
    public void deveSalvarUmLancamento() throws RegraNegocioException {
        Lancamento lancamentoASalvar = Lancamento.builder().
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        Mockito.doNothing().when(service).validar(lancamentoASalvar);
        Lancamento lancamentoSalvo = Lancamento.builder().
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();
        lancamentoSalvo.setId(1l);
        lancamentoSalvo.setStatusLancamento(StatusLancamento.PENDENTE);
        Mockito.when(repository.save(lancamentoASalvar)).thenReturn(lancamentoSalvo);
        Lancamento lancamento = service.salvar(lancamentoASalvar);
        Assertions.assertThat(lancamento.getId()).isEqualTo(lancamentoSalvo.getId());
        Assertions.assertThat(lancamento.getStatusLancamento()).isEqualTo(StatusLancamento.PENDENTE);
    }

    @Test
    public void deveLancarErroAoTentarSalvarUmLancamentoInvalido() throws RegraNegocioException {
        Lancamento lancamentoASalvar = Lancamento.builder().
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        Mockito.doThrow(RegraNegocioException.class).when(service).validar(lancamentoASalvar);
        Assertions.catchThrowableOfType(() -> service.salvar(lancamentoASalvar), RegraNegocioException.class);
        Mockito.verify(repository, Mockito.never()).save(lancamentoASalvar);
    }

    @Test
    public void deveAtualizarUmLancamento() throws RegraNegocioException {
        Lancamento lancamentoSalvo = Lancamento.builder().
                id(1l).
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        Mockito.doNothing().when(service).validar(lancamentoSalvo);
        Mockito.when(repository.save(lancamentoSalvo)).thenReturn(lancamentoSalvo);
        service.atualizar(lancamentoSalvo);
        Mockito.verify(repository, Mockito.times(1)).save(lancamentoSalvo);
    }

    @Test
    public void deveLancarErroAoTentarAtualizarUmLancamentoQueAindaNaoFoiSalvo() throws RegraNegocioException {
        Lancamento lancamentoASalvar = Lancamento.builder().
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        Assertions.catchThrowableOfType(() -> service.atualizar(lancamentoASalvar), NullPointerException.class);
        Mockito.verify(repository, Mockito.never()).save(lancamentoASalvar);
    }

    @Test
    public void deveDeletarUmLancamento() {
        Lancamento lancamento = Lancamento.builder().
                id(1l).
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        service.deletar(lancamento);
        Mockito.verify(repository).delete(lancamento);
    }

    @Test
    public void deveLancarErroAoTentarDeletarUmLancamentoQueAindaNaoFoiSalvo() {
        Lancamento lancamento = Lancamento.builder().
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        Assertions.catchThrowableOfType(() -> service.deletar(lancamento), NullPointerException.class);
        Mockito.verify(repository, Mockito.never()).delete(lancamento);
    }

    @Test
    public void deveFiltrarLancamentos() {
        Lancamento lancamento = Lancamento.builder().
                id(1l).
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        List<Lancamento> lista = Arrays.asList(lancamento);
        Mockito.when(repository.findAll(Mockito.any(Example.class))).thenReturn(lista);
        List<Lancamento> resultado = service.buscar(lancamento);
        Assertions.assertThat(resultado).isNotEmpty().hasSize(1).contains(lancamento);
    }

    @Test
    public void deveAtualizarOStatusDeUmLancamento() throws RegraNegocioException {
        Lancamento lancamento = Lancamento.builder().
                id(1l).
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        StatusLancamento novoStatus = StatusLancamento.EFETIVADO;
        Mockito.doReturn(lancamento).when(service).atualizar(lancamento);
        service.atualizarStatus(lancamento, novoStatus);
        Assertions.assertThat(lancamento.getStatusLancamento()).isEqualTo(novoStatus);
        Mockito.verify(service).atualizar(lancamento);
    }

    @Test
    public void deveObterUmLancamentoPorId() {
        Lancamento lancamento = Lancamento.builder().
                id(1l).
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();
        Long id = 1l;
        Mockito.when(repository.findById(id)).thenReturn(java.util.Optional.of(lancamento));
        java.util.Optional<Lancamento> resultado = service.obterPorId(id);
        Assertions.assertThat(resultado.isPresent()).isTrue();
    }

    @Test
    public void deveRetornarVazioQuandoOLancamentoNaoExistir() {
        Long id = 1l;
        Mockito.when(repository.findById(id)).thenReturn(java.util.Optional.empty());
        java.util.Optional<Lancamento> resultado = service.obterPorId(id);
        Assertions.assertThat(resultado.isPresent()).isFalse();
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
    public void deveObterSaldoDeUmUsuario() {
        Long idUsuario = 1l;
        Mockito.when(repository.obterSaldoPorTipoLancamentoEUsuario(idUsuario, TipoLancamento.RECEITA)).thenReturn(BigDecimal.valueOf(100));
        Mockito.when(repository.obterSaldoPorTipoLancamentoEUsuario(idUsuario, TipoLancamento.DESPESA)).thenReturn(BigDecimal.valueOf(50));
        BigDecimal saldo = service.obterSaldoPorUsuario(idUsuario);
        Assertions.assertThat(saldo).isEqualTo(BigDecimal.valueOf(50));
    }
}
