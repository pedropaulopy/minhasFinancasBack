package com.pedropaulo.minhas_financas.service.impl;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.pedropaulo.minhas_financas.service.GoogleSheetsExport;
import com.pedropaulo.minhas_financas.service.LancamentoExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class GoogleSheetsExportImplTest {

	@Mock
	private Drive drive;

	@Mock
	private Drive.Files driveFiles;

	@Mock
	private Drive.Files.Create driveFilesCreate;

	@Mock
	private Sheets sheets;

	@Mock
	private Sheets.Spreadsheets spreadsheets;

	@Mock
	private Sheets.Spreadsheets.Get spreadsheetsGet;

	@Mock
	private Sheets.Spreadsheets.BatchUpdate spreadsheetsBatchUpdate;

	@Mock
	private LancamentoExportService exportService;

	private GoogleSheetsExportImpl service;

	@BeforeEach
	void setUp() throws Exception {
		service = new GoogleSheetsExportImpl(drive, exportService, sheets);

		when(drive.files()).thenReturn(driveFiles);
		when(driveFiles.create(any(File.class), any(InputStreamContent.class))).thenReturn(driveFilesCreate);
		when(driveFilesCreate.setFields(anyString())).thenReturn(driveFilesCreate);
		when(driveFilesCreate.setSupportsAllDrives(true)).thenReturn(driveFilesCreate);

		when(sheets.spreadsheets()).thenReturn(spreadsheets);
		when(spreadsheets.get(anyString())).thenReturn(spreadsheetsGet);
		when(spreadsheetsGet.setFields(anyString())).thenReturn(spreadsheetsGet);
		when(spreadsheets.batchUpdate(anyString(), any(BatchUpdateSpreadsheetRequest.class)))
			.thenReturn(spreadsheetsBatchUpdate);
	}

	@Test
	void createSheetFromCsv_deveCriarPlanilhaUsandoNomePadraoEComPastaPai() throws Exception {
		String createdId = "spreadsheet-123";
		String webViewLink = "https://view/link";
		String webContentLink = "https://content/link";
		File createdFile = new File();
		createdFile.setId(createdId);
		createdFile.setWebViewLink(webViewLink);
		createdFile.setWebContentLink(webContentLink);
		when(driveFilesCreate.execute()).thenReturn(createdFile);

		Spreadsheet spreadsheet = spreadsheetWithSingleSheet(321, 100);
		when(spreadsheetsGet.execute()).thenReturn(spreadsheet);
		when(spreadsheetsBatchUpdate.execute()).thenReturn(new BatchUpdateSpreadsheetResponse());

		doAnswer(invocation -> {
			OutputStream out = invocation.getArgument(0, OutputStream.class);
			out.write("A,B,C\n1,2,3\n".getBytes());
			return null;
		}).when(exportService).streamCsvByIds(any(OutputStream.class), anyList());

		List<Long> ids = List.of(10L, 20L, 30L);
		String parentFolderId = "folder-999";
		String nomePlanilha = "   ";

		GoogleSheetsExport.CreatedSheet result = service.createSheetFromCsv(ids, nomePlanilha, parentFolderId);

		assertThat(result.id()).isEqualTo(createdId);
		assertThat(result.webViewLink()).isEqualTo(webViewLink);
		assertThat(result.webContentLink()).isEqualTo(webContentLink);

		ArgumentCaptor<File> metaCaptor = ArgumentCaptor.forClass(File.class);
		ArgumentCaptor<InputStreamContent> contentCaptor = ArgumentCaptor.forClass(InputStreamContent.class);
		verify(driveFiles).create(metaCaptor.capture(), contentCaptor.capture());

		File sentMeta = metaCaptor.getValue();
		assertThat(sentMeta.getMimeType()).isEqualTo("application/vnd.google-apps.spreadsheet");
		assertThat(sentMeta.getParents()).containsExactly(parentFolderId);
		assertThat(sentMeta.getName()).startsWith("Lancamentos " + LocalDate.now());

		InputStreamContent sentContent = contentCaptor.getValue();
		assertThat(sentContent.getType()).isEqualTo("text/csv");

		ArgumentCaptor<BatchUpdateSpreadsheetRequest> batchCaptor = ArgumentCaptor
			.forClass(BatchUpdateSpreadsheetRequest.class);
		verify(spreadsheets).batchUpdate(eq(createdId), batchCaptor.capture());
		BatchUpdateSpreadsheetRequest batchRequest = batchCaptor.getValue();

		// ### CORREÇÃO 1: O total de requisições agora é 20 ###
		assertThat(batchRequest.getRequests()).hasSize(20);

		AddBandingRequest bandingRequest = findAddBandingRequest(batchRequest.getRequests());
		assertThat(bandingRequest).isNotNull();
		assertThat(bandingRequest.getBandedRange().getRange().getEndRowIndex()).isEqualTo(ids.size() + 1);

		List<UpdateDimensionPropertiesRequest> dimensionRequests = findDimensionUpdates(batchRequest.getRequests());

		// ### CORREÇÃO 2: O total de redimensionamentos de coluna agora é 7 ###
		assertThat(dimensionRequests).hasSize(7);

		// ### CORREÇÃO 3: Verificando os novos tamanhos de pixel para todas as 7 colunas
		// ###
		assertThat(pixelSizeForColumn(dimensionRequests, 0)).isEqualTo(170);
		assertThat(pixelSizeForColumn(dimensionRequests, 1)).isEqualTo(240); // Era 300
		assertThat(pixelSizeForColumn(dimensionRequests, 2)).isEqualTo(150);
		assertThat(pixelSizeForColumn(dimensionRequests, 3)).isEqualTo(100); // Novo
		assertThat(pixelSizeForColumn(dimensionRequests, 4)).isEqualTo(275); // Novo
		assertThat(pixelSizeForColumn(dimensionRequests, 5)).isEqualTo(140); // Era 90
		assertThat(pixelSizeForColumn(dimensionRequests, 6)).isEqualTo(240); // Novo

		verify(exportService).streamCsvByIds(any(OutputStream.class), eq(ids));
		verify(spreadsheets).get(createdId);
		verify(spreadsheets).batchUpdate(eq(createdId), any(BatchUpdateSpreadsheetRequest.class));
		verifyNoMoreInteractions(exportService, drive, driveFiles, driveFilesCreate, sheets, spreadsheets,
				spreadsheetsGet, spreadsheetsBatchUpdate);
	}

	@Test
	void createSheetFromCsv_deveCriarPlanilhaComNomePersonalizadoSemPastaPai() throws Exception {
		String createdId = "spreadsheet-ABC";
		String webViewLink = "https://view/link2";
		String webContentLink = "https://content/link2";
		File createdFile = new File();
		createdFile.setId(createdId);
		createdFile.setWebViewLink(webViewLink);
		createdFile.setWebContentLink(webContentLink);
		when(driveFilesCreate.execute()).thenReturn(createdFile);

		Spreadsheet spreadsheet = spreadsheetWithSingleSheet(777, 50);
		when(spreadsheetsGet.execute()).thenReturn(spreadsheet);
		when(spreadsheetsBatchUpdate.execute()).thenReturn(new BatchUpdateSpreadsheetResponse());

		doAnswer(invocation -> {
			OutputStream out = invocation.getArgument(0, OutputStream.class);
			out.write("X,Y,Z\n9,8,7\n".getBytes());
			return null;
		}).when(exportService).streamCsvByIds(any(OutputStream.class), anyList());

		List<Long> ids = List.of(1L);
		String nomePlanilha = "Planilha Personalizada";

		GoogleSheetsExport.CreatedSheet result = service.createSheetFromCsv(ids, nomePlanilha, null);

		assertThat(result.id()).isEqualTo(createdId);
		assertThat(result.webViewLink()).isEqualTo(webViewLink);
		assertThat(result.webContentLink()).isEqualTo(webContentLink);

		ArgumentCaptor<File> metaCaptor = ArgumentCaptor.forClass(File.class);
		ArgumentCaptor<InputStreamContent> contentCaptor = ArgumentCaptor.forClass(InputStreamContent.class);
		verify(driveFiles).create(metaCaptor.capture(), contentCaptor.capture());

		File sentMeta = metaCaptor.getValue();
		assertThat(sentMeta.getName()).isEqualTo("Planilha Personalizada");
		assertThat(sentMeta.getParents()).isNull();

		ArgumentCaptor<BatchUpdateSpreadsheetRequest> batchCaptor = ArgumentCaptor
			.forClass(BatchUpdateSpreadsheetRequest.class);
		verify(spreadsheets).batchUpdate(eq(createdId), batchCaptor.capture());
		BatchUpdateSpreadsheetRequest batchRequest = batchCaptor.getValue();

		// ### CORREÇÃO 4: O total de requisições agora é 20 (neste teste também) ###
		assertThat(batchRequest.getRequests()).hasSize(20);

		AddBandingRequest bandingRequest = findAddBandingRequest(batchRequest.getRequests());
		assertThat(bandingRequest.getBandedRange().getRange().getEndRowIndex()).isEqualTo(ids.size() + 1);

		verify(exportService).streamCsvByIds(any(OutputStream.class), eq(ids));
		verify(spreadsheets).get(createdId);
		verify(spreadsheets).batchUpdate(eq(createdId), any(BatchUpdateSpreadsheetRequest.class));
		verifyNoMoreInteractions(exportService, drive, driveFiles, driveFilesCreate, sheets, spreadsheets,
				spreadsheetsGet, spreadsheetsBatchUpdate);
	}

	private static Spreadsheet spreadsheetWithSingleSheet(int sheetId, int rowCount) {
		GridProperties gridProperties = new GridProperties();
		gridProperties.setRowCount(rowCount);
		SheetProperties sheetProperties = new SheetProperties();
		sheetProperties.setSheetId(sheetId);
		sheetProperties.setGridProperties(gridProperties);
		Sheet sheet = new Sheet();
		sheet.setProperties(sheetProperties);
		Spreadsheet spreadsheet = new Spreadsheet();
		spreadsheet.setSheets(new ArrayList<>(List.of(sheet)));
		return spreadsheet;
	}

	private static AddBandingRequest findAddBandingRequest(List<Request> requests) {
		for (Request request : requests) {
			if (request.getAddBanding() != null) {
				return request.getAddBanding();
			}
		}
		return null;
	}

	private static List<UpdateDimensionPropertiesRequest> findDimensionUpdates(List<Request> requests) {
		List<UpdateDimensionPropertiesRequest> result = new ArrayList<>();
		for (Request request : requests) {
			if (request.getUpdateDimensionProperties() != null) {
				result.add(request.getUpdateDimensionProperties());
			}
		}
		return result;
	}

	private static Integer pixelSizeForColumn(List<UpdateDimensionPropertiesRequest> requests, int columnStartIndex) {
		for (UpdateDimensionPropertiesRequest req : requests) {
			DimensionRange range = req.getRange();
			if ("COLUMNS".equals(range.getDimension()) && range.getStartIndex() != null
					&& range.getStartIndex() == columnStartIndex) {
				return req.getProperties().getPixelSize();
			}
		}
		return null;
	}

}