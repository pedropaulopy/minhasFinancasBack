package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.api.dto.LancamentoDTO;
import com.pedropaulo.minhasFinancas.api.dto.LancamentoStatusDTO;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;

public interface LancamentoService {

  Lancamento salvar(Lancamento lancamento) throws RegraNegocioException;

  Lancamento atualizar(Long id, Authentication authentication, LancamentoDTO dto) throws RegraNegocioException;

  void deletar(Long id, Authentication authentication) throws RegraNegocioException;

  List<Lancamento> buscar(Lancamento lancamentoFiltro);

  void atualizarStatus(Long id, Authentication authentication, StatusLancamento dto) throws RegraNegocioException;

  void validar(Lancamento lancamento) throws RegraNegocioException;

  Lancamento obterPorIdLancamento(Long idLancamento, Authentication authentication) throws RegraNegocioException;

  BigDecimal obterSaldoPorUsuario(Long id) throws RegraNegocioException;

  Lancamento converterDTO(LancamentoDTO dto, Authentication authentication) throws  RegraNegocioException;
}
