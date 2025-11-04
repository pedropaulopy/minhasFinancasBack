package com.pedropaulo.minhas_financas.resource;

import com.pedropaulo.minhas_financas.api.dto.LancamentoDTO;
import com.pedropaulo.minhas_financas.service.LancamentoCsvImportService;
import com.pedropaulo.minhas_financas.api.dto.LancamentoStatusDTO;
import com.pedropaulo.minhas_financas.api.resource.LancamentoResource;
import com.pedropaulo.minhas_financas.exception.EntidadeNaoProcessavelException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
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

	private Usuario usuario() {
		Usuario u = new Usuario();
		u.setId(1L);
		u.setEmail(EMAIL);
		u.setNome("Pedro");
		return u;
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
		Usuario u = usuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(u);
		when(lancamentoService.buscar(any(Lancamento.class))).thenReturn(Collections.emptyList());

		ResponseEntity<List<Lancamento>> response = resource.buscar("Aluguel", 10, 2025, BigDecimal.valueOf(1200),
				TipoLancamento.DESPESA, StatusLancamento.PENDENTE, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull().isEmpty();
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void buscar_deveRetornarOk_comListaPreenchida() throws Exception {
		Usuario u = usuario();
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(u);

		Lancamento l = new Lancamento();
		l.setId(10L);
		l.setUsuario(u);
		l.setDescricao("Aluguel");
		l.setMes(10);
		l.setAno(2025);
		l.setValor(BigDecimal.valueOf(1200));
		l.setTipoLancamento(TipoLancamento.DESPESA);
		l.setStatusLancamento(StatusLancamento.PENDENTE);

		when(lancamentoService.buscar(any(Lancamento.class))).thenReturn(List.of(l));

		ResponseEntity<List<Lancamento>> response = resource.buscar("Aluguel", 10, 2025, BigDecimal.valueOf(1200),
				TipoLancamento.DESPESA, StatusLancamento.PENDENTE, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Assertions.assertThat(response.getBody()).isNotNull().hasSize(1).contains(l);
		verify(usuarioService).obterIdUsuarioPorEmail(EMAIL);
		verify(lancamentoService).buscar(any(Lancamento.class));
		verifyNoMoreInteractions(lancamentoService, usuarioService);
	}

	@Test
	void obterLancamento_deveRetornarOk_quandoExiste() throws Exception {
		Lancamento l = novoLancamento();
		l.setId(77L);

		when(lancamentoService.obterPorIdLancamento(eq(77L), eq(authentication))).thenReturn(l);

		ResponseEntity<?> response = resource.obterLancamento(77L, authentication);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(l);
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

}
