package com.pedropaulo.minhas_financas.service.impl;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.pedropaulo.minhas_financas.service.GoogleSheetsExport;
import com.pedropaulo.minhas_financas.service.LancamentoExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleSheetsExportImpl implements GoogleSheetsExport {

    private final Drive drive;
    private final LancamentoExportService exportService;
    private final Sheets sheets;

    @Override
    public CreatedSheet createSheetFromCsv(List<Long> ids, String nomePlanilha, String parentFolderId) throws Exception {
        var byteArrayOutputStream = new java.io.ByteArrayOutputStream(64 * 1024);
        exportService.streamCsvByIds(byteArrayOutputStream, ids);
        byte[] csvBytes = byteArrayOutputStream.toByteArray();

        File meta = new File();
        meta.setName((nomePlanilha == null || nomePlanilha.isBlank())
                ? "Lancamentos " + java.time.LocalDate.now()
                : nomePlanilha);
        meta.setMimeType("application/vnd.google-apps.spreadsheet");
        if (parentFolderId != null && !parentFolderId.isBlank()) {
            meta.setParents(List.of(parentFolderId));
        }

        var content = new InputStreamContent("text/csv", new java.io.ByteArrayInputStream(csvBytes));
        content.setLength(csvBytes.length);

        File created = drive.files()
                .create(meta, content)
                .setFields("id,webViewLink,webContentLink")
                .setSupportsAllDrives(true)
                .execute();

        String spreadsheetId = created.getId();

        // Descobre sheetId e rowCount da primeira aba
        Spreadsheet spreadsheet = sheets.spreadsheets()
                .get(spreadsheetId)
                .setFields("sheets(properties(sheetId,gridProperties(rowCount)))")
                .execute();

        Sheet first = spreadsheet.getSheets().get(0);
        Integer firstSheetId = first.getProperties().getSheetId();
        int rowCount = first.getProperties().getGridProperties().getRowCount();

        // Mapeamento fixo
        final int COLS = 8;    // A..I
        final int VALOR_COL = 2; // C
        final int DATA_COL  = 7; // H
        final int STATUS_COL = 6; // G
        final int TIPO_COL   = 5; // D

        // Ranges úteis
        GridRange headerRange = new GridRange()
                .setSheetId(firstSheetId)
                .setStartRowIndex(0).setEndRowIndex(1)
                .setStartColumnIndex(0).setEndColumnIndex(COLS);

        GridRange allRowsRange = new GridRange()
                .setSheetId(firstSheetId)
                .setStartRowIndex(0).setEndRowIndex(rowCount)
                .setStartColumnIndex(0).setEndColumnIndex(COLS);

        GridRange bodyRange = new GridRange()
                .setSheetId(firstSheetId)
                .setStartRowIndex(1).setEndRowIndex(rowCount)
                .setStartColumnIndex(0).setEndColumnIndex(COLS);

        GridRange valorColRange = new GridRange()
                .setSheetId(firstSheetId)
                .setStartRowIndex(1).setEndRowIndex(rowCount)
                .setStartColumnIndex(VALOR_COL).setEndColumnIndex(VALOR_COL + 1);

        GridRange statusColRange = new GridRange()
                .setSheetId(firstSheetId)
                .setStartRowIndex(1).setEndRowIndex(rowCount)
                .setStartColumnIndex(STATUS_COL).setEndColumnIndex(STATUS_COL + 1);

        GridRange tipoColRange = new GridRange()
                .setSheetId(firstSheetId)
                .setStartRowIndex(1).setEndRowIndex(rowCount)
                .setStartColumnIndex(TIPO_COL).setEndColumnIndex(TIPO_COL + 1);

        var requests = new ArrayList<Request>();

        // 1) Congelar cabeçalho
        requests.add(new Request().setUpdateSheetProperties(
                new UpdateSheetPropertiesRequest()
                        .setProperties(new SheetProperties()
                                .setSheetId(firstSheetId)
                                .setGridProperties(new GridProperties().setFrozenRowCount(1)))
                        .setFields("gridProperties.frozenRowCount")
        ));

        // 2) Fonte Inter no corpo (tamanho 11)
        requests.add(new Request().setRepeatCell(
                new RepeatCellRequest()
                        .setRange(bodyRange)
                        .setCell(new CellData().setUserEnteredFormat(
                                new CellFormat().setTextFormat(
                                        new TextFormat()
                                                .setFontFamily("Inter")
                                                .setFontSize(11)
                                )))
                        .setFields("userEnteredFormat.textFormat(fontFamily,fontSize)")
        ));

        // 3) Cabeçalho: Inter 12, negrito, centralizado
        requests.add(new Request().setRepeatCell(
                new RepeatCellRequest()
                        .setRange(headerRange)
                        .setCell(new CellData().setUserEnteredFormat(
                                new CellFormat()
                                        .setTextFormat(new TextFormat()
                                                .setBold(true)
                                                .setFontFamily("Inter")
                                                .setFontSize(12))
                                        .setHorizontalAlignment("CENTER")
                        ))
                        .setFields("userEnteredFormat(textFormat,horizontalAlignment)")
        ));

        // 4) Coluna C (VALOR) - formato moeda BRL
        requests.add(new Request().setRepeatCell(
                new RepeatCellRequest()
                        .setRange(new GridRange()
                                .setSheetId(firstSheetId)
                                .setStartRowIndex(1)
                                .setStartColumnIndex(VALOR_COL)
                                .setEndColumnIndex(VALOR_COL + 1))
                        .setCell(new CellData().setUserEnteredFormat(
                                new CellFormat().setNumberFormat(
                                        new NumberFormat()
                                                .setType("CURRENCY")
                                                .setPattern("R$ #.##0,00")
                                )))
                        .setFields("userEnteredFormat.numberFormat")
        ));

        // 5) Coluna C (VALOR) - alinhar à ESQUERDA
        requests.add(new Request().setRepeatCell(
                new RepeatCellRequest()
                        .setRange(valorColRange)
                        .setCell(new CellData().setUserEnteredFormat(
                                new CellFormat().setHorizontalAlignment("LEFT")
                        ))
                        .setFields("userEnteredFormat.horizontalAlignment")
        ));

        // 6) Coluna H (DATA) - formato mm/yyyy
        requests.add(new Request().setRepeatCell(
                new RepeatCellRequest()
                        .setRange(new GridRange()
                                .setSheetId(firstSheetId)
                                .setStartRowIndex(1)
                                .setStartColumnIndex(DATA_COL)
                                .setEndColumnIndex(DATA_COL + 1))
                        .setCell(new CellData().setUserEnteredFormat(
                                new CellFormat().setNumberFormat(
                                        new NumberFormat()
                                                .setType("DATE")
                                                .setPattern("mm/yyyy")
                                )))
                        .setFields("userEnteredFormat.numberFormat")
        ));

        // 7) STATUS (G): EFETIVADO / PENDENTE / CANCELADO
        // EFETIVADO -> verde claro
        requests.add(new Request().setAddConditionalFormatRule(
                new AddConditionalFormatRuleRequest()
                        .setRule(new ConditionalFormatRule()
                                .setRanges(List.of(statusColRange))
                                .setBooleanRule(new BooleanRule()
                                        .setCondition(new BooleanCondition()
                                                .setType("TEXT_EQ")
                                                .setValues(List.of(new ConditionValue().setUserEnteredValue("EFETIVADO"))))
                                        .setFormat(new CellFormat().setBackgroundColor(new Color()
                                                .setRed(0.80f).setGreen(0.94f).setBlue(0.80f)))))
                        .setIndex(0)
        ));
        // PENDENTE -> amarelo
        requests.add(new Request().setAddConditionalFormatRule(
                new AddConditionalFormatRuleRequest()
                        .setRule(new ConditionalFormatRule()
                                .setRanges(List.of(statusColRange))
                                .setBooleanRule(new BooleanRule()
                                        .setCondition(new BooleanCondition()
                                                .setType("TEXT_EQ")
                                                .setValues(List.of(new ConditionValue().setUserEnteredValue("PENDENTE"))))
                                        .setFormat(new CellFormat().setBackgroundColor(new Color()
                                                .setRed(1.00f).setGreen(0.95f).setBlue(0.70f)))))
                        .setIndex(1)
        ));
        // CANCELADO -> vermelho claro
        requests.add(new Request().setAddConditionalFormatRule(
                new AddConditionalFormatRuleRequest()
                        .setRule(new ConditionalFormatRule()
                                .setRanges(List.of(statusColRange))
                                .setBooleanRule(new BooleanRule()
                                        .setCondition(new BooleanCondition()
                                                .setType("TEXT_EQ")
                                                .setValues(List.of(new ConditionValue().setUserEnteredValue("CANCELADO"))))
                                        .setFormat(new CellFormat().setBackgroundColor(new Color()
                                                .setRed(0.98f).setGreen(0.80f).setBlue(0.80f)))))
                        .setIndex(2)
        ));

        // 8) TIPO (D): RECEITA / DESPESA
        // RECEITA -> #afcdff
        requests.add(new Request().setAddConditionalFormatRule(
                new AddConditionalFormatRuleRequest()
                        .setRule(new ConditionalFormatRule()
                                .setRanges(List.of(tipoColRange))
                                .setBooleanRule(new BooleanRule()
                                        .setCondition(new BooleanCondition()
                                                .setType("TEXT_EQ")
                                                .setValues(List.of(new ConditionValue().setUserEnteredValue("RECEITA"))))
                                        .setFormat(new CellFormat().setBackgroundColor(
                                                new Color().setRed(0.686f).setGreen(0.804f).setBlue(1.0f)))))
                        .setIndex(0)
        ));
        // DESPESA -> #ffc89f
        requests.add(new Request().setAddConditionalFormatRule(
                new AddConditionalFormatRuleRequest()
                        .setRule(new ConditionalFormatRule()
                                .setRanges(List.of(tipoColRange))
                                .setBooleanRule(new BooleanRule()
                                        .setCondition(new BooleanCondition()
                                                .setType("TEXT_EQ")
                                                .setValues(List.of(new ConditionValue().setUserEnteredValue("DESPESA"))))
                                        .setFormat(new CellFormat().setBackgroundColor(
                                                new Color().setRed(1.0f).setGreen(0.784f).setBlue(0.624f)))))
                        .setIndex(1)
        ));

        // 9) Filtro básico (A1:I)
        requests.add(new Request().setSetBasicFilter(
                new SetBasicFilterRequest().setFilter(new BasicFilter().setRange(allRowsRange))
        ));

        // 10) Banding (linhas alternadas) no intervalo A..I
        requests.add(new Request().setAddBanding(
                new AddBandingRequest().setBandedRange(
                        new BandedRange()
                                .setRange(new GridRange()
                                        .setSheetId(firstSheetId)
                                        .setStartRowIndex(0)
                                        .setEndRowIndex(ids.size()+1)
                                        .setStartColumnIndex(0)
                                        .setEndColumnIndex(COLS))
                                .setRowProperties(new BandingProperties()
                                        .setFirstBandColor(new Color().setRed(0.95f).setGreen(0.95f).setBlue(0.95f))
                                        .setSecondBandColor(new Color().setRed(1f).setGreen(1f).setBlue(1f)))
                )
        ));

        // 11) Larguras fixas por coluna A..H (apenas onde ≠ 100)
        // A = 170 px
        requests.add(new Request().setUpdateDimensionProperties(
                new UpdateDimensionPropertiesRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(firstSheetId)
                                .setDimension("COLUMNS")
                                .setStartIndex(0)  // A
                                .setEndIndex(1))
                        .setProperties(new DimensionProperties().setPixelSize(170))
                        .setFields("pixelSize")
        ));
        // B = 300 px
        requests.add(new Request().setUpdateDimensionProperties(
                new UpdateDimensionPropertiesRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(firstSheetId)
                                .setDimension("COLUMNS")
                                .setStartIndex(1)  // B
                                .setEndIndex(2))
                        .setProperties(new DimensionProperties().setPixelSize(300))
                        .setFields("pixelSize")
        ));
        // C = 150 px
        requests.add(new Request().setUpdateDimensionProperties(
                new UpdateDimensionPropertiesRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(firstSheetId)
                                .setDimension("COLUMNS")
                                .setStartIndex(2)  // C
                                .setEndIndex(3))
                        .setProperties(new DimensionProperties().setPixelSize(150))
                        .setFields("pixelSize")
        ));
        // F = 90 px
        requests.add(new Request().setUpdateDimensionProperties(
                new UpdateDimensionPropertiesRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(firstSheetId)
                                .setDimension("COLUMNS")
                                .setStartIndex(5)  // F
                                .setEndIndex(6))
                        .setProperties(new DimensionProperties().setPixelSize(90))
                        .setFields("pixelSize")
        ));
        // D, E, G, H ficam com o default (100), então não enviamos requests para elas.

        // Envia tudo
        sheets.spreadsheets().batchUpdate(
                spreadsheetId,
                new BatchUpdateSpreadsheetRequest().setRequests(requests)
        ).execute();

        return new CreatedSheet(created.getId(), created.getWebViewLink(), created.getWebContentLink());
    }
}
