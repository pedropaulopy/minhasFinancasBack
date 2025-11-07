package com.pedropaulo.minhas_financas.service.impl;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddBandingRequest;
import com.google.api.services.sheets.v4.model.AddConditionalFormatRuleRequest;
import com.google.api.services.sheets.v4.model.BandedRange;
import com.google.api.services.sheets.v4.model.BandingProperties;
import com.google.api.services.sheets.v4.model.BasicFilter;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BooleanCondition;
import com.google.api.services.sheets.v4.model.BooleanRule;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.ConditionValue;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.NumberFormat;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SetBasicFilterRequest;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.service.GoogleSheetsExport;
import com.pedropaulo.minhas_financas.service.LancamentoExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleSheetsExportImpl implements GoogleSheetsExport {

	private static final String CONDITION_TEXT_EQ = "TEXT_EQ";

	private static final String FIELD_PIXEL_SIZE = "pixelSize";

	private static final String DIMENSION_COLUMNS = "COLUMNS";

	private static final String MIME_GOOGLE_SHEET = "application/vnd.google-apps.spreadsheet";

	private static final String MIME_CSV = "text/csv";

	private static final String DRIVE_FIELDS = "id,webViewLink,webContentLink";

	private static final String SPREADSHEET_GET_FIELDS = "sheets(properties(sheetId,gridProperties(rowCount)))";

	private static final String FONTE = "Inter";

	private static final int TAM_BUFFER = 64 * 1024;

	private static final int COLS = 7;

	private static final int COL_VALOR = 1;

	private static final int COL_TIPO = 2;

	private static final int COL_STATUS = 3;

	private static final int COL_DATA = 6;

	private static final int HEADER_FONT = 12;

	private static final int BODY_FONT = 11;

	private static final String PADRAO_MOEDA = "R$ #.##0,00";

	private static final String PADRAO_DATA = "mm/yyyy";

	private static final List<Integer> LARGURAS_COLUNAS = Arrays.asList(170, // DESCRIÇÃO
			240, // VALOR
			150, // TIPO
			100, // STATUS
			275, // USUÁRIO
			140, // DATA
			240 // CATEGORIA
	);

	private final Drive drive;

	private final LancamentoExportService exportService;

	private final Sheets sheets;

	@Override
	public CreatedSheet createSheetFromCsv(List<Long> ids, String nomePlanilha, String parentFolderId)
			throws RegraNegocioException {
		try {
			byte[] csvBytes = gerarCsvBytes(ids);
			File arquivo = criarArquivoNoDrive(nomePlanilha, parentFolderId, csvBytes);
			String spreadsheetId = arquivo.getId();

			Spreadsheet spreadsheet = obterSpreadsheet(spreadsheetId);
			Sheet primeiraAba = spreadsheet.getSheets().get(0);

			int sheetId = primeiraAba.getProperties().getSheetId();
			int rowCount = primeiraAba.getProperties().getGridProperties().getRowCount();

			List<Request> requests = montarRequests(sheetId, rowCount, ids.size());
			aplicarBatch(spreadsheetId, requests);

			return new CreatedSheet(arquivo.getId(), arquivo.getWebViewLink(), arquivo.getWebContentLink());
		}
		catch (IOException e) {
			throw new RegraNegocioException("Falha ao acessar as APIs do Google: " + e.getMessage());
		}
	}

	private byte[] gerarCsvBytes(List<Long> ids) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(TAM_BUFFER)) {
			exportService.exportarCsvPorIds(out, ids);
			return out.toByteArray();
		}
	}

	private File criarArquivoNoDrive(String nomePlanilha, String parentFolderId, byte[] csvBytes) throws IOException {
		File meta = new File().setName(planilhaOuPadrao(nomePlanilha)).setMimeType(MIME_GOOGLE_SHEET);

		if (parentFolderId != null && !parentFolderId.isBlank()) {
			meta.setParents(List.of(parentFolderId));
		}

		InputStreamContent conteudo = new InputStreamContent(MIME_CSV, new ByteArrayInputStream(csvBytes));
		conteudo.setLength(csvBytes.length);

		return drive.files().create(meta, conteudo).setFields(DRIVE_FIELDS).setSupportsAllDrives(true).execute();
	}

	private Spreadsheet obterSpreadsheet(String spreadsheetId) throws IOException {
		return sheets.spreadsheets().get(spreadsheetId).setFields(SPREADSHEET_GET_FIELDS).execute();
	}

	private void aplicarBatch(String spreadsheetId, List<Request> requests) throws IOException {
		sheets.spreadsheets()
			.batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests))
			.execute();
	}

	private List<Request> montarRequests(int sheetId, int rowCount, int quantidadeIds) {
		GridRange header = range(sheetId, 0, 1, 0, COLS);
		GridRange corpo = range(sheetId, 1, rowCount, 0, COLS);
		GridRange todasLinhas = range(sheetId, 0, rowCount, 0, COLS);

		GridRange colValor = rangeCol(sheetId, 1, rowCount, COL_VALOR);
		GridRange colStatus = rangeCol(sheetId, 1, rowCount, COL_STATUS);
		GridRange colTipo = rangeCol(sheetId, 1, rowCount, COL_TIPO);
		GridRange colData = rangeCol(sheetId, 1, rowCount, COL_DATA);

		List<Request> requests = new ArrayList<>();
		requests.add(congelarCabecalho(sheetId));
		requests.add(formatarTextoCorpo(corpo));
		requests.add(formatarCabecalho(header));
		requests.add(formatarMoeda(colValor));
		requests.add(alinharEsquerda(colValor));
		requests.add(formatarData(colData));
		requests.addAll(regrasCondicionaisStatus(colStatus));
		requests.addAll(regrasCondicionaisTipo(colTipo));
		requests.add(aplicarFiltro(todasLinhas));
		requests.add(aplicarBanding(sheetId, quantidadeIds + 1));
		requests.addAll(definirLarguras(sheetId, LARGURAS_COLUNAS));
		return requests;
	}

	private Request congelarCabecalho(int sheetId) {
		return new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
			.setProperties(new SheetProperties().setSheetId(sheetId)
				.setGridProperties(new GridProperties().setFrozenRowCount(1)))
			.setFields("gridProperties.frozenRowCount"));
	}

	private Request formatarTextoCorpo(GridRange corpo) {
		return new Request().setRepeatCell(new RepeatCellRequest().setRange(corpo)
			.setCell(new CellData().setUserEnteredFormat(
					new CellFormat().setTextFormat(new TextFormat().setFontFamily(FONTE).setFontSize(BODY_FONT))))
			.setFields("userEnteredFormat.textFormat(fontFamily,fontSize)"));
	}

	private Request formatarCabecalho(GridRange header) {
		return new Request().setRepeatCell(new RepeatCellRequest().setRange(header)
			.setCell(new CellData().setUserEnteredFormat(new CellFormat()
				.setTextFormat(new TextFormat().setBold(true).setFontFamily(FONTE).setFontSize(HEADER_FONT))
				.setHorizontalAlignment("CENTER")))
			.setFields("userEnteredFormat(textFormat,horizontalAlignment)"));
	}

	private Request formatarMoeda(GridRange colValor) {
		return new Request().setRepeatCell(new RepeatCellRequest().setRange(colValor)
			.setCell(new CellData().setUserEnteredFormat(
					new CellFormat().setNumberFormat(new NumberFormat().setType("CURRENCY").setPattern(PADRAO_MOEDA))))
			.setFields("userEnteredFormat.numberFormat"));
	}

	private Request alinharEsquerda(GridRange col) {
		return new Request().setRepeatCell(new RepeatCellRequest().setRange(col)
			.setCell(new CellData().setUserEnteredFormat(new CellFormat().setHorizontalAlignment("LEFT")))
			.setFields("userEnteredFormat.horizontalAlignment"));
	}

	private Request formatarData(GridRange colData) {
		return new Request().setRepeatCell(new RepeatCellRequest().setRange(colData)
			.setCell(new CellData().setUserEnteredFormat(
					new CellFormat().setNumberFormat(new NumberFormat().setType("DATE").setPattern(PADRAO_DATA))))
			.setFields("userEnteredFormat.numberFormat"));
	}

	private List<Request> regrasCondicionaisStatus(GridRange colStatus) {
		List<Request> r = new ArrayList<>();
		r.add(adicionarRegra(colStatus, "EFETIVADO", new Color().setRed(0.80f).setGreen(0.94f).setBlue(0.80f), 0));
		r.add(adicionarRegra(colStatus, "PENDENTE", new Color().setRed(1.00f).setGreen(0.95f).setBlue(0.70f), 1));
		r.add(adicionarRegra(colStatus, "CANCELADO", new Color().setRed(0.98f).setGreen(0.80f).setBlue(0.80f), 2));
		return r;
	}

	private List<Request> regrasCondicionaisTipo(GridRange colTipo) {
		List<Request> r = new ArrayList<>();
		r.add(adicionarRegra(colTipo, "RECEITA", new Color().setRed(0.686f).setGreen(0.804f).setBlue(1.0f), 0));
		r.add(adicionarRegra(colTipo, "DESPESA", new Color().setRed(1.0f).setGreen(0.784f).setBlue(0.624f), 1));
		return r;
	}

	private Request adicionarRegra(GridRange range, String valor, Color cor, int index) {
		return new Request()
			.setAddConditionalFormatRule(
					new AddConditionalFormatRuleRequest()
						.setRule(
								new com.google.api.services.sheets.v4.model.ConditionalFormatRule()
									.setRanges(List.of(range))
									.setBooleanRule(
											new BooleanRule()
												.setCondition(
														new BooleanCondition().setType(CONDITION_TEXT_EQ)
															.setValues(List
																.of(new ConditionValue().setUserEnteredValue(valor))))
												.setFormat(new CellFormat().setBackgroundColor(cor))))
						.setIndex(index));
	}

	private Request aplicarFiltro(GridRange range) {
		return new Request()
			.setSetBasicFilter(new SetBasicFilterRequest().setFilter(new BasicFilter().setRange(range)));
	}

	private Request aplicarBanding(int sheetId, int totalLinhas) {
		return new Request().setAddBanding(new AddBandingRequest()
			.setBandedRange(new BandedRange().setRange(range(sheetId, 0, totalLinhas, 0, COLS))
				.setRowProperties(new BandingProperties()
					.setFirstBandColor(new Color().setRed(0.95f).setGreen(0.95f).setBlue(0.95f))
					.setSecondBandColor(new Color().setRed(1f).setGreen(1f).setBlue(1f)))));
	}

	private List<Request> definirLarguras(int sheetId, List<Integer> larguras) {
		List<Request> r = new ArrayList<>();
		for (int i = 0; i < larguras.size(); i++) {
			r.add(new Request().setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
				.setRange(new DimensionRange().setSheetId(sheetId)
					.setDimension(DIMENSION_COLUMNS)
					.setStartIndex(i)
					.setEndIndex(i + 1))
				.setProperties(new DimensionProperties().setPixelSize(larguras.get(i)))
				.setFields(FIELD_PIXEL_SIZE)));
		}
		return r;
	}

	private static String planilhaOuPadrao(String nomePlanilha) {
		return (nomePlanilha == null || nomePlanilha.isBlank()) ? "Lancamentos " + LocalDate.now() : nomePlanilha;
	}

	private static GridRange range(int sheetId, int startRow, int endRow, int startCol, int endCol) {
		return new GridRange().setSheetId(sheetId)
			.setStartRowIndex(startRow)
			.setEndRowIndex(endRow)
			.setStartColumnIndex(startCol)
			.setEndColumnIndex(endCol);
	}

	private static GridRange rangeCol(int sheetId, int startRow, int endRow, int col) {
		return range(sheetId, startRow, endRow, col, col + 1);
	}

}
