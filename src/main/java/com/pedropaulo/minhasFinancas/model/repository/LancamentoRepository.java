package com.pedropaulo.minhasFinancas.model.repository;

import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import com.pedropaulo.minhasFinancas.model.enums.TipoLancamento;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
    @Query(
            value = "select sum(l.valor) from Lancamento l join l.usuario u " +
                    "where u.id =:idUsuario and l.tipoLancamento =:tipo and l.statusLancamento = :status group by u")
    BigDecimal obterSaldoPorTipoLancamentoEUsuario(
            @Param("idUsuario") Long idUsuario,
            @Param("tipo") TipoLancamento tipo,
            @Param("status") StatusLancamento status
    );
}