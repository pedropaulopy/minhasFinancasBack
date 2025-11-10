package com.pedropaulo.minhas_financas.model.repository;

import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

	Optional<Categoria> findByNomeAndUsuario(String nome, Usuario usuario);

	Optional<Categoria> findByNomeIgnoreCaseAndUsuario(String nome, Usuario usuario);

	Optional<Categoria> findByIdAndUsuario_Id(Long id, Long usuarioId);

	@Query("select c from Categoria c where c.usuario.id = :uid and c.nome in :nomes")
	List<Categoria> findByUsuarioIdAndNomesIn(@Param("uid") Long uid, @Param("nomes") Set<String> nomes);

	List<Categoria> findByNomeIgnoreCaseInAndUsuario_Id(Collection<String> nomes, Long usuarioId);

}
