package com.pedropaulo.minhas_financas.service;

import java.util.List;

public interface GoogleSheetsExport {

    CreatedSheet createSheetFromCsv(List<Long> ids, String nomePlanilha, String parentFolderId) throws Exception;

    record CreatedSheet(String id, String webViewLink, String webContentLink) {}
}
