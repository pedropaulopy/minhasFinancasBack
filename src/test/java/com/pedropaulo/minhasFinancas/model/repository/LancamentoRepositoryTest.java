package com.pedropaulo.minhasFinancas.model.repository;

import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import com.pedropaulo.minhasFinancas.model.enums.TipoLancamento;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class LancamentoRepositoryTest {
    @Autowired
    LancamentoRepository repository;

    @Autowired
    static TestEntityManager entityManager;

    public static Lancamento criaEPersisteLancamento(){
        Lancamento lancamento = Lancamento.builder().
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        entityManager.persist(lancamento);
        return lancamento;
    }

    @Test
    public void deveSalvarUmLancamento(){
        Lancamento lancamento = Lancamento.builder().
                ano(2025).
                mes(11).
                descricao("Lançamento teste").
                valor(BigDecimal.valueOf(100)).
                tipoLancamento(TipoLancamento.DESPESA).
                statusLancamento(StatusLancamento.PENDENTE).
                dataCadastro(LocalDate.now()).build();

        lancamento = repository.save(lancamento);
        Assertions.assertThat((lancamento.getId())).isNotNull();
    }

    @Test
    public void deveDeletarUmLancamento(){
        Lancamento lancamento = criaEPersisteLancamento();

        lancamento = entityManager.find(Lancamento.class, lancamento.getId());
        repository.delete(lancamento);
        Lancamento lancamentoInexistente = entityManager.find(Lancamento.class, lancamento.getId());
        Assertions.assertThat(lancamentoInexistente).isNull();
    }

    @Test
    public void deveAtualizarUmLancamento(){
        Lancamento lancamento = criaEPersisteLancamento();

        lancamento.setAno(2024);
        lancamento.setDescricao("Teste atualização");
        lancamento.setStatusLancamento(StatusLancamento.EFETIVADO);
        repository.save(lancamento);
        Lancamento lancamentoAtualizado = entityManager.find(Lancamento.class, lancamento.getId());
        Assertions.assertThat(lancamentoAtualizado.getAno()).isEqualTo(2024);
        Assertions.assertThat(lancamentoAtualizado.getDescricao()).isEqualTo("Teste atualização");
        Assertions.assertThat(lancamentoAtualizado.getStatusLancamento()).isEqualTo(StatusLancamento.EFETIVADO);
    }

    @Test
    public void deveBuscarUmLancamentoPorId(){
        Lancamento lancamento = criaEPersisteLancamento();
        Optional<Lancamento> lancamentoEncontrado = repository.findById(lancamento.getId());
        Assertions.assertThat((lancamentoEncontrado).isPresent()).isTrue();
    }
}
