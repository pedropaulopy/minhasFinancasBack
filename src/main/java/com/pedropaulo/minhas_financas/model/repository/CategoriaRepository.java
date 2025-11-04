package com.pedropaulo.minhas_financas.model.repository;

import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

	Optional<Categoria> findByNomeAndUsuario(String nome, Usuario usuario);

	Optional<Categoria> findByNomeIgnoreCaseAndUsuario(String nome, Usuario usuario);

	Optional<Categoria> findByIdAndUsuario_Id(Long id, Long usuarioId);

}
