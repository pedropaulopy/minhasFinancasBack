package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;

import java.util.List;
import java.util.Set;

public interface CategoriaService {
    Set<Categoria> buscarOuCriarCategorias(List<String> nomesCategorias, Usuario usuario);
}