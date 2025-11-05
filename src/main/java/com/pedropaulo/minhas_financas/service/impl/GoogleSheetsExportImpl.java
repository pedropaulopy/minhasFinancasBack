package com.pedropaulo.minhas_financas.service.impl;

import com.google.api.services.drive.Drive;
import com.pedropaulo.minhas_financas.service.GoogleSheetsExport;
import com.pedropaulo.minhas_financas.service.LancamentoExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleSheetsExportImpl implements GoogleSheetsExport {

    private final Drive drive;
    private final LancamentoExportService exportService;

    @Override
    public CreatedSheet createSheetFromCsv(List<Long> ids, String nomePlanilha, String parentFolderId) throws Exception {
        var byteArrayOutputStream = new java.io.ByteArrayOutputStream(64 * 1024);
        exportService.streamCsvByIds(byteArrayOutputStream, ids);
        byte[] csvBytes = byteArrayOutputStream.toByteArray();

        com.google.api.services.drive.model.File meta = new com.google.api.services.drive.model.File();
        meta.setName((nomePlanilha == null || nomePlanilha.isBlank())
                ? "Lancamentos " + java.time.LocalDate.now()
                : nomePlanilha);
        meta.setMimeType("application/vnd.google-apps.spreadsheet");
        if (parentFolderId != null && !parentFolderId.isBlank()) {
            meta.setParents(java.util.List.of(parentFolderId));
        }

        var content = new com.google.api.client.http.InputStreamContent(
                "text/csv",
                new java.io.ByteArrayInputStream(csvBytes)
        );
        content.setLength(csvBytes.length);

        com.google.api.services.drive.model.File created = drive.files()
                .create(meta, content)
                .setFields("id,webViewLink,webContentLink")
                .setSupportsAllDrives(true)
                .execute();

        return new CreatedSheet(created.getId(), created.getWebViewLink(), created.getWebContentLink());
    }

}
