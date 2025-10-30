package com.pedropaulo.minhas_financas.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

@Entity
@Table(name = "usuario", schema = "financas")
@Builder // usado para criar objetos de forma mais simples
@Data // lombok gera getters, setters hashcode e equals
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nome")
  private String nome;

  @Column(name = "email")
  private String email;

  @JsonIgnore
  @Column(name = "senha")
  private String senha;

  @Column(name = "data_cadastro")
  @Convert(converter = Jsr310JpaConverters.LocalDateConverter.class)
  private LocalDate dataCadastro;
}
