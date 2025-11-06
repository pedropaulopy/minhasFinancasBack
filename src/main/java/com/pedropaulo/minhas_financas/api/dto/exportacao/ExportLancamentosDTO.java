package com.pedropaulo.minhas_financas.api.dto.exportacao;

import lombok.Data;

import java.util.List;

@Data
public class ExportLancamentosDTO {

	private List<Long> idsRequisitados;

}
