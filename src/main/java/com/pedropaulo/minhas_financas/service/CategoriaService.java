package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CategoriaService {

	Set<Categoria> buscarOuCriarCategorias(List<String> nomesCategorias, Authentication authentication)
			throws RegraNegocioException;

	Categoria buscarOuCriarCategoria(Usuario usuario, String nome) throws RegraNegocioException;

	List<Categoria> buscarPorNome(Categoria categoriaFiltro) throws RegraNegocioException;

	Categoria salvar(Categoria categoria) throws RegraNegocioException;

	Categoria atualizar(Long idCategoria, Authentication authentication, CategoriaDTO dto) throws RegraNegocioException;

	void validar(Categoria categoria) throws RegraNegocioException;

	Categoria converterDTO(CategoriaDTO dto, Authentication authentication) throws RegraNegocioException;

	Optional<Categoria> obterPorIdCategoria(Long idCategoria, Authentication authentication)
			throws RegraNegocioException;

	void deletar(Long idCategoria, Authentication authentication) throws RegraNegocioException;

}