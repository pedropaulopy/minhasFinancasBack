package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.api.dto.LancamentoDTO;
import com.pedropaulo.minhas_financas.api.dto.LancamentoDTOFactory;
import com.pedropaulo.minhas_financas.exception.EntidadeNaoProcessavelException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import com.pedropaulo.minhas_financas.service.impl.LancamentoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Example;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static com.pedropaulo.minhas_financas.service.testUtils.AuthMocks.auth;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class LancamentoServiceTest {

	@Mock
	LancamentoRepository repository;

	@Mock
	UsuarioService usuarioService;

	@Mock
	CategoriaService categoriaService;

	LancamentoServiceImpl service;

	private Authentication authentication;

	@BeforeEach
	void setUp() {
		service = new LancamentoServiceImpl(repository, usuarioService, categoriaService);
	}

	private Usuario criarUsuario(Long id, String email) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setEmail(email);
		return usuario;
	}

	private Lancamento lancamentoValido(Usuario usuario) {
		return Lancamento.builder()
			.ano(2025)
			.mes(11)
			.descricao("Lançamento teste")
			.valor(BigDecimal.valueOf(100))
			.tipoLancamento(TipoLancamento.DESPESA)
			.statusLancamento(StatusLancamento.PENDENTE)
			.dataCadastro(LocalDate.now())
			.usuario(usuario)
			.build();
	}

	@Test
	void deveAtualizarUmLancamento() throws Exception {
		Long lancamentoId = 1L;
		String email = "usuario@teste.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(10L, email);

		Lancamento existente = lancamentoValido(usuario);
		existente.setId(lancamentoId);

		LancamentoDTO dto = LancamentoDTOFactory.create(lancamentoId, "Lançamento teste", 11, 2025,
				BigDecimal.valueOf(100), TipoLancamento.DESPESA.name(), StatusLancamento.PENDENTE.name());
		dto.setCategorias(Collections.emptyList());

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(repository.findLancamentoByIdAndUsuarioId(lancamentoId, usuario.getId()))
			.willReturn(Optional.of(existente));
		given(repository.save(existente)).willReturn(existente);

		Lancamento atualizado = service.atualizar(lancamentoId, authentication, dto);

		then(repository).should().save(existente);
		assertThat(atualizado).isEqualTo(existente);
	}

	@Test
	void deveSalvarLancamentoComStatusPendente() throws Exception {
		String email = "user@teste.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(1L, email);
		Lancamento novoLancamento = lancamentoValido(usuario);
		novoLancamento.setStatusLancamento(null);

		given(repository.save(any(Lancamento.class))).willAnswer(inv -> inv.getArgument(0));

		Lancamento salvo = service.salvar(novoLancamento);

		assertThat(salvo.getStatusLancamento()).isEqualTo(StatusLancamento.PENDENTE);
		then(repository).should().save(novoLancamento);
	}

	@Test
	void deveDeletarLancamentoQuandoPendente() throws Exception {
		String email = "user@teste.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(2L, email);
		Long lancamentoId = 55L;

		Lancamento lancamentoPendente = lancamentoValido(usuario);
		lancamentoPendente.setId(lancamentoId);
		lancamentoPendente.setStatusLancamento(StatusLancamento.PENDENTE);

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(repository.findLancamentoByIdAndUsuarioId(lancamentoId, usuario.getId()))
			.willReturn(Optional.of(lancamentoPendente));

		service.deletar(lancamentoId, authentication);

		then(repository).should().delete(lancamentoPendente);
	}

	@Test
	void deveBuscarComTodosFiltrosEComCategorias() {
		Lancamento filtro = new Lancamento();
		Usuario usuarioFiltro = new Usuario();
		usuarioFiltro.setId(9L);
		filtro.setUsuario(usuarioFiltro);
		filtro.setDescricao("mercado");
		filtro.setMes(10);
		filtro.setAno(2024);
		filtro.setValor(BigDecimal.TEN);
		filtro.setTipoLancamento(TipoLancamento.RECEITA);
		filtro.setStatusLancamento(StatusLancamento.EFETIVADO);
		List<Long> categoriaIds = Arrays.asList(1L, 2L);

		List<Lancamento> listaEsperada = Collections.singletonList(new Lancamento());
		given(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
			.willReturn(listaEsperada);

		List<Lancamento> resultado = service.buscar(filtro, categoriaIds);

		assertThat(resultado).isEqualTo(listaEsperada);
		then(repository).should().findAll(any(org.springframework.data.jpa.domain.Specification.class));
	}

	@Test
	void deveAtualizarStatusQuandoValido() throws Exception {
		String email = "user@teste.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(3L, email);
		Long lancamentoId = 77L;

		Lancamento lancamentoPendente = lancamentoValido(usuario);
		lancamentoPendente.setId(lancamentoId);
		lancamentoPendente.setStatusLancamento(StatusLancamento.PENDENTE);

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(repository.findLancamentoByIdAndUsuarioId(lancamentoId, usuario.getId()))
			.willReturn(Optional.of(lancamentoPendente));

		service.atualizarStatus(lancamentoId, authentication, StatusLancamento.EFETIVADO);

		assertThat(lancamentoPendente.getStatusLancamento()).isEqualTo(StatusLancamento.EFETIVADO);
	}

	@Test
	void deveLancarErroAoAtualizarStatusComNulo() throws Exception {
		String email = "user@teste.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(4L, email);
		Long lancamentoId = 88L;

		Lancamento lancamentoPendente = lancamentoValido(usuario);
		lancamentoPendente.setId(lancamentoId);

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(repository.findLancamentoByIdAndUsuarioId(lancamentoId, usuario.getId()))
			.willReturn(Optional.of(lancamentoPendente));

		assertThatThrownBy(() -> service.atualizarStatus(lancamentoId, authentication, null))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("status válido");
	}

	@Test
	void validarDeveRejeitarDescricaoInvalida() {
		Lancamento lancamento = new Lancamento();
		lancamento.setDescricao(" ");
		lancamento.setMes(1);
		lancamento.setAno(2025);
		lancamento.setValor(BigDecimal.ONE);
		lancamento.setTipoLancamento(TipoLancamento.RECEITA);
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		lancamento.setUsuario(usuario);

		assertThatThrownBy(() -> service.validar(lancamento)).isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void validarDeveRejeitarMesInvalido() {
		Lancamento lancamento = new Lancamento();
		lancamento.setDescricao("ok");
		lancamento.setMes(13);
		lancamento.setAno(2025);
		lancamento.setValor(BigDecimal.ONE);
		lancamento.setTipoLancamento(TipoLancamento.RECEITA);
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		lancamento.setUsuario(usuario);

		assertThatThrownBy(() -> service.validar(lancamento)).isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void validarDeveRejeitarAnoInvalido() {
		Lancamento lancamento = new Lancamento();
		lancamento.setDescricao("ok");
		lancamento.setMes(1);
		lancamento.setAno(123);
		lancamento.setValor(BigDecimal.ONE);
		lancamento.setTipoLancamento(TipoLancamento.RECEITA);
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		lancamento.setUsuario(usuario);

		assertThatThrownBy(() -> service.validar(lancamento)).isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void validarDeveRejeitarValorInvalido() {
		Lancamento lancamento = new Lancamento();
		lancamento.setDescricao("ok");
		lancamento.setMes(1);
		lancamento.setAno(2025);
		lancamento.setValor(BigDecimal.ZERO);
		lancamento.setTipoLancamento(TipoLancamento.RECEITA);
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		lancamento.setUsuario(usuario);

		assertThatThrownBy(() -> service.validar(lancamento)).isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void validarDeveRejeitarTipoNulo() {
		Lancamento lancamento = new Lancamento();
		lancamento.setDescricao("ok");
		lancamento.setMes(1);
		lancamento.setAno(2025);
		lancamento.setValor(BigDecimal.ONE);
		lancamento.setTipoLancamento(null);
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		lancamento.setUsuario(usuario);

		assertThatThrownBy(() -> service.validar(lancamento)).isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void validarDeveRejeitarUsuarioInvalido() {
		Lancamento lancamento = new Lancamento();
		lancamento.setDescricao("ok");
		lancamento.setMes(1);
		lancamento.setAno(2025);
		lancamento.setValor(BigDecimal.ONE);
		lancamento.setTipoLancamento(TipoLancamento.RECEITA);
		lancamento.setUsuario(new Usuario());

		assertThatThrownBy(() -> service.validar(lancamento)).isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void obterPorIdLancamentoDeveRetornarQuandoEncontrado() throws Exception {
		String email = "x@x.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(50L, email);
		Long lancamentoId = 5L;
		Lancamento lancamentoEsperado = lancamentoValido(usuario);
		lancamentoEsperado.setId(lancamentoId);

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(repository.findLancamentoByIdAndUsuarioId(lancamentoId, usuario.getId()))
			.willReturn(Optional.of(lancamentoEsperado));

		Lancamento lancamentoObtido = service.obterPorIdLancamento(lancamentoId, authentication);

		assertThat(lancamentoObtido).isEqualTo(lancamentoEsperado);
	}

	@Test
	void obterPorIdLancamentoDeveLancarQuandoNaoEncontrado() throws RegraNegocioException {
		String email = "y@y.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(60L, email);
		Long lancamentoId = 6L;

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(repository.findLancamentoByIdAndUsuarioId(lancamentoId, usuario.getId())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.obterPorIdLancamento(lancamentoId, authentication))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessageContaining("Lançamento não encontrado");
	}

	@Test
	void obterSaldoPorUsuarioDeveSubtrairReceitasEDespesas() throws Exception {
		Long usuarioId = 10L;

		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(eq(usuarioId), eq(TipoLancamento.RECEITA),
				eq(StatusLancamento.EFETIVADO)))
			.willReturn(new BigDecimal("150.00"));
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(eq(usuarioId), eq(TipoLancamento.DESPESA),
				eq(StatusLancamento.EFETIVADO)))
			.willReturn(new BigDecimal("50.00"));

		BigDecimal saldoCalculado = service.obterSaldoPorUsuario(usuarioId);

		assertThat(saldoCalculado).isEqualByComparingTo("100.00");
	}

	@Test
	void obterSaldoPorUsuarioDeveTratarNulosComoZero() throws Exception {
		Long usuarioId = 11L;

		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(eq(usuarioId), eq(TipoLancamento.RECEITA),
				eq(StatusLancamento.EFETIVADO)))
			.willReturn(null);
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(eq(usuarioId), eq(TipoLancamento.DESPESA),
				eq(StatusLancamento.EFETIVADO)))
			.willReturn(null);

		BigDecimal saldoCalculado = service.obterSaldoPorUsuario(usuarioId);

		assertThat(saldoCalculado).isEqualByComparingTo("0.00");
	}

	@Test
	void converterDTODevePopularLancamentoComCategoriasResolvidas() throws Exception {
		String email = "user@teste.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(99L, email);

		LancamentoDTO dto = LancamentoDTOFactory.create(null, "Desc", 9, 2025, new BigDecimal("123.45"),
				TipoLancamento.RECEITA.name(), StatusLancamento.PENDENTE.name());
		dto.setCategorias(Arrays.asList("Mercado", "Transporte"));

		Categoria categoriaMercado = new Categoria();
		categoriaMercado.setId(1L);
		categoriaMercado.setNome("Mercado");
		Categoria categoriaTransporte = new Categoria();
		categoriaTransporte.setId(2L);
		categoriaTransporte.setNome("Transporte");

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(categoriaService.buscarPorNome(any(Categoria.class))).willAnswer(inv -> {
			Categoria filtro = inv.getArgument(0);
			if ("Mercado".equalsIgnoreCase(filtro.getNome()))
				return Collections.singletonList(categoriaMercado);
			if ("Transporte".equalsIgnoreCase(filtro.getNome()))
				return Collections.singletonList(categoriaTransporte);
			return Collections.emptyList();
		});

		Lancamento convertido = service.converterDTO(dto, authentication);

		assertThat(convertido.getDescricao()).isEqualTo("Desc");
		assertThat(convertido.getMes()).isEqualTo(9);
		assertThat(convertido.getAno()).isEqualTo(2025);
		assertThat(convertido.getValor()).isEqualByComparingTo("123.45");
		assertThat(convertido.getUsuario().getId()).isEqualTo(99L);
		assertThat(convertido.getTipoLancamento()).isEqualTo(TipoLancamento.RECEITA);
		assertThat(convertido.getStatusLancamento()).isEqualTo(StatusLancamento.PENDENTE);
		assertThat(convertido.getCategorias()).extracting("nome").containsExactlyInAnyOrder("Mercado", "Transporte");
	}

	@Test
	void validarStatusLancamentoDeveLancarQuandoNaoPendente() throws Exception {
		String email = "user@teste.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(70L, email);
		Long lancamentoId = 700L;

		Lancamento lancamentoEfetivado = lancamentoValido(usuario);
		lancamentoEfetivado.setId(lancamentoId);
		lancamentoEfetivado.setStatusLancamento(StatusLancamento.EFETIVADO);

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(repository.findLancamentoByIdAndUsuarioId(lancamentoId, usuario.getId()))
			.willReturn(Optional.of(lancamentoEfetivado));

		assertThatThrownBy(() -> service.validarStatusLancamento(lancamentoId, authentication))
			.isInstanceOf(EntidadeNaoProcessavelException.class);
	}

	@Test
	void resolverCategoriasDoUsuarioDeveRetornarVazioParaEntradaNulaOuVazia() throws Exception {
		String email = "user@teste.com";
		authentication = auth(email);

		Set<Categoria> categoriasQuandoNulo = service.resolverCategoriasDoUsuario(null, authentication);
		Set<Categoria> categoriasQuandoVazio = service.resolverCategoriasDoUsuario(Collections.emptyList(),
				authentication);

		assertThat(categoriasQuandoNulo).isEmpty();
		assertThat(categoriasQuandoVazio).isEmpty();
	}

	@Test
	void resolverCategoriasDoUsuarioDeveCriarQuandoNaoEncontrar() throws Exception {
		String email = "user@teste.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(80L, email);

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(categoriaService.buscarPorNome(any(Categoria.class))).willReturn(Collections.emptyList());

		Categoria categoriaCriada = new Categoria();
		categoriaCriada.setId(10L);
		categoriaCriada.setNome("Nova");
		given(categoriaService.salvar(any(CategoriaDTO.class), eq(authentication))).willReturn(categoriaCriada);

		Set<Categoria> categoriasResolvidas = service.resolverCategoriasDoUsuario(Collections.singletonList("Nova"),
				authentication);

		assertThat(categoriasResolvidas).extracting("nome").containsExactly("Nova");
		then(categoriaService).should().salvar(any(CategoriaDTO.class), eq(authentication));
	}

	@Test
	void salvarTodosDeveChamarSaveAllEFlush() {
		List<Lancamento> lote = Arrays.asList(new Lancamento(), new Lancamento());

		service.salvarTodos(lote);

		then(repository).should().saveAll(lote);
		then(repository).should().flush();
	}

	@Test
	void resolverCategoriasDoUsuarioDeveIgnorarNomesEmBranco() throws Exception {
		String email = "user@teste.com";
		authentication = auth(email);
		Usuario usuario = criarUsuario(90L, email);

		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
		given(categoriaService.buscarPorNome(any(Categoria.class))).willReturn(Collections.emptyList());

		Categoria categoriaUtilidade = new Categoria();
		categoriaUtilidade.setId(123L);
		categoriaUtilidade.setNome("Utilidade");
		given(categoriaService.salvar(any(CategoriaDTO.class), eq(authentication))).willReturn(categoriaUtilidade);

		Set<Categoria> categoriasResolvidas = service
			.resolverCategoriasDoUsuario(Arrays.asList(" ", "\t", null, "Utilidade"), authentication);

		assertThat(categoriasResolvidas).extracting("nome").containsExactly("Utilidade");
	}

}
