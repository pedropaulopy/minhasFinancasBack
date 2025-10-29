package com.pedropaulo.minhas_financas.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenDTO {
  private String nome;
  private String token;
}
