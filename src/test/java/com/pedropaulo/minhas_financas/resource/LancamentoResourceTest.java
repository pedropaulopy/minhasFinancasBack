package com.pedropaulo.minhas_financas.resource;

import com.pedropaulo.minhas_financas.api.dto.LancamentoDTO;
import com.pedropaulo.minhas_financas.api.dto.LancamentoStatusDTO;
import com.pedropaulo.minhas_financas.api.dto.importacao.ImportResultadoDTO;
import com.pedropaulo.minhas_financas.api.resource.LancamentoResource;
import com.pedropaulo.minhas_financas.exception.EntidadeNaoProcessavelException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import com.pedropaulo.minhas_financas.service.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class LancamentoResourceTest {

	private static final String EMAIL = "usuario@teste.com";

	private static final LocalDate DATA_FIXA = LocalDate.of(2025, 10, 1);

	@Mock
	private LancamentoService lancamentoService;

	@Mock
	private UsuarioService usuarioService;

	@Mock
	private LancamentoCsvImportService lancamentoCsvImportService;

	@Mock
	private LancamentoExportService exportService;

	@Mock
	private GoogleSheetsExport sheetsExport;

	private LancamentoResource resource;

	private Authentication authentication;

	@BeforeEach
	void setUp() {
		resource = new LancamentoResource(lancamentoService, usuarioService, lancamentoCsvImportService, exportService,
				sheetsExport);
		authentication = new TestingAuthenticationToken(EMAIL, null);
	}

	private LancamentoDTO dtoValido() {
		LancamentoDTO dto = new LancamentoDTO();
		dto.setUsuario(1L);
		dto.setDescricao("Salário");
		dto.setValor(BigDecimal.valueOf(5000));
		dto.setMes(10);
		dto.setAno(2025);
		dto.setTipoLancamento("RECEITA");
		dto.setStatusLancamento("PENDENTE");
		return dto;
	}

	private Lancamento novoLancamento() {
		return Lancamento.builder()
			.ano(2025)
			.mes(10)
			.descricao("Salário")
			.valor(BigDecimal.valueOf(5000))
			.tipoLancamento(TipoLancamento.RECEITA)
			.statusLancamento(StatusLancamento.PENDENTE)
			.dataCadastro(DATA_FIXA)
			.build();
	}

	private Usuario criarUsuario() {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		usuario.setNome("Pedro");
		return usuario;
	}

	@Test
	void salvar_deveRetornarCreated_quandoSucesso() throws Exception {
		LancamentoDTO dto = dtoValido();
		Lancamento entidade = novoLancamento();
		Lancamento salvo = Lancamento.builder()
			.id(99L)
			.ano(entidade.getAno())
			.mes(entidade.getMes())
			.descricao(entidade.getDescricao())
			.valor(entidade.getValor())
			.tipoLancamento(entidade.getTipoLancamento())
			.statusLancamento(entidade.getStatusLancamento())
			.dataCadastro(entidade.getDataCadastro())
			.build();

		when(lancamentoService.converterDTO(eq(dto), eq(authentication))).thenReturn(entidade);
		when(lancamentoService.salvar(eq(entidade))).thenReturn(salvo);

		ResponseEntity<?> response = resource.salvar(dto, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isEqualTo(salvo);
		verify(lancamentoService).converterDTO(dto, authentication);
		verify(lancamentoService).salvar(entidade);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void salvar_deveRetornarBadRequest_quandoRegraNegocio() throws Exception {
		LancamentoDTO dto = dtoValido();

		when(lancamentoService.converterDTO(any(LancamentoDTO.class), eq(authentication))).thenReturn(novoLancamento());
		when(lancamentoService.salvar(any(Lancamento.class)))
			.thenThrow(new RegraNegocioException("Insira um valor válido."));

		ResponseEntity<?> response = resource.salvar(dto, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo("Insira um valor válido.");
		verify(lancamentoService).converterDTO(any(LancamentoDTO.class), eq(authentication));
		verify(lancamentoService).salvar(any(Lancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizar_deveRetornarOk_quandoSucesso() throws Exception {
		Long id = 1L;
		LancamentoDTO dto = dtoValido();
		Lancamento atualizado = Lancamento.builder()
			.id(id)
			.descricao("Atualizado")
			.mes(10)
			.ano(2025)
			.valor(BigDecimal.valueOf(5500))
			.tipoLancamento(TipoLancamento.RECEITA)
			.statusLancamento(StatusLancamento.EFETIVADO)
			.dataCadastro(DATA_FIXA)
			.build();

		when(lancamentoService.atualizar(eq(id), eq(authentication), eq(dto))).thenReturn(atualizado);

		ResponseEntity<?> response = resource.atualizar(id, dto, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(atualizado);
		verify(lancamentoService).atualizar(id, authentication, dto);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizar_deveRetornarBadRequest_quandoRegraNegocio() throws Exception {
		Long id = 7L;
		LancamentoDTO dto = dtoValido();

		when(lancamentoService.atualizar(eq(id), eq(authentication), eq(dto)))
			.thenThrow(new RegraNegocioException("Dados inválidos para atualização."));

		ResponseEntity<?> response = resource.atualizar(id, dto, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo("Dados inválidos para atualização.");
		verify(lancamentoService).atualizar(id, authentication, dto);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizar_deveRetornarUnprocessableEntity_quandoEntidadeNaoProcessavel() throws Exception {
		Long id = 1L;
		LancamentoDTO dto = dtoValido();
		String mensagem = "Lançamentos efetivados ou cancelados não podem ser editados.";

		when(lancamentoService.atualizar(eq(id), eq(authentication), eq(dto)))
			.thenThrow(new EntidadeNaoProcessavelException(mensagem));

		ResponseEntity<?> response = resource.atualizar(id, dto, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(response.getBody()).isEqualTo(mensagem);
		verify(lancamentoService).atualizar(id, authentication, dto);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizarStatus_deveRetornarCreated_quandoSucesso() throws Exception {
		Long id = 5L;
		LancamentoStatusDTO dto = new LancamentoStatusDTO();
		dto.setStatus("EFETIVADO");

		doNothing().when(lancamentoService).atualizarStatus(eq(id), eq(authentication), eq(StatusLancamento.EFETIVADO));

		ResponseEntity<?> response = resource.atualizarStatus(id, dto, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		verify(lancamentoService).atualizarStatus(id, authentication, StatusLancamento.EFETIVADO);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizarStatus_deveRetornarBadRequest_quandoRegraNegocio() throws Exception {
		Long id = 999L;
		LancamentoStatusDTO dto = new LancamentoStatusDTO();
		dto.setStatus("EFETIVADO");

		doThrow(new RegraNegocioException("Lançamento não encontrado.")).when(lancamentoService)
			.atualizarStatus(eq(id), eq(authentication), any(StatusLancamento.class));

		ResponseEntity<?> response = resource.atualizarStatus(id, dto, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo("Lançamento não encontrado.");
		verify(lancamentoService).atualizarStatus(eq(id), eq(authentication), any(StatusLancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizarStatus_deveRetornarUnprocessableEntity_quandoEntidadeNaoProcessavel() throws Exception {
		Long id = 1L;
		LancamentoStatusDTO dto = new LancamentoStatusDTO();
		dto.setStatus("EFETIVADO");
		String msg = "Lançamentos efetivados ou cancelados não podem ser editados.";

		doThrow(new EntidadeNaoProcessavelException(msg)).when(lancamentoService)
			.atualizarStatus(eq(id), eq(authentication), any(StatusLancamento.class));

		ResponseEntity<?> response = resource.atualizarStatus(id, dto, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(response.getBody()).isEqualTo(msg);
		verify(lancamentoService).atualizarStatus(eq(id), eq(authentication), any(StatusLancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void deletar_deveRetornarNoContent_quandoSucesso() throws Exception {
		Long id = 3L;

		doNothing().when(lancamentoService).deletar(eq(id), eq(authentication));

		ResponseEntity<?> response = resource.deletar(id, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(lancamentoService).deletar(id, authentication);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void deletar_deveRetornarNotFound_quandoRegraNegocio() throws Exception {
		Long id = 44L;

		doThrow(new RegraNegocioException("Lançamento não encontrado.")).when(lancamentoService)
			.deletar(eq(id), eq(authentication));

		ResponseEntity<?> response = resource.deletar(id, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isEqualTo("Lançamento não encontrado.");
		verify(lancamentoService).deletar(id, authentication);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void buscar_deveRetornarOk_comListaVazia() throws Exception {
		Usuario usuario = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuario);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(Collections.emptyList());

		ResponseEntity<List<Lancamento>> response = resource.buscar("Aluguel", 10, 2025, BigDecimal.valueOf(1200),
				TipoLancamento.DESPESA, StatusLancamento.PENDENTE, Collections.emptyList(), authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull().isEmpty();
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void buscar_deveRetornarOk_comListaPreenchida() throws Exception {
		Usuario usuario = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuario);

		Lancamento lancamento = new Lancamento();
		lancamento.setId(10L);
		lancamento.setUsuario(usuario);
		lancamento.setDescricao("Aluguel");
		lancamento.setMes(10);
		lancamento.setAno(2025);
		lancamento.setValor(BigDecimal.valueOf(1200));
		lancamento.setTipoLancamento(TipoLancamento.DESPESA);
		lancamento.setStatusLancamento(StatusLancamento.PENDENTE);

		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(List.of(lancamento));

		ResponseEntity<List<Lancamento>> response = resource.buscar("Aluguel", 10, 2025, BigDecimal.valueOf(1200),
				TipoLancamento.DESPESA, StatusLancamento.PENDENTE, Collections.emptyList(), authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Assertions.assertThat(response.getBody()).isNotNull().hasSize(1).contains(lancamento);
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void obterLancamento_deveRetornarOk_quandoExiste() throws Exception {
		Lancamento lancamento = novoLancamento();
		lancamento.setId(77L);

		when(lancamentoService.obterPorIdLancamento(eq(77L), eq(authentication))).thenReturn(lancamento);

		ResponseEntity<?> response = resource.obterLancamento(77L, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(lancamento);
		verify(lancamentoService).obterPorIdLancamento(77L, authentication);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void obterLancamento_deveRetornarNotFound_quandoNaoExiste() throws Exception {
		when(lancamentoService.obterPorIdLancamento(eq(321L), eq(authentication)))
			.thenThrow(new RegraNegocioException("Lançamento não encontrado."));

		ResponseEntity<?> response = resource.obterLancamento(321L, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isEqualTo("Lançamento não encontrado.");
		verify(lancamentoService).obterPorIdLancamento(321L, authentication);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void importarLancamentos_quandoArquivoNulo_entaoRetornaBadRequest() {
		ResponseEntity<ImportResultadoDTO> resposta = resource.importarLancamentos(null, authentication);
		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resposta.getBody()).isNull();
		verifyNoInteractions(lancamentoCsvImportService);
	}

	@Test
	void importarLancamentos_quandoArquivoVazio_entaoRetornaBadRequest() throws Exception {
		MultipartFile arquivoVazio = new org.springframework.mock.web.MockMultipartFile("file", new byte[0]);
		ResponseEntity<ImportResultadoDTO> resposta = resource.importarLancamentos(arquivoVazio, authentication);
		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resposta.getBody()).isNull();
		verifyNoInteractions(lancamentoCsvImportService);
	}

	@Test
	void importarLancamentos_quandoSucessoSemFalhas_entaoRetornaOk() throws Exception {
		byte[] conteudoArquivo = "qualquer".getBytes();
		MultipartFile arquivoCsv = new org.springframework.mock.web.MockMultipartFile("file", "dados.csv", "text/csv",
				conteudoArquivo);

		Usuario usuarioAutenticado = new Usuario();
		usuarioAutenticado.setId(42L);
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		ImportResultadoDTO resultadoImportacao = new ImportResultadoDTO();
		when(lancamentoCsvImportService.importar(any(InputStream.class), eq(1000), eq(42L)))
			.thenReturn(resultadoImportacao);

		ResponseEntity<ImportResultadoDTO> resposta = resource.importarLancamentos(arquivoCsv, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getBody()).isSameAs(resultadoImportacao);
		verify(lancamentoCsvImportService).importar(any(InputStream.class), eq(1000), eq(42L));
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verifyNoMoreInteractions(lancamentoCsvImportService, usuarioService);
	}

	@Test
	void importarLancamentos_quandoHouverFalhas_entaoRetornaMultiStatus() throws Exception {
		byte[] conteudoArquivo = "conteudo".getBytes();
		MultipartFile arquivoCsv = new org.springframework.mock.web.MockMultipartFile("file", "dados.csv", "text/csv",
				conteudoArquivo);

		Usuario usuarioAutenticado = new Usuario();
		usuarioAutenticado.setId(7L);
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		ImportResultadoDTO resultadoParcial = new ImportResultadoDTO();
		resultadoParcial.addFalha(1, "linha inválida", "erro de formato");
		when(lancamentoCsvImportService.importar(any(InputStream.class), eq(1000), eq(7L)))
			.thenReturn(resultadoParcial);

		ResponseEntity<ImportResultadoDTO> resposta = resource.importarLancamentos(arquivoCsv, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.MULTI_STATUS);
		assertThat(resposta.getBody()).isSameAs(resultadoParcial);
		verify(lancamentoCsvImportService).importar(any(InputStream.class), eq(1000), eq(7L));
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verifyNoMoreInteractions(lancamentoCsvImportService, usuarioService);
	}

	@Test
	void importarLancamentos_quandoExcecao_entaoRetornaInternalServerError() throws Exception {
		byte[] conteudoArquivo = "conteudo".getBytes();
		MultipartFile arquivoCsv = new org.springframework.mock.web.MockMultipartFile("file", "dados.csv", "text/csv",
				conteudoArquivo);

		Usuario usuarioAutenticado = new Usuario();
		usuarioAutenticado.setId(99L);
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);
		when(lancamentoCsvImportService.importar(any(InputStream.class), eq(1000), eq(99L)))
			.thenThrow(new RuntimeException("falha X"));

		ResponseEntity<ImportResultadoDTO> resposta = resource.importarLancamentos(arquivoCsv, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resposta.getBody()).isNotNull();
		assertThat(resposta.getBody().getErros()).isNotEmpty();
		assertThat(resposta.getBody().getErros().get(0).getMotivo()).contains("Erro ao processar arquivo: falha X");
		verify(lancamentoCsvImportService).importar(any(InputStream.class), eq(1000), eq(99L));
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verifyNoMoreInteractions(lancamentoCsvImportService, usuarioService);
	}

	@Test
	void exportJson_quandoSemIds_entaoRetornaBadRequest() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(Collections.emptyList());

		ResponseEntity<StreamingResponseBody> resposta = resource.exportJson(null, null, null, null, null, null,
				Collections.emptyList(), authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(usuarioService, lancamentoService);
		verifyNoInteractions(exportService);
	}

	@Test
	void exportJson_quandoSucesso_entaoRetornaOkComStream() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		Lancamento lancamento = new Lancamento();
		lancamento.setId(10L);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(List.of(lancamento));

		doAnswer(invocacao -> {
			OutputStream outputStream = invocacao.getArgument(0, OutputStream.class);
			outputStream.write("[{\"ok\":true}]".getBytes(StandardCharsets.UTF_8));
			return null;
		}).when(exportService).streamJsonByIds(any(OutputStream.class), eq(List.of(10L)));

		ResponseEntity<StreamingResponseBody> resposta = resource.exportJson("descricao", 10, 2025, null,
				TipoLancamento.RECEITA, StatusLancamento.PENDENTE, Collections.emptyList(), authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getHeaders().getFirst("Content-Disposition")).contains("lancamentos_JSON.json");
		assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

		ByteArrayOutputStream conteudoGerado = new ByteArrayOutputStream();
		resposta.getBody().writeTo(conteudoGerado);
		assertThat(conteudoGerado.toString(StandardCharsets.UTF_8)).isEqualTo("[{\"ok\":true}]");

		verify(exportService).streamJsonByIds(any(OutputStream.class), eq(List.of(10L)));
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(exportService, usuarioService, lancamentoService);
	}

	@Test
	void exportCsv_quandoSemIds_entaoRetornaBadRequest() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(Collections.emptyList());

		ResponseEntity<StreamingResponseBody> resposta = resource.exportCsv(null, null, null, null, null, null,
				Collections.emptyList(), authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(usuarioService, lancamentoService);
		verifyNoInteractions(exportService);
	}

	@Test
	void exportCsv_quandoSucesso_entaoRetornaOkComStream() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		Lancamento lancamento = new Lancamento();
		lancamento.setId(77L);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(List.of(lancamento));

		doAnswer(invocacao -> {
			OutputStream outputStream = invocacao.getArgument(0, OutputStream.class);
			outputStream.write("A,B\n1,2\n".getBytes(StandardCharsets.UTF_8));
			return null;
		}).when(exportService).streamCsvByIds(any(OutputStream.class), eq(List.of(77L)));

		ResponseEntity<StreamingResponseBody> resposta = resource.exportCsv("descricao", 1, 2024, null, null, null,
				Collections.emptyList(), authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getHeaders().getFirst("Content-Disposition")).contains("lancamentos_CSV.csv");
		assertThat(resposta.getHeaders().getContentType().toString()).isEqualTo("text/csv;charset=UTF-8");

		ByteArrayOutputStream conteudoGerado = new ByteArrayOutputStream();
		resposta.getBody().writeTo(conteudoGerado);
		assertThat(conteudoGerado.toString(StandardCharsets.UTF_8)).isEqualTo("A,B\n1,2\n");

		verify(exportService).streamCsvByIds(any(OutputStream.class), eq(List.of(77L)));
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(exportService, usuarioService, lancamentoService);
	}

	@Test
	void exportToGoogleSheets_quandoSucesso_entaoRetornaOk() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		Lancamento lancamento = new Lancamento();
		lancamento.setId(5L);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(List.of(lancamento));

		GoogleSheetsExport.CreatedSheet planilhaCriada = new GoogleSheetsExport.CreatedSheet("ID1", "VIEW", "CONTENT");
		when(sheetsExport.createSheetFromCsv(eq(List.of(5L)), eq("Nome"), eq("Pasta"))).thenReturn(planilhaCriada);

		ResponseEntity<?> resposta = resource.exportToGoogleSheets("descricao", 2, 2023, null, null, null,
				Collections.emptyList(), "Nome", "Pasta", authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getBody()).isNotNull();

		verify(sheetsExport).createSheetFromCsv(eq(List.of(5L)), eq("Nome"), eq("Pasta"));
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(sheetsExport, usuarioService, lancamentoService);
	}

	@Test
	void exportToGoogleSheets_quandoFalha_entaoRetornaInternalServerError() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		Lancamento lancamento = new Lancamento();
		lancamento.setId(9L);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(List.of(lancamento));

		when(sheetsExport.createSheetFromCsv(eq(List.of(9L)), any(), any()))
			.thenThrow(new RuntimeException("falha sheets"));

		ResponseEntity<?> resposta = resource.exportToGoogleSheets(null, null, null, null, null, null,
				Collections.emptyList(), null, null, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resposta.getBody()).isNotNull();

		verify(sheetsExport).createSheetFromCsv(eq(List.of(9L)), isNull(), isNull());
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(sheetsExport, usuarioService, lancamentoService);
	}

}
