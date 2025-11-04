package com.pedropaulo.minhas_financas.model.repository;

import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import org.assertj.core.api.Assertions;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
		statements = "TRUNCATE TABLE financas.lancamento RESTART IDENTITY CASCADE")
class LancamentoRepositoryTest {

	private static final int ANO = 2025;

	private static final int MES = 11;

	private static final String DESCRICAO = "Lançamento teste";

	private static final BigDecimal VALOR = BigDecimal.valueOf(100);

	private static final LocalDate DATA_FIXA = LocalDate.of(2025, 11, 1);

	@Autowired
	LancamentoRepository repository;

	@Autowired
	TestEntityManager entityManager;

	private Lancamento novoLancamento() {
		return Lancamento.builder()
			.ano(ANO)
			.mes(MES)
			.descricao(DESCRICAO)
			.valor(VALOR)
			.tipoLancamento(TipoLancamento.DESPESA)
			.statusLancamento(StatusLancamento.PENDENTE)
			.dataCadastro(DATA_FIXA)
			.build();
	}

	private Lancamento persistido() {
		Lancamento l = novoLancamento();
		entityManager.persist(l);
		entityManager.flush();
		entityManager.clear();
		return l;
	}

	@Test
	void deveSalvar() {
		Lancamento salvo = repository.save(novoLancamento());
		entityManager.flush();
		assertThat(salvo.getId()).isNotNull();
	}

	@Test
	void deveDeletar() {
		Lancamento lancamento = persistido();

		repository.delete(lancamento);
		entityManager.flush();
		entityManager.clear();

		assertThat(repository.findById(lancamento.getId())).isNotPresent();
	}

	@Test
	void deveAtualizar() {
		Lancamento lancamento = persistido();

		lancamento.setAno(2024);
		lancamento.setDescricao("Teste atualização");
		lancamento.setStatusLancamento(StatusLancamento.EFETIVADO);
		repository.save(lancamento);
		entityManager.flush();
		entityManager.clear();

		Lancamento reloaded = entityManager.find(Lancamento.class, lancamento.getId());
		assertThat(reloaded.getAno()).isEqualTo(2024);
		assertThat(reloaded.getDescricao()).isEqualTo("Teste atualização");
		assertThat(reloaded.getStatusLancamento()).isEqualTo(StatusLancamento.EFETIVADO);
	}

	@Test
	void deveBuscarPorId() {
		Lancamento lancamento = persistido();

		assertThat(repository.findById(lancamento.getId())).isPresent();
	}

}
