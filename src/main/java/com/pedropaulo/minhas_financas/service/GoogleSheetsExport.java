package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.exception.RegraNegocioException;

import java.util.List;

public interface GoogleSheetsExport {
	RecordCreatedSheet criarPlanilhaCsv(List<Long> ids, String nomePlanilha, String parentFolderId) throws RegraNegocioException;

}
