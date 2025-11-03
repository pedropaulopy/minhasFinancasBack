package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.api.dto.LancamentoDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface LancamentoService {

	Lancamento salvar(Lancamento lancamento) throws RegraNegocioException;

	Lancamento atualizar(Long id, Authentication authentication, LancamentoDTO dto) throws RegraNegocioException;

	void deletar(Long id, Authentication authentication) throws RegraNegocioException;

	List<Lancamento> buscar(Lancamento lancamentoFiltro);

	void atualizarStatus(Long id, Authentication authentication, StatusLancamento dto) throws RegraNegocioException;

	void validar(Lancamento lancamento) throws RegraNegocioException;

	Lancamento obterPorIdLancamento(Long idLancamento, Authentication authentication) throws RegraNegocioException;

	BigDecimal obterSaldoPorUsuario(Long id) throws RegraNegocioException;

	Lancamento converterDTO(LancamentoDTO dto, Authentication authentication) throws RegraNegocioException;

	void validarStatusLancamento(Long id, Authentication authentication) throws RegraNegocioException;

    void salvarTodos(List<Lancamento> lote);

}
