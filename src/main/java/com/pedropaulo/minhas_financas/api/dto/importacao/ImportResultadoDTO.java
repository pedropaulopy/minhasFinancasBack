package com.pedropaulo.minhas_financas.api.dto.importacao;
import java.util.ArrayList;
import java.util.List;

public class ImportResultadoDTO {

	private long totalLidas;

	private long totalSucesso;

	private long totalFalha;

	private final List<AuxiliarLinhaErro> erros = new ArrayList<>();

	public void incLida() {
		this.totalLidas++;
	}

	public void incSucesso() {
		this.totalSucesso++;
	}

	public void addFalha(long linha, String motivo, String raw) {
		this.totalFalha++;
		this.erros.add(new AuxiliarLinhaErro(linha, motivo, raw));
	}

}
