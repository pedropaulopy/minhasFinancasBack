package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import com.pedropaulo.minhas_financas.service.CategoriaService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

	private final CategoriaRepository repository;

	private final UsuarioService usuarioService;

	private final LancamentoRepository lancamentoRepository;

	@Override
	public List<Categoria> buscarPorNome(Categoria categoriaFiltro) throws RegraNegocioException {
		String nome = categoriaFiltro.getNome();
		if (nome != null) {
			nome = nome.trim();
			if (nome.isEmpty()) {
				categoriaFiltro.setNome(null);
			}
			else {
				categoriaFiltro.setNome(nome);
			}
		}
		Example example = Example.of(categoriaFiltro,
				ExampleMatcher.matching().withIgnoreCase().withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING));
		List<Categoria> listaCategorias = repository.findAll(example);
		if (listaCategorias.isEmpty()) {
			throw new RegraNegocioException("Nenhum lançamento encontrado para este nome.");
		}
		return listaCategorias;
	}

	@Override
	public void validar(Categoria categoria) throws RegraNegocioException {
		Optional<Categoria> listaCategorias = repository.findByNomeIgnoreCaseAndUsuario(categoria.getNome(),
				categoria.getUsuario());
		if (!listaCategorias.isEmpty()) {
			throw new RegraNegocioException("Uma categoria com esse nome já existe");
		}
		if (categoria.getNome() == null || categoria.getNome().trim().equals("")) {
			throw new RegraNegocioException("Insira uma nome válido.");
		}
	}

	@Override
	public Categoria salvar(CategoriaDTO dto, Authentication authentication) throws RegraNegocioException {
		Categoria categoria = this.converterDTO(dto, authentication);
		this.validar(categoria);
		return repository.save(categoria);
	}

	@Override
	public Categoria atualizar(Long idCategoria, Authentication authentication, CategoriaDTO dto)
			throws RegraNegocioException {
		Categoria categoriaExistente = this.obterPorIdCategoria(idCategoria, authentication)
			.orElseThrow(() -> new RegraNegocioException("Nenhuma categoria foi encontrada para o ID fornecido"));
		categoriaExistente.setNome(dto.getNome());
		String novoNome = dto.getNome();
		if (novoNome == null || novoNome.trim().isEmpty()) {
			throw new RegraNegocioException("Insira um nome válido.");
		}
		if (!categoriaExistente.getNome().equalsIgnoreCase(novoNome)) {
			Optional<Categoria> categoriaComNovoNome = repository.findByNomeIgnoreCaseAndUsuario(novoNome,
					categoriaExistente.getUsuario());
			if (categoriaComNovoNome.isPresent()) {
				throw new RegraNegocioException("Uma categoria com esse nome já existe");
			}
		}
		repository.save(categoriaExistente);

		return repository.save(categoriaExistente);
	}

	@Override
	public Categoria converterDTO(CategoriaDTO dto, Authentication authentication) throws RegraNegocioException {
		Usuario usuario = usuarioService.obterIdUsuarioPorEmail(authentication.getName());
		Categoria categoria = new Categoria();
		categoria.setUsuario(usuario);
		categoria.setNome(dto.getNome());
		return categoria;
	}

	@Override
	public Optional<Categoria> obterPorIdCategoria(Long idCategoria, Authentication authentication)
			throws RegraNegocioException {
		String email = authentication.getName();
		Long idUsuario = usuarioService.obterIdUsuarioPorEmail(email).getId();
		return repository.findByIdAndUsuario_Id(idCategoria, idUsuario);
	}

	@Override
	public void deletar(Long idCategoria, Authentication authentication) throws RegraNegocioException {

		Categoria categoria = this.obterPorIdCategoria(idCategoria, authentication).orElseThrow();

		boolean emUso = lancamentoRepository.existsByCategorias_Id(idCategoria);

		if (emUso) {
			long qtd = lancamentoRepository.countByCategorias_Id(idCategoria);
			throw new RegraNegocioException(
					"Não é possível excluir: a categoria está vinculada a " + qtd + " lançamento(s).");
		}

		repository.delete(categoria);
	}

}
