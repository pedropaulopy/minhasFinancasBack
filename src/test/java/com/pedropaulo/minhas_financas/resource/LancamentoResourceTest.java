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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
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
	private LancamentoCsvImportService importService;

	private Authentication authentication;

	private LancamentoResource resource;

	@BeforeEach
	void setUp() {
		resource = new LancamentoResource(lancamentoService, usuarioService, importService);
		authentication = new TestingAuthenticationToken(EMAIL, null);
	}

	private LancamentoDTO criarLancamentoDTOValido() {
		LancamentoDTO lancamentoDTO = new LancamentoDTO();
		lancamentoDTO.setUsuario(1L);
		lancamentoDTO.setDescricao("Salário");
		lancamentoDTO.setValor(BigDecimal.valueOf(5000));
		lancamentoDTO.setMes(10);
		lancamentoDTO.setAno(2025);
		lancamentoDTO.setTipoLancamento("RECEITA");
		lancamentoDTO.setStatusLancamento("PENDENTE");
		return lancamentoDTO;
	}

	private Lancamento criarLancamentoNovo() {
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

	private Usuario criarUsuarioAutenticado() {
		Usuario usuarioAutenticado = new Usuario();
		usuarioAutenticado.setId(1L);
		usuarioAutenticado.setEmail(EMAIL);
		usuarioAutenticado.setNome("Pedro");
		return usuarioAutenticado;
	}

	@Test
	void salvar_deveRetornarCreated_quandoSucesso() throws Exception {
		LancamentoDTO lancamentoDTO = criarLancamentoDTOValido();
		Lancamento lancamentoConvertido = criarLancamentoNovo();
		Lancamento lancamentoSalvo = Lancamento.builder()
			.id(99L)
			.ano(lancamentoConvertido.getAno())
			.mes(lancamentoConvertido.getMes())
			.descricao(lancamentoConvertido.getDescricao())
			.valor(lancamentoConvertido.getValor())
			.tipoLancamento(lancamentoConvertido.getTipoLancamento())
			.statusLancamento(lancamentoConvertido.getStatusLancamento())
			.dataCadastro(lancamentoConvertido.getDataCadastro())
			.build();

		when(lancamentoService.converterDTO(eq(lancamentoDTO), eq(authentication))).thenReturn(lancamentoConvertido);
		when(lancamentoService.salvar(eq(lancamentoConvertido))).thenReturn(lancamentoSalvo);

		ResponseEntity<?> resposta = resource.salvar(lancamentoDTO, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(resposta.getBody()).isEqualTo(lancamentoSalvo);
		verify(lancamentoService).converterDTO(lancamentoDTO, authentication);
		verify(lancamentoService).salvar(lancamentoConvertido);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void salvar_deveRetornarBadRequest_quandoRegraNegocio() throws Exception {
		LancamentoDTO lancamentoDTO = criarLancamentoDTOValido();

		when(lancamentoService.converterDTO(any(LancamentoDTO.class), eq(authentication)))
			.thenReturn(criarLancamentoNovo());
		when(lancamentoService.salvar(any(Lancamento.class)))
			.thenThrow(new RegraNegocioException("Insira um valor válido."));

		ResponseEntity<?> resposta = resource.salvar(lancamentoDTO, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resposta.getBody()).isEqualTo("Insira um valor válido.");
		verify(lancamentoService).converterDTO(any(LancamentoDTO.class), eq(authentication));
		verify(lancamentoService).salvar(any(Lancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizar_deveRetornarOk_quandoSucesso() throws Exception {
		Long lancamentoId = 1L;
		LancamentoDTO lancamentoDTO = criarLancamentoDTOValido();
		Lancamento lancamentoAtualizado = Lancamento.builder()
			.id(lancamentoId)
			.descricao("Atualizado")
			.mes(10)
			.ano(2025)
			.valor(BigDecimal.valueOf(5500))
			.tipoLancamento(TipoLancamento.RECEITA)
			.statusLancamento(StatusLancamento.EFETIVADO)
			.dataCadastro(DATA_FIXA)
			.build();

		when(lancamentoService.atualizar(eq(lancamentoId), eq(authentication), eq(lancamentoDTO)))
			.thenReturn(lancamentoAtualizado);

		ResponseEntity<?> resposta = resource.atualizar(lancamentoId, lancamentoDTO, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getBody()).isEqualTo(lancamentoAtualizado);
		verify(lancamentoService).atualizar(lancamentoId, authentication, lancamentoDTO);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizar_deveRetornarBadRequest_quandoRegraNegocio() throws Exception {
		Long lancamentoId = 7L;
		LancamentoDTO lancamentoDTO = criarLancamentoDTOValido();

		when(lancamentoService.atualizar(eq(lancamentoId), eq(authentication), eq(lancamentoDTO)))
			.thenThrow(new RegraNegocioException("Dados inválidos para atualização."));

		ResponseEntity<?> resposta = resource.atualizar(lancamentoId, lancamentoDTO, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resposta.getBody()).isEqualTo("Dados inválidos para atualização.");
		verify(lancamentoService).atualizar(lancamentoId, authentication, lancamentoDTO);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizar_deveRetornarUnprocessableEntity_quandoEntidadeNaoProcessavel() throws Exception {
		Long lancamentoId = 1L;
		LancamentoDTO lancamentoDTO = criarLancamentoDTOValido();
		String mensagem = "Lançamentos efetivados ou cancelados não podem ser editados.";

		when(lancamentoService.atualizar(eq(lancamentoId), eq(authentication), eq(lancamentoDTO)))
			.thenThrow(new EntidadeNaoProcessavelException(mensagem));

		ResponseEntity<?> resposta = resource.atualizar(lancamentoId, lancamentoDTO, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(resposta.getBody()).isEqualTo(mensagem);
		verify(lancamentoService).atualizar(lancamentoId, authentication, lancamentoDTO);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizarStatus_deveRetornarCreated_quandoSucesso() throws Exception {
		Long lancamentoId = 5L;
		LancamentoStatusDTO statusDTO = new LancamentoStatusDTO();
		statusDTO.setStatus("EFETIVADO");

		doNothing().when(lancamentoService)
			.atualizarStatus(eq(lancamentoId), eq(authentication), eq(StatusLancamento.EFETIVADO));

		ResponseEntity<?> resposta = resource.atualizarStatus(lancamentoId, statusDTO, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		verify(lancamentoService).atualizarStatus(lancamentoId, authentication, StatusLancamento.EFETIVADO);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizarStatus_deveRetornarBadRequest_quandoRegraNegocio() throws Exception {
		Long lancamentoId = 999L;
		LancamentoStatusDTO statusDTO = new LancamentoStatusDTO();
		statusDTO.setStatus("EFETIVADO");

		doThrow(new RegraNegocioException("Lançamento não encontrado.")).when(lancamentoService)
			.atualizarStatus(eq(lancamentoId), eq(authentication), any(StatusLancamento.class));

		ResponseEntity<?> resposta = resource.atualizarStatus(lancamentoId, statusDTO, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resposta.getBody()).isEqualTo("Lançamento não encontrado.");
		verify(lancamentoService).atualizarStatus(eq(lancamentoId), eq(authentication), any(StatusLancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void atualizarStatus_deveRetornarUnprocessableEntity_quandoEntidadeNaoProcessavel() throws Exception {
		Long lancamentoId = 1L;
		LancamentoStatusDTO statusDTO = new LancamentoStatusDTO();
		statusDTO.setStatus("EFETIVADO");
		String mensagemErro = "Lançamentos efetivados ou cancelados não podem ser editados.";

		doThrow(new EntidadeNaoProcessavelException(mensagemErro)).when(lancamentoService)
			.atualizarStatus(eq(lancamentoId), eq(authentication), any(StatusLancamento.class));

		ResponseEntity<?> resposta = resource.atualizarStatus(lancamentoId, statusDTO, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(resposta.getBody()).isEqualTo(mensagemErro);
		verify(lancamentoService).atualizarStatus(eq(lancamentoId), eq(authentication), any(StatusLancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void deletar_deveRetornarNoContent_quandoSucesso() throws Exception {
		Long lancamentoId = 3L;

		doNothing().when(lancamentoService).deletar(eq(lancamentoId), eq(authentication));

		ResponseEntity<?> resposta = resource.deletar(lancamentoId, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(lancamentoService).deletar(lancamentoId, authentication);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void deletar_deveRetornarNotFound_quandoRegraNegocio() throws Exception {
		Long lancamentoId = 44L;

		doThrow(new RegraNegocioException("Lançamento não encontrado.")).when(lancamentoService)
			.deletar(eq(lancamentoId), eq(authentication));

		ResponseEntity<?> resposta = resource.deletar(lancamentoId, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(resposta.getBody()).isEqualTo("Lançamento não encontrado.");
		verify(lancamentoService).deletar(lancamentoId, authentication);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void buscar_deveRetornarOk_comListaVazia() throws Exception {
		Usuario usuarioAutenticado = criarUsuarioAutenticado();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);
		when(lancamentoService.buscar(any(Lancamento.class))).thenReturn(Collections.emptyList());

		ResponseEntity<List<Lancamento>> resposta = resource.buscar("Aluguel", 10, 2025, BigDecimal.valueOf(1200),
				TipoLancamento.DESPESA, StatusLancamento.PENDENTE, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getBody()).isNotNull().isEmpty();
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void buscar_deveRetornarOk_comListaPreenchida() throws Exception {
		Usuario usuarioAutenticado = criarUsuarioAutenticado();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		Lancamento lancamentoEncontrado = new Lancamento();
		lancamentoEncontrado.setId(10L);
		lancamentoEncontrado.setUsuario(usuarioAutenticado);
		lancamentoEncontrado.setDescricao("Aluguel");
		lancamentoEncontrado.setMes(10);
		lancamentoEncontrado.setAno(2025);
		lancamentoEncontrado.setValor(BigDecimal.valueOf(1200));
		lancamentoEncontrado.setTipoLancamento(TipoLancamento.DESPESA);
		lancamentoEncontrado.setStatusLancamento(StatusLancamento.PENDENTE);

		when(lancamentoService.buscar(any(Lancamento.class))).thenReturn(List.of(lancamentoEncontrado));

		ResponseEntity<List<Lancamento>> resposta = resource.buscar("Aluguel", 10, 2025, BigDecimal.valueOf(1200),
				TipoLancamento.DESPESA, StatusLancamento.PENDENTE, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		Assertions.assertThat(resposta.getBody()).isNotNull().hasSize(1).contains(lancamentoEncontrado);
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void obterLancamento_deveRetornarOk_quandoExiste() throws Exception {
		Lancamento lancamento = criarLancamentoNovo();
		lancamento.setId(77L);

		when(lancamentoService.obterPorIdLancamento(eq(77L), eq(authentication))).thenReturn(lancamento);

		ResponseEntity<?> resposta = resource.obterLancamento(77L, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getBody()).isEqualTo(lancamento);
		verify(lancamentoService).obterPorIdLancamento(77L, authentication);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void obterLancamento_deveRetornarNotFound_quandoNaoExiste() throws Exception {
		when(lancamentoService.obterPorIdLancamento(eq(321L), eq(authentication)))
			.thenThrow(new RegraNegocioException("Lançamento não encontrado."));

		ResponseEntity<?> resposta = resource.obterLancamento(321L, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(resposta.getBody()).isEqualTo("Lançamento não encontrado.");
		verify(lancamentoService).obterPorIdLancamento(321L, authentication);
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void importarLancamentos_deveRetornarBadRequest_quandoArquivoNulo() {
		ResponseEntity<?> resposta = resource.importarLancamentos(null, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resposta.getBody()).isNull();

		verifyNoInteractions(usuarioService, importService);
	}

	@Test
	void importarLancamentos_deveRetornarBadRequest_quandoArquivoVazio() {
		MultipartFile arquivoVazio = new MockMultipartFile("file", new byte[0]);

		ResponseEntity<?> resposta = resource.importarLancamentos(arquivoVazio, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resposta.getBody()).isNull();

		verifyNoInteractions(usuarioService, importService);
	}

	@Test
	void importarLancamentos_deveRetornarOk_quandoSemFalhas() throws Exception {
		byte[] conteudoCsv = "qualquer".getBytes();
		MultipartFile arquivoCsv = new MockMultipartFile("file", "dados.csv", "text/csv", conteudoCsv);

		Usuario usuarioAutenticado = criarUsuarioAutenticado();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		ImportResultadoDTO resultadoImportacao = new ImportResultadoDTO();
		when(importService.importar(any(InputStream.class), eq(1000), eq(usuarioAutenticado.getId())))
			.thenReturn(resultadoImportacao);

		ResponseEntity<ImportResultadoDTO> resposta = resource.importarLancamentos(arquivoCsv, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resposta.getBody()).isSameAs(resultadoImportacao);

		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(importService).importar(any(InputStream.class), eq(1000), eq(usuarioAutenticado.getId()));
		verifyNoMoreInteractions(usuarioService, importService);
	}

	@Test
	void importarLancamentos_deveRetornarMultiStatus_quandoHaFalhas() throws Exception {
		byte[] conteudoCsv = "qualquer".getBytes();
		MultipartFile arquivoCsv = new MockMultipartFile("file", "dados.csv", "text/csv", conteudoCsv);

		Usuario usuarioAutenticado = criarUsuarioAutenticado();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);

		ImportResultadoDTO resultadoImportacao = new ImportResultadoDTO();
		resultadoImportacao.addFalha(2, "Linha inválida", "raw"); // força MULTI_STATUS
		when(importService.importar(any(InputStream.class), eq(1000), eq(usuarioAutenticado.getId())))
			.thenReturn(resultadoImportacao);

		ResponseEntity<ImportResultadoDTO> resposta = resource.importarLancamentos(arquivoCsv, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.MULTI_STATUS);
		assertThat(resposta.getBody()).isSameAs(resultadoImportacao);
		assertThat(resposta.getBody().getTotalFalha()).isGreaterThan(0);

		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(importService).importar(any(InputStream.class), eq(1000), eq(usuarioAutenticado.getId()));
		verifyNoMoreInteractions(usuarioService, importService);
	}

	@Test
	void importarLancamentos_deveRetornarInternalServerError_quandoExcecao() throws Exception {
		byte[] conteudoCsv = "qualquer".getBytes();
		MultipartFile arquivoCsv = new MockMultipartFile("file", "dados.csv", "text/csv", conteudoCsv);

		Usuario usuarioAutenticado = criarUsuarioAutenticado();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuarioAutenticado);
		when(importService.importar(any(InputStream.class), eq(1000), eq(usuarioAutenticado.getId())))
			.thenThrow(new RuntimeException("falha inesperada"));

		ResponseEntity<ImportResultadoDTO> resposta = resource.importarLancamentos(arquivoCsv, authentication);

		assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resposta.getBody()).isNotNull();
		assertThat(resposta.getBody().getTotalFalha()).isEqualTo(1);
		assertThat(resposta.getBody().getErros()).isNotEmpty();
		assertThat(resposta.getBody().getErros().get(0).motivo)
			.isEqualTo("Erro ao processar arquivo: falha inesperada");

		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(importService).importar(any(InputStream.class), eq(1000), eq(usuarioAutenticado.getId()));
		verifyNoMoreInteractions(usuarioService, importService);
	}

}
