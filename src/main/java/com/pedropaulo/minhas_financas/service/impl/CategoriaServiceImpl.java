package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
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
    private  final UsuarioService service;

    @Override
    public Set<Categoria> buscarOuCriarCategorias(List<String> nomesCategorias, Authentication authentication) throws RegraNegocioException {
        Set<Categoria> categorias = new HashSet<>();
        if (nomesCategorias == null || nomesCategorias.isEmpty()) {
            return categorias;
        }
        String email = authentication.getName();
        Usuario usuario = service.obterIdUsuarioPorEmail(email);
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

    @Override
    public List<Categoria> buscarPorNome(Categoria categoriaFiltro) {
        Example example = Example.of(categoriaFiltro,
                ExampleMatcher.matching().withIgnoreCase().withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING));
        return repository.findAll(example);
    }

    @Override
    public void validar(Categoria categoria) throws RegraNegocioException {

        if (categoria.getNome() == null || categoria.getNome().trim().equals("")) {
            throw new RegraNegocioException("Insira uma nome válido.");
        }

    }

    @Override
    public Categoria salvar(Categoria categoria){
        return null;
    }
}
