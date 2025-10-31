package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;

public interface CategoriaService {
    Set<Categoria> buscarOuCriarCategorias(List<String> nomesCategorias, Authentication authentication) throws RegraNegocioException;
    List<Categoria> buscarPorNome(Categoria categoriaFiltro);
    Categoria salvar(Categoria categoria) throws  RegraNegocioException;
    void validar(Categoria categoria) throws RegraNegocioException;
}