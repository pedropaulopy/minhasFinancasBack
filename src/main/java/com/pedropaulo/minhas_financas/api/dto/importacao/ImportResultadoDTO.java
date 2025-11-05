package com.pedropaulo.minhas_financas.api.dto.importacao;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ImportResultadoDTO {

	private long totalLidas;

	private long totalSucesso;

	private long totalFalha;

	public final List<AuxiliarLinhaErro> erros = new ArrayList<>();

	public void incLida() {
		this.totalLidas++;
	}

	public void incSucesso() {
		this.totalSucesso++;
	}

	@Getter
	public static class AuxiliarLinhaErro {

		public final long linha;

		public final String motivo;

		public final String raw;

		public AuxiliarLinhaErro(long linha, String motivo, String raw) {
			this.linha = linha;
			this.motivo = motivo;
			this.raw = raw;
		}

	}

	public void addFalha(long linha, String motivo, String raw) {
		this.totalFalha++;
		this.erros.add(new AuxiliarLinhaErro(linha, motivo, raw));
	}

}
