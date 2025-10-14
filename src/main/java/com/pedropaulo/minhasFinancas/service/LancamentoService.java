package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;

import java.util.List;
import java.util.Optional;

public interface LancamentoService {

    Lancamento salvar(Lancamento lancamento) throws RegraNegocioException;
    Lancamento atualizar(Lancamento lancamento) throws RegraNegocioException;
    void deletar(Lancamento lancamento);
    List<Lancamento> buscar(Lancamento lancamentoFiltro);
    void atualizarStatus(Lancamento lancamento, StatusLancamento status) throws RegraNegocioException;
    void validar(Lancamento lancamento) throws RegraNegocioException;
    Optional<Lancamento> obterPorId(Long id);
}
