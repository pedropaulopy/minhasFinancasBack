package com.pedropaulo.minhas_financas.resource;

import com.pedropaulo.minhas_financas.api.resource.LancamentoResource;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LancamentoResourceTestRefactored {

	private static final String EMAIL = "usuario@teste.com";

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

	private Usuario criarUsuario() {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		usuario.setNome("Pedro");
		return usuario;
	}

	@Test
	void export_quandoSemIds_entaoRetornaBadRequest() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(Collections.emptyList());

		ResponseEntity<StreamingResponseBody> resposta = resource.export("csv", null, null, null, null, null, null,
				Collections.emptyList(), authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(usuarioService, lancamentoService);
	}

	@Test
	void export_quandoJsonComSucesso_entaoRetornaOkComStream() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		Lancamento lancamento = new Lancamento();
		lancamento.setId(10L);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(List.of(lancamento));

		doAnswer(invocacao -> {
			OutputStream outputStream = invocacao.getArgument(0, OutputStream.class);
			outputStream.write("[{\"ok\":true}]".getBytes(StandardCharsets.UTF_8));
			return null;
		}).when(exportService).exportarJsonPorIds(any(OutputStream.class), eq(List.of(10L)));

		ResponseEntity<StreamingResponseBody> resposta = resource.export("json", "descricao", 10, 2025, null, null,
				null, Collections.emptyList(), authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getHeaders().getFirst("Content-Disposition"))
			.isEqualTo("attachment; filename=\"lancamentos.json\"");

		ByteArrayOutputStream conteudoGerado = new ByteArrayOutputStream();
		resposta.getBody().writeTo(conteudoGerado);
		assertThat(conteudoGerado.toString(StandardCharsets.UTF_8)).isEqualTo("[{\"ok\":true}]");

		verify(exportService).exportarJsonPorIds(any(OutputStream.class), eq(List.of(10L)));
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(exportService, usuarioService, lancamentoService);
	}

	@Test
	void export_quandoCsvComSucesso_entaoRetornaOkComStream() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		Lancamento lancamento = new Lancamento();
		lancamento.setId(77L);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(List.of(lancamento));

		doAnswer(invocacao -> {
			OutputStream outputStream = invocacao.getArgument(0, OutputStream.class);
			outputStream.write("A,B\n1,2\n".getBytes(StandardCharsets.UTF_8));
			return null;
		}).when(exportService).exportarCsvPorIds(any(OutputStream.class), eq(List.of(77L)));

		ResponseEntity<StreamingResponseBody> resposta = resource.export("csv", "descricao", 1, 2024, null, null, null,
				Collections.emptyList(), authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getHeaders().getFirst("Content-Disposition"))
			.isEqualTo("attachment; filename=\"lancamentos.csv\"");

		ByteArrayOutputStream conteudoGerado = new ByteArrayOutputStream();
		resposta.getBody().writeTo(conteudoGerado);
		assertThat(conteudoGerado.toString(StandardCharsets.UTF_8)).isEqualTo("A,B\n1,2\n");

		verify(exportService).exportarCsvPorIds(any(OutputStream.class), eq(List.of(77L)));
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class), anyList());
		verifyNoMoreInteractions(exportService, usuarioService, lancamentoService);
	}

	@Test
	void export_quandoTipoExportInvalido_entaoAssumeCsvComoPadrao() throws Exception {
		Usuario usuarioAutenticado = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		Lancamento lancamento = new Lancamento();
		lancamento.setId(77L);
		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(List.of(lancamento));

		doAnswer(invocacao -> {
			OutputStream outputStream = invocacao.getArgument(0, OutputStream.class);
			outputStream.write("default_csv".getBytes(StandardCharsets.UTF_8));
			return null;
		}).when(exportService).exportarCsvPorIds(any(OutputStream.class), eq(List.of(77L)));

		ResponseEntity<StreamingResponseBody> resposta = resource.export("invalid_type", null, null, null, null, null,
				null, Collections.emptyList(), authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getHeaders().getFirst("Content-Disposition"))
			.isEqualTo("attachment; filename=\"lancamentos.csv\"");

		ByteArrayOutputStream conteudoGerado = new ByteArrayOutputStream();
		resposta.getBody().writeTo(conteudoGerado);
		assertThat(conteudoGerado.toString(StandardCharsets.UTF_8)).isEqualTo("default_csv");

		verify(exportService).exportarCsvPorIds(any(OutputStream.class), eq(List.of(77L)));
		verifyNoMoreInteractions(exportService);
	}

}
