package com.pedropaulo.minhas_financas.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import lombok.*;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

@Entity
@Table(name = "lancamento", schema = "financas")
@Builder // usado para criar objetos de forma mais simples
@Data // lombok gera getters, setters hashcode e equals
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lancamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "descricao")
	private String descricao;

	@Column(name = "mes")
	private Integer mes;

	@Column(name = "ano")
	private Integer ano;

	@JoinColumn(name = "id_usuario")
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
	private Usuario usuario;

	@Column(name = "valor")
	private BigDecimal valor;

	@Column(name = "data_cadastro")
	@Convert(converter = Jsr310JpaConverters.LocalDateConverter.class)
	private LocalDate dataCadastro;

	@Column(name = "tipo")
	@Enumerated(EnumType.STRING)
	private TipoLancamento tipoLancamento;

	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private StatusLancamento statusLancamento;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lancamento_categoria",
            joinColumns = @JoinColumn(name = "id_lancamento"),
            inverseJoinColumns = @JoinColumn(name = "id_categoria")
    )
    private Set<Categoria> categorias;

}
