package com.pedropaulo.minhasFinancas.service.impl;

import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import com.pedropaulo.minhasFinancas.model.enums.TipoLancamento;
import com.pedropaulo.minhasFinancas.model.repository.LancamentoRepository;
import com.pedropaulo.minhasFinancas.service.LancamentoService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class LancamentoServiceImpl implements LancamentoService {
    private LancamentoRepository repository;

    public LancamentoServiceImpl(LancamentoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Lancamento salvar(Lancamento lancamento) throws RegraNegocioException {
        validar(lancamento);
        lancamento.setStatusLancamento(StatusLancamento.PENDENTE);
        return repository.save(lancamento);
    }

    @Override
    @Transactional
    public Lancamento atualizar(Lancamento lancamento) throws RegraNegocioException {
        Objects.requireNonNull(lancamento.getId());
        validar(lancamento);
        return repository.save(lancamento);
    }

    @Override
    public void deletar(Lancamento lancamento) {
        Objects.requireNonNull(lancamento.getId());
        repository.delete(lancamento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lancamento> buscar(Lancamento lancamentoFiltro) {
        Example example = Example.of(lancamentoFiltro, ExampleMatcher.matching().withIgnoreCase().withIgnoreCase().withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING));
        return repository.findAll(example);
    }

    @Override
    @Transactional
    public void atualizarStatus(Lancamento lancamento, StatusLancamento status) throws RegraNegocioException {
        lancamento.setStatusLancamento(status);
        atualizar(lancamento);
    }

    @Override
    public void validar(Lancamento lancamento) throws RegraNegocioException {
        if(lancamento.getDescricao() == null || lancamento.getDescricao().trim().equals("")){
            throw new RegraNegocioException("Insira uma descrição válida.");
        }
        if(lancamento.getMes() == null || lancamento.getMes()>12 || lancamento.getMes()<1){
            throw new RegraNegocioException("Insira um mês válido.");
        }
        if(lancamento.getAno() == null || lancamento.getAno().toString().length()!=4){
            throw new RegraNegocioException("Insira um ano válido.");
        }
        if(lancamento.getValor() == null || lancamento.getValor().doubleValue()<1){
            throw new RegraNegocioException("Insira um valor válido.");
        }
        if(lancamento.getTipoLancamento() == null){
            throw new RegraNegocioException("Insira um tipo de transação válido.");
        }
        if(lancamento.getUsuario() == null || lancamento.getUsuario().getId()==null){
            throw new RegraNegocioException("Informe um usuário válido.");
        }
    }

    @Override
    public Optional<Lancamento> obterPorId(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obterSaldoPorUsuario(Long id) {
        BigDecimal receitas = repository.obterSaldoPorTipoLancamentoEUsuario(id, TipoLancamento.valueOf(TipoLancamento.RECEITA.name()));
        BigDecimal despesas = repository.obterSaldoPorTipoLancamentoEUsuario(id, TipoLancamento.valueOf(TipoLancamento.DESPESA.name()));
        if(receitas == null){
            receitas = BigDecimal.ZERO;
        }
        if(despesas == null){
            despesas = BigDecimal.ZERO;
        }
        return receitas.subtract(despesas);
    }
}
