package com.pedropaulo.minhasFinancas.api.dto;

import java.math.BigDecimal;

/*
 Fábrica de testes para criar LancamentoDTO. Simples: usa sempre o construtor sem-args e setters,
 evitando problemas com construtores package-private / diferentes assinaturas.
*/
public final class LancamentoDTOFactory {

    private LancamentoDTOFactory() {}

    public static LancamentoDTO create(Long usuarioId, String descricao, Integer mes, Integer ano, BigDecimal valor, String tipo, String status) {
        LancamentoDTO dto = new LancamentoDTO();
        dto.setDescricao(descricao);
        dto.setMes(mes);
        dto.setAno(ano);
        dto.setValor(valor);
        dto.setUsuario(usuarioId);
        dto.setTipoLancamento(tipo);
        dto.setStatusLancamento(status);
        return dto;
    }
}
