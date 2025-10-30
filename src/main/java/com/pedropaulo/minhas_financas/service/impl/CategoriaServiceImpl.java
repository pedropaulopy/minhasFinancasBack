package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {
    private final CategoriaRepository repository;

    @Override
    public Set<Categoria> buscarOuCriarCategorias(List<String> nomesCategorias, Usuario usuario) {
        Set<Categoria> categorias = new HashSet<>();
        if (nomesCategorias == null || nomesCategorias.isEmpty()) {
            return categorias;
        }
        for (String nome : nomesCategorias) {
            Optional<Categoria> categoriaExistente = repository.findByNomeAndUsuario(nome, usuario);

            if (categoriaExistente.isPresent()) {
                categorias.add(categoriaExistente.get());
            } else {
                Categoria novaCategoria = Categoria.builder()
                        .nome(nome)
                        .usuario(usuario)
                        .build();
                Categoria categoriaSalva = repository.save(novaCategoria);
                categorias.add(categoriaSalva);
            }
        }
        return categorias;
    }
}
