package com.pedropaulo.minhas_financas.service.impl;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.service.GoogleSheetsExport;
import com.pedropaulo.minhas_financas.service.LancamentoExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleSheetsExportImpl implements GoogleSheetsExport {

    private static final String CONDITION_TEXT_EQ = "TEXT_EQ";

    private static final String FIELD_PIXEL_SIZE = "pixelSize";

    private static final String DIMENSION_COLUMNS = "COLUMNS";

    private final Drive drive;

    private final LancamentoExportService exportService;

    private final Sheets sheets;

    @Override
    public CreatedSheet createSheetFromCsv(List<Long> ids, String nomePlanilha, String parentFolderId)
            throws RegraNegocioException {
        try {
            var byteArrayOutputStream = new java.io.ByteArrayOutputStream(64 * 1024);
            exportService.streamCsvByIds(byteArrayOutputStream, ids);
            byte[] csvBytes = byteArrayOutputStream.toByteArray();

            File meta = new File();
            meta.setName((nomePlanilha == null || nomePlanilha.isBlank()) ? "Lancamentos " + java.time.LocalDate.now()
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

            Spreadsheet spreadsheet = sheets.spreadsheets()
                    .get(spreadsheetId)
                    .setFields("sheets(properties(sheetId,gridProperties(rowCount)))")
                    .execute();

            Sheet first = spreadsheet.getSheets().get(0);
            Integer firstSheetId = first.getProperties().getSheetId();
            int rowCount = first.getProperties().getGridProperties().getRowCount();

            final int COLS = 7;
            final int VALOR_COL = 1;
            final int DATA_COL = 6;
            final int STATUS_COL = 3;
            final int TIPO_COL = 2;

            GridRange headerRange = new GridRange().setSheetId(firstSheetId)
                    .setStartRowIndex(0)
                    .setEndRowIndex(1)
                    .setStartColumnIndex(0)
                    .setEndColumnIndex(COLS);

            GridRange allRowsRange = new GridRange().setSheetId(firstSheetId)
                    .setStartRowIndex(0)
                    .setEndRowIndex(rowCount)
                    .setStartColumnIndex(0)
                    .setEndColumnIndex(COLS);

            GridRange bodyRange = new GridRange().setSheetId(firstSheetId)
                    .setStartRowIndex(1)
                    .setEndRowIndex(rowCount)
                    .setStartColumnIndex(0)
                    .setEndColumnIndex(COLS);

            GridRange valorColRange = new GridRange().setSheetId(firstSheetId)
                    .setStartRowIndex(1)
                    .setEndRowIndex(rowCount)
                    .setStartColumnIndex(VALOR_COL)
                    .setEndColumnIndex(VALOR_COL + 1);

            GridRange statusColRange = new GridRange().setSheetId(firstSheetId)
                    .setStartRowIndex(1)
                    .setEndRowIndex(rowCount)
                    .setStartColumnIndex(STATUS_COL)
                    .setEndColumnIndex(STATUS_COL + 1);

            GridRange tipoColRange = new GridRange().setSheetId(firstSheetId)
                    .setStartRowIndex(1)
                    .setEndRowIndex(rowCount)
                    .setStartColumnIndex(TIPO_COL)
                    .setEndColumnIndex(TIPO_COL + 1);

            var requests = new ArrayList<Request>();

            requests.add(new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
                    .setProperties(new SheetProperties().setSheetId(firstSheetId)
                            .setGridProperties(new GridProperties().setFrozenRowCount(1)))
                    .setFields("gridProperties.frozenRowCount")));

            requests.add(new Request().setRepeatCell(new RepeatCellRequest().setRange(bodyRange)
                    .setCell(new CellData().setUserEnteredFormat(
                            new CellFormat().setTextFormat(new TextFormat().setFontFamily("Inter").setFontSize(11))))
                    .setFields("userEnteredFormat.textFormat(fontFamily,fontSize)")));

            requests.add(new Request().setRepeatCell(new RepeatCellRequest().setRange(headerRange)
                    .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                            .setTextFormat(new TextFormat().setBold(true).setFontFamily("Inter").setFontSize(12))
                            .setHorizontalAlignment("CENTER")))
                    .setFields("userEnteredFormat(textFormat,horizontalAlignment)")));

            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                    .setRange(new GridRange().setSheetId(firstSheetId)
                            .setStartRowIndex(1)
                            .setStartColumnIndex(VALOR_COL)
                            .setEndColumnIndex(VALOR_COL + 1))
                    .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                            .setNumberFormat(new NumberFormat().setType("CURRENCY").setPattern("R$ #.##0,00"))))
                    .setFields("userEnteredFormat.numberFormat")));

            requests.add(new Request().setRepeatCell(new RepeatCellRequest().setRange(valorColRange)
                    .setCell(new CellData().setUserEnteredFormat(new CellFormat().setHorizontalAlignment("LEFT")))
                    .setFields("userEnteredFormat.horizontalAlignment")));

            requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                    .setRange(new GridRange().setSheetId(firstSheetId)
                            .setStartRowIndex(1)
                            .setStartColumnIndex(DATA_COL)
                            .setEndColumnIndex(DATA_COL + 1))
                    .setCell(new CellData().setUserEnteredFormat(
                            new CellFormat().setNumberFormat(new NumberFormat().setType("DATE").setPattern("mm/yyyy"))))
                    .setFields("userEnteredFormat.numberFormat")));

            requests.add(
                    new Request().setAddConditionalFormatRule(
                            new AddConditionalFormatRuleRequest()
                                    .setRule(
                                            new ConditionalFormatRule().setRanges(List.of(statusColRange))
                                                    .setBooleanRule(
                                                            new BooleanRule()
                                                                    .setCondition(new BooleanCondition()
                                                                            .setType(CONDITION_TEXT_EQ)
                                                                            .setValues(
                                                                                    List.of(new ConditionValue()
                                                                                            .setUserEnteredValue("EFETIVADO"))))
                                                                    .setFormat(new CellFormat()
                                                                            .setBackgroundColor(new Color().setRed(0.80f)
                                                                                    .setGreen(0.94f)
                                                                                    .setBlue(0.80f)))))
                                    .setIndex(0)));

            requests.add(
                    new Request().setAddConditionalFormatRule(
                            new AddConditionalFormatRuleRequest()
                                    .setRule(
                                            new ConditionalFormatRule().setRanges(List.of(statusColRange))
                                                    .setBooleanRule(
                                                            new BooleanRule()
                                                                    .setCondition(new BooleanCondition()
                                                                            .setType(CONDITION_TEXT_EQ)
                                                                            .setValues(
                                                                                    List.of(new ConditionValue()
                                                                                            .setUserEnteredValue("PENDENTE"))))
                                                                    .setFormat(new CellFormat()
                                                                            .setBackgroundColor(new Color().setRed(1.00f)
                                                                                    .setGreen(0.95f)
                                                                                    .setBlue(0.70f)))))
                                    .setIndex(1)));

            requests.add(
                    new Request().setAddConditionalFormatRule(
                            new AddConditionalFormatRuleRequest()
                                    .setRule(
                                            new ConditionalFormatRule().setRanges(List.of(statusColRange))
                                                    .setBooleanRule(
                                                            new BooleanRule()
                                                                    .setCondition(new BooleanCondition()
                                                                            .setType(CONDITION_TEXT_EQ)
                                                                            .setValues(
                                                                                    List.of(new ConditionValue()
                                                                                            .setUserEnteredValue("CANCELADO"))))
                                                                    .setFormat(new CellFormat()
                                                                            .setBackgroundColor(new Color().setRed(0.98f)
                                                                                    .setGreen(0.80f)
                                                                                    .setBlue(0.80f)))))
                                    .setIndex(2)));

            requests.add(
                    new Request()
                            .setAddConditionalFormatRule(
                                    new AddConditionalFormatRuleRequest().setRule(
                                                    new ConditionalFormatRule().setRanges(List.of(tipoColRange))
                                                            .setBooleanRule(
                                                                    new BooleanRule()
                                                                            .setCondition(new BooleanCondition()
                                                                                    .setType(CONDITION_TEXT_EQ)
                                                                                    .setValues(
                                                                                            List.of(new ConditionValue()
                                                                                                    .setUserEnteredValue("RECEITA"))))
                                                                            .setFormat(new CellFormat()
                                                                                    .setBackgroundColor(new Color().setRed(0.686f)
                                                                                            .setGreen(0.804f)
                                                                                            .setBlue(1.0f)))))
                                            .setIndex(0)));

            requests.add(
                    new Request()
                            .setAddConditionalFormatRule(
                                    new AddConditionalFormatRuleRequest().setRule(
                                                    new ConditionalFormatRule().setRanges(List.of(tipoColRange))
                                                            .setBooleanRule(
                                                                    new BooleanRule()
                                                                            .setCondition(new BooleanCondition()
                                                                                    .setType(CONDITION_TEXT_EQ)
                                                                                    .setValues(
                                                                                            List.of(new ConditionValue()
                                                                                                    .setUserEnteredValue("DESPESA"))))
                                                                            .setFormat(new CellFormat()
                                                                                    .setBackgroundColor(new Color().setRed(1.0f)
                                                                                            .setGreen(0.784f)
                                                                                            .setBlue(0.624f)))))
                                            .setIndex(1)));

            requests.add(new Request()
                    .setSetBasicFilter(new SetBasicFilterRequest().setFilter(new BasicFilter().setRange(allRowsRange))));

            requests.add(new Request().setAddBanding(new AddBandingRequest().setBandedRange(new BandedRange()
                    .setRange(new GridRange().setSheetId(firstSheetId)
                            .setStartRowIndex(0)
                            .setEndRowIndex(ids.size() + 1)
                            .setStartColumnIndex(0)
                            .setEndColumnIndex(COLS))
                    .setRowProperties(new BandingProperties()
                            .setFirstBandColor(new Color().setRed(0.95f).setGreen(0.95f).setBlue(0.95f))
                            .setSecondBandColor(new Color().setRed(1f).setGreen(1f).setBlue(1f))))));

            // Define os tamanhos das colunas em ordem
            List<Integer> columnWidths = List.of(
                    170, // DESC
                    240, // VALOR_LANC
                    150, // TIPO
                    100, // STATUS
                    275, // USUARIO
                    140, // DATA_LANC
                    240  // CATEGORIA
            );

            // Itera sobre a lista e cria um request de atualização de dimensão para cada coluna
            for (int i = 0; i < columnWidths.size(); i++) {
                requests.add(new Request().setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                        .setRange(new DimensionRange().setSheetId(firstSheetId)
                                .setDimension(DIMENSION_COLUMNS)
                                .setStartIndex(i)
                                .setEndIndex(i + 1)) // O índice final é exclusivo
                        .setProperties(new DimensionProperties().setPixelSize(columnWidths.get(i)))
                        .setFields(FIELD_PIXEL_SIZE)));
            }

            sheets.spreadsheets()
                    .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests))
                    .execute();

            return new CreatedSheet(created.getId(), created.getWebViewLink(), created.getWebContentLink());
        }
        catch (java.io.IOException e) {
            throw new RegraNegocioException("Falha ao acessar as APIs do Google: " + e.getMessage());
        }
    }

}