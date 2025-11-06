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
import com.pedropaulo.minhas_financas.service.LancamentoCsvImportService;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static com.pedropaulo.minhas_financas.service.testUtils.AuthMocks.auth;
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

	private LancamentoResource resource;

	private Authentication authentication;

	@BeforeEach
	void setUp() {
		resource = new LancamentoResource(lancamentoService, usuarioService, lancamentoCsvImportService);
		authentication = auth(EMAIL);
	}

	@Test
	void salvar_deveRetornarCreated_quandoSucesso() throws Exception {
		LancamentoDTO lancamentoDTO = dtoValido();
		Lancamento entidadeLancamento = novoLancamento();
		Lancamento lancamentoSalvo = Lancamento.builder()
			.id(99L)
			.ano(entidadeLancamento.getAno())
			.mes(entidadeLancamento.getMes())
			.descricao(entidadeLancamento.getDescricao())
			.valor(entidadeLancamento.getValor())
			.tipoLancamento(entidadeLancamento.getTipoLancamento())
			.statusLancamento(entidadeLancamento.getStatusLancamento())
			.dataCadastro(entidadeLancamento.getDataCadastro())
			.build();

		when(lancamentoService.converterDTO(eq(lancamentoDTO), eq(authentication))).thenReturn(entidadeLancamento);
		when(lancamentoService.salvar(eq(entidadeLancamento))).thenReturn(lancamentoSalvo);

		ResponseEntity<?> response = resource.salvar(lancamentoDTO, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isEqualTo(lancamentoSalvo);
		verify(lancamentoService).converterDTO(lancamentoDTO, authentication);
		verify(lancamentoService).salvar(entidadeLancamento);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void salvar_deveRetornarBadRequest_quandoRegraNegocio() throws Exception {
		LancamentoDTO lancamentoDTO = dtoValido();

		when(lancamentoService.converterDTO(any(LancamentoDTO.class), eq(authentication))).thenReturn(novoLancamento());
		when(lancamentoService.salvar(any(Lancamento.class)))
			.thenThrow(new RegraNegocioException("Insira um valor válido."));

		ResponseEntity<?> response = resource.salvar(lancamentoDTO, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo("Insira um valor válido.");
		verify(lancamentoService).converterDTO(any(LancamentoDTO.class), eq(authentication));
		verify(lancamentoService).salvar(any(Lancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizar_deveRetornarOk_quandoSucesso() throws Exception {
		Long identificador = 1L;
		LancamentoDTO lancamentoDTO = dtoValido();
		Lancamento lancamentoAtualizado = Lancamento.builder()
			.id(identificador)
			.descricao("Atualizado")
			.mes(10)
			.ano(2025)
			.valor(BigDecimal.valueOf(5500))
			.tipoLancamento(TipoLancamento.RECEITA)
			.statusLancamento(StatusLancamento.EFETIVADO)
			.dataCadastro(DATA_FIXA)
			.build();

		when(lancamentoService.atualizar(eq(identificador), eq(authentication), eq(lancamentoDTO)))
			.thenReturn(lancamentoAtualizado);

		ResponseEntity<?> response = resource.atualizar(identificador, lancamentoDTO, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(lancamentoAtualizado);
		verify(lancamentoService).atualizar(identificador, authentication, lancamentoDTO);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizar_deveRetornarBadRequest_quandoRegraNegocio() throws Exception {
		Long identificador = 7L;
		LancamentoDTO lancamentoDTO = dtoValido();

		when(lancamentoService.atualizar(eq(identificador), eq(authentication), eq(lancamentoDTO)))
			.thenThrow(new RegraNegocioException("Dados inválidos para atualização."));

		ResponseEntity<?> response = resource.atualizar(identificador, lancamentoDTO, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo("Dados inválidos para atualização.");
		verify(lancamentoService).atualizar(identificador, authentication, lancamentoDTO);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizar_deveRetornarUnprocessableEntity_quandoEntidadeNaoProcessavel() throws Exception {
		Long identificador = 1L;
		LancamentoDTO lancamentoDTO = dtoValido();
		String mensagemErro = "Lançamentos efetivados ou cancelados não podem ser editados.";

		when(lancamentoService.atualizar(eq(identificador), eq(authentication), eq(lancamentoDTO)))
			.thenThrow(new EntidadeNaoProcessavelException(mensagemErro));

		ResponseEntity<?> response = resource.atualizar(identificador, lancamentoDTO, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(response.getBody()).isEqualTo(mensagemErro);
		verify(lancamentoService).atualizar(identificador, authentication, lancamentoDTO);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizarStatus_deveRetornarCreated_quandoSucesso() throws Exception {
		Long identificador = 5L;
		LancamentoStatusDTO statusDTO = new LancamentoStatusDTO();
		statusDTO.setStatus("EFETIVADO");

		doNothing().when(lancamentoService)
			.atualizarStatus(eq(identificador), eq(authentication), eq(StatusLancamento.EFETIVADO));

		ResponseEntity<?> response = resource.atualizarStatus(identificador, statusDTO, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		verify(lancamentoService).atualizarStatus(identificador, authentication, StatusLancamento.EFETIVADO);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizarStatus_deveRetornarBadRequest_quandoRegraNegocio() throws Exception {
		Long identificador = 999L;
		LancamentoStatusDTO statusDTO = new LancamentoStatusDTO();
		statusDTO.setStatus("EFETIVADO");

		doThrow(new RegraNegocioException("Lançamento não encontrado.")).when(lancamentoService)
			.atualizarStatus(eq(identificador), eq(authentication), any(StatusLancamento.class));

		ResponseEntity<?> response = resource.atualizarStatus(identificador, statusDTO, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo("Lançamento não encontrado.");
		verify(lancamentoService).atualizarStatus(eq(identificador), eq(authentication), any(StatusLancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizarStatus_deveRetornarUnprocessableEntity_quandoEntidadeNaoProcessavel() throws Exception {
		Long identificador = 1L;
		LancamentoStatusDTO statusDTO = new LancamentoStatusDTO();
		statusDTO.setStatus("EFETIVADO");
		String mensagemErro = "Lançamentos efetivados ou cancelados não podem ser editados.";

		doThrow(new EntidadeNaoProcessavelException(mensagemErro)).when(lancamentoService)
			.atualizarStatus(eq(identificador), eq(authentication), any(StatusLancamento.class));

		ResponseEntity<?> response = resource.atualizarStatus(identificador, statusDTO, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(response.getBody()).isEqualTo(mensagemErro);
		verify(lancamentoService).atualizarStatus(eq(identificador), eq(authentication), any(StatusLancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void deletar_deveRetornarNoContent_quandoSucesso() throws Exception {
		Long identificador = 3L;

		doNothing().when(lancamentoService).deletar(eq(identificador), eq(authentication));

		ResponseEntity<?> response = resource.deletar(identificador, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(lancamentoService).deletar(identificador, authentication);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void deletar_deveRetornarNotFound_quandoRegraNegocio() throws Exception {
		Long identificador = 44L;

		doThrow(new RegraNegocioException("Lançamento não encontrado.")).when(lancamentoService)
			.deletar(eq(identificador), eq(authentication));

		ResponseEntity<?> response = resource.deletar(identificador, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isEqualTo("Lançamento não encontrado.");
		verify(lancamentoService).deletar(identificador, authentication);
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

		Lancamento lancamentoEncontrado = new Lancamento();
		lancamentoEncontrado.setId(10L);
		lancamentoEncontrado.setUsuario(usuario);
		lancamentoEncontrado.setDescricao("Aluguel");
		lancamentoEncontrado.setMes(10);
		lancamentoEncontrado.setAno(2025);
		lancamentoEncontrado.setValor(BigDecimal.valueOf(1200));
		lancamentoEncontrado.setTipoLancamento(TipoLancamento.DESPESA);
		lancamentoEncontrado.setStatusLancamento(StatusLancamento.PENDENTE);

		when(lancamentoService.buscar(any(Lancamento.class), anyList())).thenReturn(List.of(lancamentoEncontrado));

		ResponseEntity<List<Lancamento>> response = resource.buscar("Aluguel", 10, 2025, BigDecimal.valueOf(1200),
				TipoLancamento.DESPESA, StatusLancamento.PENDENTE, Collections.emptyList(), authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Assertions.assertThat(response.getBody()).isNotNull().hasSize(1).contains(lancamentoEncontrado);
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
	void upload_deveRetornarBadRequest_quandoArquivoForNulo() {
		ResponseEntity<ImportResultadoDTO> response = resource.importarLancamentos(null, authentication);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNull();
		verifyNoInteractions(usuarioService, lancamentoCsvImportService);
	}

	@Test
	void upload_deveRetornarBadRequest_quandoArquivoEstiverVazio() throws IOException {
		MultipartFile arquivoVazio = mock(MultipartFile.class);
		when(arquivoVazio.isEmpty()).thenReturn(true);

		ResponseEntity<ImportResultadoDTO> response = resource.importarLancamentos(arquivoVazio, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNull();
		verify(arquivoVazio, never()).getInputStream();
		verifyNoInteractions(usuarioService, lancamentoCsvImportService);
	}

	@Test
	void upload_deveRetornarOK_quandoImportarSemFalhas() throws Exception {
		MockMultipartFile arquivoValido = new MockMultipartFile("file", "dados.csv", "text/csv", "a;b;c".getBytes());

		Usuario usuario = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuario);

		ImportResultadoDTO resultadoImportacao = new ImportResultadoDTO();
		when(lancamentoCsvImportService.importar(any(InputStream.class), eq(usuario.getId())))
			.thenReturn(resultadoImportacao);

		ResponseEntity<ImportResultadoDTO> response = resource.importarLancamentos(arquivoValido, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(resultadoImportacao);
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoCsvImportService).importar(any(InputStream.class), eq(usuario.getId()));
		verifyNoMoreInteractions(usuarioService, lancamentoCsvImportService);
	}

	@Test
	void upload_deveRetornarMultiStatus_quandoHouverFalhasParciais() throws Exception {
		MultipartFile arquivoComFalhas = mock(MultipartFile.class);
		when(arquivoComFalhas.isEmpty()).thenReturn(false);
		when(arquivoComFalhas.getInputStream()).thenReturn(new ByteArrayInputStream("x;y;z".getBytes()));

		Usuario usuario = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuario);

		ImportResultadoDTO resultadoParcial = new ImportResultadoDTO();
		resultadoParcial.addFalha(1, "erro", "raw");
		when(lancamentoCsvImportService.importar(any(InputStream.class), eq(usuario.getId())))
			.thenReturn(resultadoParcial);

		ResponseEntity<ImportResultadoDTO> response = resource.importarLancamentos(arquivoComFalhas, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.MULTI_STATUS);
		assertThat(response.getBody()).isSameAs(resultadoParcial);
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoCsvImportService).importar(any(InputStream.class), eq(usuario.getId()));
		verifyNoMoreInteractions(usuarioService, lancamentoCsvImportService);
	}

	@Test
	void upload_deveRetornarErroInterno_quandoOcorrerExcecaoDuranteProcessamento() throws Exception {
		MultipartFile arquivoLendoFalha = mock(MultipartFile.class);
		when(arquivoLendoFalha.isEmpty()).thenReturn(false);
		when(arquivoLendoFalha.getInputStream()).thenReturn(new ByteArrayInputStream("abc".getBytes()));

		Usuario usuario = criarUsuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuario);

		when(lancamentoCsvImportService.importar(any(InputStream.class), eq(usuario.getId())))
			.thenThrow(new RuntimeException("falha X"));

		ResponseEntity<ImportResultadoDTO> response = resource.importarLancamentos(arquivoLendoFalha, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getTotalFalha()).isGreaterThan(0);
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoCsvImportService).importar(any(InputStream.class), eq(usuario.getId()));
		verifyNoMoreInteractions(usuarioService, lancamentoCsvImportService);
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

}
