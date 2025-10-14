package com.pedropaulo.minhasFinancas.model.entity;

import jakarta.persistence.*;
import lombok.*;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import com.pedropaulo.minhasFinancas.model.enums.TipoLancamento;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="lancamento", schema="financas")
@Builder //usado para criar objetos de forma mais simples
@Data //lombok gera getters, setters hashcode e equals
@NoArgsConstructor
@AllArgsConstructor
public class Lancamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="descricao")
    private String descricao;


    @Column(name="mes")
    private Integer mes;

    @Column(name="ano")
    private Integer ano;

    @ManyToOne
    @JoinColumn(name="id_usuario")
    private Usuario usuario;

    @Column(name="valor")
    private BigDecimal valor;

    @Column(name="data_cadastro")
    @Convert(converter = Jsr310JpaConverters.LocalDateConverter.class)
    private LocalDate dataCadastro;

    @Column(name="tipo")
    @Enumerated(EnumType.STRING)
    private TipoLancamento tipoLancamento;

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private StatusLancamento statusLancamento;
}
