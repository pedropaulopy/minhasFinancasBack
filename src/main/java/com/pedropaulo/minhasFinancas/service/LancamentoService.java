package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.api.dto.LancamentoDTO;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import java.math.BigDecimal;
import java.util.List;

public interface LancamentoService {

  Lancamento salvar(Lancamento lancamento) throws RegraNegocioException;

  Lancamento atualizar(Long id, LancamentoDTO dto) throws RegraNegocioException;

  void deletar(Long id) throws RegraNegocioException;

  List<Lancamento> buscar(Lancamento lancamentoFiltro);

  void atualizarStatus(Long id, StatusLancamento status) throws RegraNegocioException;

  void validar(Lancamento lancamento) throws RegraNegocioException;

  Lancamento obterPorIdLancamento(Long id) throws RegraNegocioException;

  BigDecimal obterSaldoPorUsuario(Long id) throws RegraNegocioException;

  Lancamento converterDTO(LancamentoDTO dto) throws  RegraNegocioException;
}
