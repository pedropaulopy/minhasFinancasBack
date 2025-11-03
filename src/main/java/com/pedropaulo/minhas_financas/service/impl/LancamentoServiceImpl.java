package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.api.dto.LancamentoDTO;
import com.pedropaulo.minhas_financas.exception.EntidadeNaoProcessavelException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import com.pedropaulo.minhas_financas.service.CategoriaService;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LancamentoServiceImpl implements LancamentoService {

	private final LancamentoRepository repository;

	private final UsuarioService usuarioService;

	private final CategoriaService categoriaService;

	public LancamentoServiceImpl(LancamentoRepository repository, UsuarioService usuarioService,
			CategoriaService categoriaService) {
		this.repository = repository;
		this.usuarioService = usuarioService;
		this.categoriaService = categoriaService;
	}

	@Override
	@Transactional
	public Lancamento salvar(Lancamento lancamento) throws RegraNegocioException {
		validar(lancamento);
		lancamento.setStatusLancamento(StatusLancamento.PENDENTE);
		return repository.save(lancamento);
	}

	@Transactional
	public Lancamento atualizar(Long id, Authentication authentication, LancamentoDTO dto)
			throws RegraNegocioException {
		this.validarStatusLancamento(id, authentication);
		Lancamento lancamento = this.obterPorIdLancamento(id, authentication);

		lancamento.setDescricao(dto.getDescricao());
		lancamento.setValor(dto.getValor());
		lancamento.setMes(dto.getMes());
		lancamento.setAno(dto.getAno());
		lancamento.setTipoLancamento(TipoLancamento.valueOf(dto.getTipoLancamento()));
		lancamento.setStatusLancamento(StatusLancamento.valueOf(dto.getStatusLancamento()));
		Set<Categoria> categorias = categoriaService.buscarOuCriarCategorias(dto.getCategorias(), authentication);
		lancamento.setCategorias(categorias);
		return repository.save(lancamento);
	}

	@Override
	public void deletar(Long id, Authentication authentication) throws RegraNegocioException {
		this.validarStatusLancamento(id, authentication);
		Lancamento lancamento = this.obterPorIdLancamento(id, authentication);
		repository.delete(lancamento);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Lancamento> buscar(Lancamento lancamentoFiltro) {
		Example example = Example.of(lancamentoFiltro,
				ExampleMatcher.matching()
					.withIgnoreCase()
					.withIgnoreCase()
					.withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING));
		return repository.findAll(example);
	}

	@Override
	@Transactional
	public void atualizarStatus(Long id, Authentication authentication, StatusLancamento status)
			throws RegraNegocioException {
		this.validarStatusLancamento(id, authentication);
		Lancamento lancamento = this.obterPorIdLancamento(id, authentication);
		if (status == null) {
			throw new RegraNegocioException(
					"Não foi possível atualizar o status do lançamento, envie um status válido.");
		}
		lancamento.setStatusLancamento(status);
	}

	@Override
	public void validar(Lancamento lancamento) throws RegraNegocioException {
		if (lancamento.getDescricao() == null || lancamento.getDescricao().trim().equals("")) {
			throw new RegraNegocioException("Insira uma descrição válida.");
		}
		if (lancamento.getMes() == null || lancamento.getMes() > 12 || lancamento.getMes() < 1) {
			throw new RegraNegocioException("Insira um mês válido.");
		}
		if (lancamento.getAno() == null || lancamento.getAno().toString().length() != 4) {
			throw new RegraNegocioException("Insira um ano válido.");
		}
		if (lancamento.getValor() == null || lancamento.getValor().doubleValue() < 1) {
			throw new RegraNegocioException("Insira um valor válido.");
		}
		if (lancamento.getTipoLancamento() == null) {
			throw new RegraNegocioException("Insira um tipo de transação válido.");
		}
		if (lancamento.getUsuario() == null || lancamento.getUsuario().getId() == null) {
			throw new RegraNegocioException("Informe um usuário válido.");
		}
	}

	public Lancamento obterPorIdLancamento(Long idLancamento, Authentication authentication)
			throws RegraNegocioException {
		Usuario usuario = usuarioService.obterIdUsuarioPorEmail(authentication.getName());
		Long idUsuario = usuario.getId();
		return repository.findLancamentoByIdAndUsuarioId(idLancamento, idUsuario)
			.orElseThrow(() -> new RegraNegocioException("Lançamento não encontrado para o ID informado."));
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal obterSaldoPorUsuario(Long id) throws RegraNegocioException {
		usuarioService.obterPorId(id);
		BigDecimal receitas = repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(id,
				TipoLancamento.valueOf(TipoLancamento.RECEITA.name()),
				StatusLancamento.valueOf(StatusLancamento.EFETIVADO.name()));
		BigDecimal despesas = repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(id,
				TipoLancamento.valueOf(TipoLancamento.DESPESA.name()),
				StatusLancamento.valueOf(StatusLancamento.EFETIVADO.name()));
		if (receitas == null) {
			receitas = BigDecimal.ZERO;
		}
		if (despesas == null) {
			despesas = BigDecimal.ZERO;
		}
		return receitas.subtract(despesas);
	}

	@Override
	public Lancamento converterDTO(LancamentoDTO dto, Authentication authentication) throws RegraNegocioException {
		Usuario usuario = usuarioService.obterIdUsuarioPorEmail(authentication.getName());
		Lancamento lancamento = new Lancamento();
		lancamento.setDescricao(dto.getDescricao());
		lancamento.setMes(dto.getMes());
		lancamento.setAno(dto.getAno());
		lancamento.setValor(dto.getValor());
		lancamento.setDataCadastro(LocalDate.now());
		lancamento.setUsuario(usuario);
		lancamento.setTipoLancamento(TipoLancamento.valueOf(dto.getTipoLancamento()));
		lancamento.setStatusLancamento(StatusLancamento.valueOf(dto.getStatusLancamento()));
		Set<Categoria> categorias = categoriaService.buscarOuCriarCategorias(dto.getCategorias(), authentication);
		lancamento.setCategorias(categorias);
		return lancamento;
	}

	@Override
	public void validarStatusLancamento(Long idLancamento, Authentication authentication) throws RegraNegocioException {
		Lancamento lancamento = this.obterPorIdLancamento(idLancamento, authentication);
		if (lancamento.getStatusLancamento() != StatusLancamento.PENDENTE) {
			throw new EntidadeNaoProcessavelException(
					"Lançamentos efetivados ou cancelados não podem ser editados ou deletados.");
		}
	}

    @Override
    @Transactional
    public void salvarTodos(List<Lancamento> lote) {
        repository.saveAll(lote);
        repository.flush();
    }


}
