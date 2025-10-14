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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class LancamentoServiceTest {
    @SpyBean
    LancamentoServiceImpl service;

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
}
