package com.pedropaulo.minhas_financas.model.repository;

import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class LancamentoRepositoryTest {
    @Autowired LancamentoRepository repository;
    @Autowired TestEntityManager entityManager;

    public Lancamento criaEPersisteLancamento(){
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
    public void deveSalvarUmLancamento() {
        Lancamento lancamento = Lancamento.builder()
                .ano(2025).mes(11).descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now())
                .build();

        Lancamento salvo = repository.save(lancamento);
        assertThat(salvo.getId()).isNotNull();
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
