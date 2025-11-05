package com.pedropaulo.minhas_financas.api.dto.importacao;

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