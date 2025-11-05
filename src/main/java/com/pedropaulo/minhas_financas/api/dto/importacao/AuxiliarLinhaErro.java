package com.pedropaulo.minhas_financas.api.dto.importacao;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuxiliarLinhaErro {

    public final long linha;

    public final String motivo;

    public final String raw;

    public AuxiliarLinhaErro(long linha, String motivo, String raw) {
        this.linha = linha;
        this.motivo = motivo;
        this.raw = raw;
    }

}