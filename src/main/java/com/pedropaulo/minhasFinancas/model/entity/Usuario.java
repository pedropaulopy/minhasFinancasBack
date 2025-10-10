package com.pedropaulo.minhasFinancas.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name="usuario", schema="financas")
@Builder //usado para criar objetos de forma mais simples
@Data //lombok gera getters, setters hashcode e equals
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="nome")
    private String nome;

    @Column(name="email")
    private String email;

    @Column(name="senha")
    private String senha;
}
