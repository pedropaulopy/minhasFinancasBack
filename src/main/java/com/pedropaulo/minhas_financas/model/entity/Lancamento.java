package com.pedropaulo.minhas_financas.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "lancamento", schema = "financas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(exclude = { "usuario", "categorias" })
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lancamento {

	@Id
	@EqualsAndHashCode.Include
	@SequenceGenerator(name = "lancamento_seq_gen", sequenceName = "financas.lancamento_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lancamento_seq_gen")
	@Column(name = "id")
	private Long id;

	@Column(name = "descricao", length = 255)
	private String descricao;

	@Column(name = "mes")
	private Integer mes;

	@Column(name = "ano")
	private Integer ano;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnore
	private Usuario usuario;

	@Column(name = "valor", precision = 19, scale = 2)
	private BigDecimal valor;

	@Column(name = "data_cadastro")
	private LocalDate dataCadastro;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", length = 20)
	private TipoLancamento tipoLancamento;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20)
	private StatusLancamento statusLancamento;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "lancamento_categoria", schema = "financas", joinColumns = @JoinColumn(name = "id_lancamento"),
			inverseJoinColumns = @JoinColumn(name = "id_categoria"))
	private Set<Categoria> categorias;

}
