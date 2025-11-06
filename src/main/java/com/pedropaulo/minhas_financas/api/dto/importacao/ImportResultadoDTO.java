package com.pedropaulo.minhas_financas.api.dto.importacao;

import java.util.ArrayList;
import java.util.List;

public class ImportResultadoDTO {

	public static class LinhaErro {

		// públicos para manter compatibilidade com testes que fazem acesso direto
		public final long linha;

		public final String motivo;

		public final String raw;

		public LinhaErro(long linha, String motivo, String raw) {
			this.linha = linha;
			this.motivo = motivo;
			this.raw = raw;
		}

		public long getLinha() {
			return linha;
		}

		public String getMotivo() {
			return motivo;
		}

		// alias usado em alguns testes
		public String getMensagem() {
			return motivo;
		}

		public String getRaw() {
			return raw;
		}

	}

	private long totalLidas;

	private long totalSucesso;

	private long totalFalha;

	private final List<LinhaErro> erros = new ArrayList<>();

	public void incLida() {
		this.totalLidas++;
	}

	public void incSucesso() {
		this.totalSucesso++;
	}

	public void addFalha(long linha, String motivo, String raw) {
		this.totalFalha++;
		this.erros.add(new LinhaErro(linha, motivo, raw));
	}

	public long getTotalLidas() {
		return totalLidas;
	}

	public long getTotalSucesso() {
		return totalSucesso;
	}

	public long getTotalFalha() {
		return totalFalha;
	}

	public List<LinhaErro> getErros() {
		return erros;
	}

}
