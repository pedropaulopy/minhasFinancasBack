package com.pedropaulo.minhas_financas.api.dto.exportacao;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class exportSheetsResultadoDTO {

	private String sheetId;

	private String webViewLink;

	private String webContentLink;

}
