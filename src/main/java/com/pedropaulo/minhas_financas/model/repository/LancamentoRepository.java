package com.pedropaulo.minhas_financas.model.repository;

import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

	@Query(value = "select sum(l.valor) from Lancamento l join l.usuario u "
			+ "where u.id =:idUsuario and l.tipoLancamento =:tipo and l.statusLancamento = :status  "
			+ "and l.ano = YEAR(CURRENT_DATE) " + "and l.mes = MONTH(CURRENT_DATE)  group by u")
	BigDecimal obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(@Param("idUsuario") Long idUsuario,
			@Param("tipo") TipoLancamento tipo, @Param("status") StatusLancamento status);

	Optional<Lancamento> findLancamentoByIdAndUsuarioId(Long id, Long idUsuario);

	boolean existsByCategorias_Id(Long idCategoria);

	long countByCategorias_Id(Long idCategoria);

}
