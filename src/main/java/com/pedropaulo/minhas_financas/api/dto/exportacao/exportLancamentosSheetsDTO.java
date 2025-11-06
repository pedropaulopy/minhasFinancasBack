package com.pedropaulo.minhas_financas.api.dto.exportacao;

import lombok.Data;
import java.util.List;

@Data
public class exportLancamentosSheetsDTO {

	private List<Long> idsRequisitados;

	private String nomePlanilha;

	private String folderId;

}
