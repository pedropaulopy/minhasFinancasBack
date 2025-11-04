package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.api.dto.LancamentoDTO;
import com.pedropaulo.minhas_financas.api.dto.LancamentoDTOFactory;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import com.pedropaulo.minhas_financas.service.impl.LancamentoServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
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

	@Mock
	Authentication authentication;

	LancamentoServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new LancamentoServiceImpl(repository, usuarioService, categoriaService);
	}

	private Usuario usuario(Long id, String email) {
		Usuario u = new Usuario();
		u.setId(id);
		u.setEmail(email);
		return u;
	}

	private Lancamento lancamentoValido(Usuario u) {
		return Lancamento.builder()
			.ano(2025)
			.mes(11)
			.descricao("Lançamento teste")
			.valor(BigDecimal.valueOf(100))
			.tipoLancamento(TipoLancamento.DESPESA)
			.statusLancamento(StatusLancamento.PENDENTE)
			.dataCadastro(LocalDate.now())
			.usuario(u)
			.build();
	}

	@Test
	void deveSalvarUmLancamento() throws Exception {
		Usuario u = usuario(1L, "usuario@teste.com");
		Lancamento aSalvar = lancamentoValido(u);

		Lancamento salvo = lancamentoValido(u);
		salvo.setId(1L);

		given(repository.save(aSalvar)).willReturn(salvo);

		Lancamento out = service.salvar(aSalvar);

		assertThat(out.getId()).isEqualTo(1L);
		assertThat(out.getStatusLancamento()).isEqualTo(StatusLancamento.PENDENTE);
		then(repository).should().save(aSalvar);
	}

	@Test
	void deveLancarErroAoTentarSalvarUmLancamentoInvalido() {
		Lancamento invalido = new Lancamento();
		assertThatThrownBy(() -> service.salvar(invalido)).isInstanceOf(RegraNegocioException.class);
		then(repository).shouldHaveNoInteractions();
	}

	@Test
	void deveAtualizarUmLancamento() throws Exception {
		Long id = 1L;
		String email = "usuario@teste.com";
		Usuario u = usuario(10L, email);

		Lancamento existente = lancamentoValido(u);
		existente.setId(id);

		LancamentoDTO dto = LancamentoDTOFactory.create(id, "Lançamento teste", 11, 2025, BigDecimal.valueOf(100),
				TipoLancamento.DESPESA.name(), StatusLancamento.PENDENTE.name());

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(repository.findLancamentoByIdAndUsuarioId(id, u.getId())).willReturn(Optional.of(existente));
		given(categoriaService.buscarOuCriarCategorias(dto.getCategorias(), authentication))
			.willReturn(Collections.emptySet());
		given(repository.save(existente)).willReturn(existente);

		Lancamento atualizado = service.atualizar(id, authentication, dto);

		then(repository).should().save(existente);
		assertThat(atualizado).isEqualTo(existente);
	}

	@Test
	void deveLancarErroAoTentarAtualizarUmLancamentoQueAindaNaoFoiSalvo() throws Exception {
		Long id = 1L;
		String email = "usuario@teste.com";
		Usuario u = usuario(10L, email);

		LancamentoDTO dto = LancamentoDTOFactory.create(id, "Lançamento teste", 11, 2025, BigDecimal.valueOf(100),
				TipoLancamento.DESPESA.name(), StatusLancamento.PENDENTE.name());

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(repository.findLancamentoByIdAndUsuarioId(id, u.getId())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.atualizar(id, authentication, dto)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Lançamento não encontrado para o ID informado.");
		then(repository).should(never()).save(any());
	}

	@Test
	void deveDeletarUmLancamento() throws Exception {
		Long id = 1L;
		String email = "usuario@teste.com";
		Usuario u = usuario(7L, email);
		Lancamento existente = lancamentoValido(u);
		existente.setId(id);

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(repository.findLancamentoByIdAndUsuarioId(id, u.getId())).willReturn(Optional.of(existente));

		service.deletar(id, authentication);

		then(repository).should().delete(existente);
	}

	@Test
	void deveLancarErroAoTentarDeletarUmLancamentoQueAindaNaoFoiSalvo() throws Exception {
		Long id = 1L;
		String email = "usuario@teste.com";
		Usuario u = usuario(7L, email);

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(repository.findLancamentoByIdAndUsuarioId(id, u.getId())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.deletar(id, authentication)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Lançamento não encontrado para o ID informado.");
		then(repository).should(never()).delete(any());
	}

	@Test
	void deveFiltrarLancamentos() {
		Lancamento l = new Lancamento();
		l.setId(1L);
		given(repository.findAll(any(org.springframework.data.domain.Example.class)))
			.willReturn(Collections.singletonList(l));

		List<Lancamento> out = service.buscar(l);

		assertThat(out).hasSize(1).containsExactly(l);
		then(repository).should().findAll(any(org.springframework.data.domain.Example.class));
	}

	@Test
	void deveAtualizarOStatusDeUmLancamento() throws Exception {
		Long id = 1L;
		String email = "usuario@teste.com";
		Usuario u = usuario(3L, email);
		Lancamento existente = lancamentoValido(u);
		existente.setId(id);
		existente.setStatusLancamento(StatusLancamento.PENDENTE);

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(repository.findLancamentoByIdAndUsuarioId(id, u.getId())).willReturn(Optional.of(existente));

		service.atualizarStatus(id, authentication, StatusLancamento.EFETIVADO);

		assertThat(existente.getStatusLancamento()).isEqualTo(StatusLancamento.EFETIVADO);
	}

	@Test
	void deveObterUmLancamentoPorId() throws Exception {
		Long idLanc = 1L;
		String email = "usuario@teste.com";
		Usuario u = usuario(1L, email);
		Lancamento l = lancamentoValido(u);
		l.setId(idLanc);

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(repository.findLancamentoByIdAndUsuarioId(idLanc, u.getId())).willReturn(Optional.of(l));

		Lancamento out = service.obterPorIdLancamento(idLanc, authentication);

		assertThat(out).isNotNull();
		assertThat(out.getId()).isEqualTo(idLanc);
	}

	@Test
	void deveRetornarErroQuandoOLancamentoNaoExistir() throws Exception {
		Long idLanc = 1L;
		String email = "usuario@teste.com";
		Usuario u = usuario(1L, email);

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(repository.findLancamentoByIdAndUsuarioId(idLanc, u.getId())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.obterPorIdLancamento(idLanc, authentication))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessage("Lançamento não encontrado para o ID informado.");
	}

	@Test
	void deveLancarErrosAoValidarUmLancamento() {
		Lancamento l = new Lancamento();

		Throwable t = catchThrowable(() -> service.validar(l));
		assertThat(t).isInstanceOf(RegraNegocioException.class).hasMessage("Insira uma descrição válida.");

		l.setDescricao("");
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira uma descrição válida.");

		l.setDescricao("Salário");
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira um mês válido.");

		l.setMes(0);
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira um mês válido.");

		l.setMes(13);
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira um mês válido.");

		l.setMes(1);
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira um ano válido.");

		l.setAno(202);
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira um ano válido.");

		l.setAno(22025);
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira um ano válido.");

		l.setAno(2025);
		l.setValor(BigDecimal.ZERO);
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira um valor válido.");

		l.setValor(BigDecimal.valueOf(-1));
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira um valor válido.");

		l.setValor(BigDecimal.ONE);
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Insira um tipo de transação válido.");

		l.setTipoLancamento(TipoLancamento.RECEITA);
		t = catchThrowable(() -> service.validar(l));
		assertThat(t).hasMessage("Informe um usuário válido.");
	}

	@Test
	void deveObterSaldoDeUmUsuario() throws Exception {
		Long idUsuario = 1L;
		given(usuarioService.obterPorId(idUsuario)).willReturn(Optional.of(new Usuario()));
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.RECEITA,
				StatusLancamento.EFETIVADO))
			.willReturn(BigDecimal.valueOf(100));
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.DESPESA,
				StatusLancamento.EFETIVADO))
			.willReturn(BigDecimal.valueOf(50));

		BigDecimal saldo = service.obterSaldoPorUsuario(idUsuario);

		assertThat(saldo).isEqualTo(BigDecimal.valueOf(50));
	}

	@Test
	void deveConverterUmLancamentoDTOEmUmLancamento() throws Exception {
		String email = "usuario@teste.com";
		Usuario u = usuario(1L, email);
		LancamentoDTO dto = new LancamentoDTO();
		dto.setDescricao("Teste DTO");
		dto.setMes(10);
		dto.setAno(2025);
		dto.setValor(BigDecimal.TEN);
		dto.setTipoLancamento(TipoLancamento.RECEITA.name());
		dto.setStatusLancamento(StatusLancamento.PENDENTE.name());

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(categoriaService.buscarOuCriarCategorias(dto.getCategorias(), authentication))
			.willReturn(Collections.emptySet());

		LocalDate hoje = LocalDate.now();
		Lancamento l = service.converterDTO(dto, authentication);

		assertThat(l.getDescricao()).isEqualTo(dto.getDescricao());
		assertThat(l.getMes()).isEqualTo(dto.getMes());
		assertThat(l.getAno()).isEqualTo(dto.getAno());
		assertThat(l.getValor()).isEqualTo(dto.getValor());
		assertThat(l.getTipoLancamento().name()).isEqualTo(dto.getTipoLancamento());
		assertThat(l.getStatusLancamento().name()).isEqualTo(dto.getStatusLancamento());
		assertThat(l.getUsuario()).isEqualTo(u);
		assertThat(l.getDataCadastro()).isEqualTo(hoje);
		then(categoriaService).should().buscarOuCriarCategorias(dto.getCategorias(), authentication);
	}

	@Test
	void deveLancarErroAoConverterDTOComUsuarioInvalido() throws Exception {
		String email = "usuario@teste.com";
		LancamentoDTO dto = new LancamentoDTO();
		dto.setDescricao("Teste DTO");
		dto.setMes(10);
		dto.setAno(2025);
		dto.setValor(BigDecimal.TEN);
		dto.setTipoLancamento(TipoLancamento.RECEITA.name());
		dto.setStatusLancamento(StatusLancamento.PENDENTE.name());

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email))
			.willThrow(new RegraNegocioException("Usuário não encontrado"));

		assertThatThrownBy(() -> service.converterDTO(dto, authentication)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Usuário não encontrado");
		then(categoriaService).shouldHaveNoInteractions();
	}

	@Test
	void deveLancarErroAoConverterDTOComTipoInvalido() throws Exception {
		String email = "usuario@teste.com";
		Usuario u = usuario(1L, email);

		LancamentoDTO dto = new LancamentoDTO();
		dto.setDescricao("Teste DTO");
		dto.setMes(10);
		dto.setAno(2025);
		dto.setValor(BigDecimal.TEN);
		dto.setTipoLancamento("TIPO_INVALIDO");
		dto.setStatusLancamento(StatusLancamento.PENDENTE.name());

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);

		assertThatThrownBy(() -> service.converterDTO(dto, authentication))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void deveLancarErroAoConverterDTOComStatusInvalido() throws Exception {
		String email = "usuario@teste.com";
		Usuario u = usuario(1L, email);

		LancamentoDTO dto = new LancamentoDTO();
		dto.setDescricao("Teste DTO");
		dto.setMes(10);
		dto.setAno(2025);
		dto.setValor(BigDecimal.TEN);
		dto.setTipoLancamento(TipoLancamento.RECEITA.name());
		dto.setStatusLancamento("STATUS_INVALIDO");

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);

		assertThatThrownBy(() -> service.converterDTO(dto, authentication))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void deveObterSaldoDeUmUsuarioOndeReceitasEhNull() throws Exception {
		Long idUsuario = 1L;
		given(usuarioService.obterPorId(idUsuario)).willReturn(Optional.of(new Usuario()));
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.RECEITA,
				StatusLancamento.EFETIVADO))
			.willReturn(null);
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.DESPESA,
				StatusLancamento.EFETIVADO))
			.willReturn(BigDecimal.valueOf(50));

		BigDecimal saldo = service.obterSaldoPorUsuario(idUsuario);

		assertThat(saldo).isEqualTo(BigDecimal.valueOf(-50));
	}

	@Test
	void deveObterSaldoDeUmUsuarioOndeDespesasEhNull() throws Exception {
		Long idUsuario = 1L;
		given(usuarioService.obterPorId(idUsuario)).willReturn(Optional.of(new Usuario()));
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.RECEITA,
				StatusLancamento.EFETIVADO))
			.willReturn(BigDecimal.valueOf(100));
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.DESPESA,
				StatusLancamento.EFETIVADO))
			.willReturn(null);

		BigDecimal saldo = service.obterSaldoPorUsuario(idUsuario);

		assertThat(saldo).isEqualTo(BigDecimal.valueOf(100));
	}

	@Test
	void deveObterSaldoDeUmUsuarioOndeAmbosSaoNull() throws Exception {
		Long idUsuario = 1L;
		given(usuarioService.obterPorId(idUsuario)).willReturn(Optional.of(new Usuario()));
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.RECEITA,
				StatusLancamento.EFETIVADO))
			.willReturn(null);
		given(repository.obterSaldoPorTipoLancamentoEUsuarioEStatusEAnoEMes(idUsuario, TipoLancamento.DESPESA,
				StatusLancamento.EFETIVADO))
			.willReturn(null);

		BigDecimal saldo = service.obterSaldoPorUsuario(idUsuario);

		assertThat(saldo).isZero();
	}

	@Test
	void deveLancarErroAoObterSaldoDeUsuarioInexistente() throws Exception {
		Long idUsuario = 1L;
		given(usuarioService.obterPorId(idUsuario)).willThrow(new RegraNegocioException("Usuário não encontrado"));

		assertThatThrownBy(() -> service.obterSaldoPorUsuario(idUsuario)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Usuário não encontrado");

		then(repository).shouldHaveNoInteractions();
	}

	@Test
	void deveValidarStatusLancamentoQuandoEstiverPendente() throws Exception {
		Long id = 1L;
		String email = "usuario@teste.com";
		Usuario u = usuario(5L, email);
		Lancamento l = new Lancamento();
		l.setId(id);
		l.setStatusLancamento(StatusLancamento.PENDENTE);
		l.setUsuario(u);

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(repository.findLancamentoByIdAndUsuarioId(id, u.getId())).willReturn(Optional.of(l));

		assertThatCode(() -> service.validarStatusLancamento(id, authentication)).doesNotThrowAnyException();

		then(repository).should().findLancamentoByIdAndUsuarioId(id, u.getId());
	}

    @Test
    void deveLancarErroAoValidarStatusLancamentoQuandoEstiverEfetivado() throws Exception {
        Long id = 1L;
        String email = "usuario@teste.com";
        Usuario u = usuario(5L, email);
        Lancamento l = new Lancamento();
        l.setId(id);
        l.setStatusLancamento(StatusLancamento.EFETIVADO);
        l.setUsuario(u);

        given(authentication.getName()).willReturn(email);
        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
        given(repository.findLancamentoByIdAndUsuarioId(id, u.getId())).willReturn(Optional.of(l));

        Throwable erro = Assertions.catchThrowable(() -> service.validarStatusLancamento(id, authentication));

        Assertions.assertThat(erro)
                .isInstanceOf(com.pedropaulo.minhas_financas.exception.EntidadeNaoProcessavelException.class)
                .hasMessage("Lançamentos efetivados ou cancelados não podem ser editados ou deletados.");
    }


    @Test
	void deveLancarErroAoValidarStatusLancamentoQuandoEstiverCancelado() throws Exception {
		Long id = 1L;
		String email = "usuario@teste.com";
		Usuario u = usuario(5L, email);
		Lancamento l = new Lancamento();
		l.setId(id);
		l.setStatusLancamento(StatusLancamento.CANCELADO);
		l.setUsuario(u);

		given(authentication.getName()).willReturn(email);
		given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(u);
		given(repository.findLancamentoByIdAndUsuarioId(id, u.getId())).willReturn(Optional.of(l));

		assertThatThrownBy(() -> service.validarStatusLancamento(id, authentication))
			.isInstanceOf(com.pedropaulo.minhas_financas.exception.EntidadeNaoProcessavelException.class)
			.hasMessage("Lançamentos efetivados ou cancelados não podem ser editados ou deletados.");
	}

}
