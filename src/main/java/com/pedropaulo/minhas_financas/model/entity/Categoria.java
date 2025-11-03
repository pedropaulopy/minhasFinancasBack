package com.pedropaulo.minhas_financas.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "categoria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "nome")
	private String nome;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnore
    @ToString.Exclude
	private Usuario usuario;

	@ManyToMany(mappedBy = "categorias", fetch = FetchType.LAZY)
	@JsonIgnore
	private Set<Lancamento> lancamentos;

}
