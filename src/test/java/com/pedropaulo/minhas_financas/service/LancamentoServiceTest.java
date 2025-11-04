package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.api.dto.LancamentoDTO;
import com.pedropaulo.minhas_financas.api.dto.LancamentoDTOFactory;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
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
    void deveSalvarUmLancamento() throws Exception {
        Usuario usuario = criarUsuario(1L, "usuario@teste.com");
        Lancamento lancamentoParaSalvar = lancamentoValido(usuario);

        Lancamento lancamentoSalvo = lancamentoValido(usuario);
        lancamentoSalvo.setId(1L);

        given(repository.save(lancamentoParaSalvar)).willReturn(lancamentoSalvo);

        Lancamento out = service.salvar(lancamentoParaSalvar);

        assertThat(out.getId()).isEqualTo(1L);
        assertThat(out.getStatusLancamento()).isEqualTo(StatusLancamento.PENDENTE);
        then(repository).should().save(lancamentoParaSalvar);
    }

    @Test
    void deveLancarErroAoTentarSalvarUmLancamentoInvalido() {
        Lancamento lancamentoInvalido = new Lancamento();
        assertThatThrownBy(() -> service.salvar(lancamentoInvalido)).isInstanceOf(RegraNegocioException.class);
        then(repository).shouldHaveNoInteractions();
    }

    @Test
    void deveAtualizarUmLancamento() throws Exception {
        Long id = 1L;
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(10L, email);

        Lancamento existente = lancamentoValido(usuario);
        existente.setId(id);

        LancamentoDTO dto = LancamentoDTOFactory.create(id, "Lançamento teste", 11, 2025, BigDecimal.valueOf(100),
                TipoLancamento.DESPESA.name(), StatusLancamento.PENDENTE.name());
        dto.setCategorias(Collections.emptyList());

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(id, usuario.getId())).willReturn(Optional.of(existente));
        given(repository.save(existente)).willReturn(existente);

        Lancamento atualizado = service.atualizar(id, authentication, dto);

        then(repository).should().save(existente);
        assertThat(atualizado).isEqualTo(existente);
    }

    @Test
    void deveLancarErroAoTentarAtualizarUmLancamentoQueAindaNaoFoiSalvo() throws Exception {
        Long id = 1L;
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(10L, email);

        LancamentoDTO dto = LancamentoDTOFactory.create(id, "Lançamento teste", 11, 2025, BigDecimal.valueOf(100),
                TipoLancamento.DESPESA.name(), StatusLancamento.PENDENTE.name());

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(id, usuario.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(id, authentication, dto)).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Lançamento não encontrado para o ID informado.");
        then(repository).should(never()).save(any());
    }

    @Test
    void deveDeletarUmLancamento() throws Exception {
        Long id = 1L;
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(7L, email);
        Lancamento existente = lancamentoValido(usuario);
        existente.setId(id);

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(id, usuario.getId())).willReturn(Optional.of(existente));

        service.deletar(id, authentication);

        then(repository).should().delete(existente);
    }

    @Test
    void deveLancarErroAoTentarDeletarUmLancamentoQueAindaNaoFoiSalvo() throws Exception {
        Long id = 1L;
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(7L, email);

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(id, usuario.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(id, authentication)).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Lançamento não encontrado para o ID informado.");
        then(repository).should(never()).delete(any(Lancamento.class));
    }

    @Test
    void deveFiltrarLancamentos() {
        Lancamento lancamento = new Lancamento();
        lancamento.setId(1L);

        given(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .willReturn(Collections.singletonList(lancamento));

        List<Lancamento> out = service.buscar(lancamento, Collections.emptyList());

        assertThat(out).hasSize(1).containsExactly(lancamento);
        then(repository).should().findAll(any(org.springframework.data.jpa.domain.Specification.class));
    }

    @Test
    void deveAtualizarOStatusDeUmLancamento() throws Exception {
        Long id = 1L;
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(3L, email);
        Lancamento existente = lancamentoValido(usuario);
        existente.setId(id);
        existente.setStatusLancamento(StatusLancamento.PENDENTE);

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(id, usuario.getId())).willReturn(Optional.of(existente));

        service.atualizarStatus(id, authentication, StatusLancamento.EFETIVADO);

        assertThat(existente.getStatusLancamento()).isEqualTo(StatusLancamento.EFETIVADO);
    }

    @Test
    void deveObterUmLancamentoPorId() throws Exception {
        Long idLancamento = 1L;
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(1L, email);
        Lancamento lancamento = lancamentoValido(usuario);
        lancamento.setId(idLancamento);

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(idLancamento, usuario.getId())).willReturn(Optional.of(lancamento));

        Lancamento out = service.obterPorIdLancamento(idLancamento, authentication);

        assertThat(out).isNotNull();
        assertThat(out.getId()).isEqualTo(idLancamento);
    }

    @Test
    void deveRetornarErroQuandoOLancamentoNaoExistir() throws Exception {
        Long idLancamento = 1L;
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(1L, email);

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(idLancamento, usuario.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.obterPorIdLancamento(idLancamento, authentication))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Lançamento não encontrado para o ID informado.");
    }

    @Test
    void deveLancarErrosAoValidarUmLancamento() {
        Lancamento lancamento = new Lancamento();

        Throwable erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira uma descrição válida.");

        lancamento.setDescricao("");
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira uma descrição válida.");

        lancamento.setDescricao("Salário");
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira um mês válido.");

        lancamento.setMes(0);
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira um mês válido.");

        lancamento.setMes(13);
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira um mês válido.");

        lancamento.setMes(1);
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira um ano válido.");

        lancamento.setAno(202);
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira um ano válido.");

        lancamento.setAno(22025);
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira um ano válido.");

        lancamento.setAno(2025);
        lancamento.setValor(BigDecimal.ZERO);
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira um valor válido.");

        lancamento.setValor(BigDecimal.valueOf(-1));
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira um valor válido.");

        lancamento.setValor(BigDecimal.ONE);
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Insira um tipo de transação válido.");

        lancamento.setTipoLancamento(TipoLancamento.RECEITA);
        erro = catchThrowable(() -> service.validar(lancamento));
        assertThat(erro).hasMessage("Informe um usuário válido.");
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
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(1L, email);
        LancamentoDTO dto = new LancamentoDTO();
        dto.setDescricao("Teste DTO");
        dto.setMes(10);
        dto.setAno(2025);
        dto.setValor(BigDecimal.TEN);
        dto.setTipoLancamento(TipoLancamento.RECEITA.name());
        dto.setStatusLancamento(StatusLancamento.PENDENTE.name());
        dto.setCategorias(Collections.emptyList());

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);

        LocalDate hoje = LocalDate.now();
        Lancamento convertido = service.converterDTO(dto, authentication);

        assertThat(convertido.getDescricao()).isEqualTo(dto.getDescricao());
        assertThat(convertido.getMes()).isEqualTo(dto.getMes());
        assertThat(convertido.getAno()).isEqualTo(dto.getAno());
        assertThat(convertido.getValor()).isEqualTo(dto.getValor());
        assertThat(convertido.getTipoLancamento().name()).isEqualTo(dto.getTipoLancamento());
        assertThat(convertido.getStatusLancamento().name()).isEqualTo(dto.getStatusLancamento());
        assertThat(convertido.getUsuario()).isEqualTo(usuario);
        assertThat(convertido.getDataCadastro()).isEqualTo(hoje);
    }

    @Test
    void deveLancarErroAoConverterDTOComUsuarioInvalido() throws Exception {
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        LancamentoDTO dto = new LancamentoDTO();
        dto.setDescricao("Teste DTO");
        dto.setMes(10);
        dto.setAno(2025);
        dto.setValor(BigDecimal.TEN);
        dto.setTipoLancamento(TipoLancamento.RECEITA.name());
        dto.setStatusLancamento(StatusLancamento.PENDENTE.name());

        given(usuarioService.obterIdUsuarioPorEmail(email))
                .willThrow(new RegraNegocioException("Usuário não encontrado"));

        assertThatThrownBy(() -> service.converterDTO(dto, authentication)).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Usuário não encontrado");
        then(categoriaService).shouldHaveNoInteractions();
    }

    @Test
    void deveLancarErroAoConverterDTOComTipoInvalido() throws Exception {
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(1L, email);

        LancamentoDTO dto = new LancamentoDTO();
        dto.setDescricao("Teste DTO");
        dto.setMes(10);
        dto.setAno(2025);
        dto.setValor(BigDecimal.TEN);
        dto.setTipoLancamento("TIPO_INVALIDO");
        dto.setStatusLancamento(StatusLancamento.PENDENTE.name());

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);

        assertThatThrownBy(() -> service.converterDTO(dto, authentication))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveLancarErroAoConverterDTOComStatusInvalido() throws Exception {
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(1L, email);

        LancamentoDTO dto = new LancamentoDTO();
        dto.setDescricao("Teste DTO");
        dto.setMes(10);
        dto.setAno(2025);
        dto.setValor(BigDecimal.TEN);
        dto.setTipoLancamento(TipoLancamento.RECEITA.name());
        dto.setStatusLancamento("STATUS_INVALIDO");

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);

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
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(5L, email);
        Lancamento lancamento = new Lancamento();
        lancamento.setId(id);
        lancamento.setStatusLancamento(StatusLancamento.PENDENTE);
        lancamento.setUsuario(usuario);

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(id, usuario.getId())).willReturn(Optional.of(lancamento));

        assertThatCode(() -> service.validarStatusLancamento(id, authentication)).doesNotThrowAnyException();

        then(repository).should().findLancamentoByIdAndUsuarioId(id, usuario.getId());
    }

    @Test
    void deveLancarErroAoValidarStatusLancamentoQuandoEstiverEfetivado() throws Exception {
        Long id = 1L;
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(5L, email);
        Lancamento lancamento = new Lancamento();
        lancamento.setId(id);
        lancamento.setStatusLancamento(StatusLancamento.EFETIVADO);
        lancamento.setUsuario(usuario);

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(id, usuario.getId())).willReturn(Optional.of(lancamento));

        Throwable erro = catchThrowable(() -> service.validarStatusLancamento(id, authentication));

        assertThat(erro).isInstanceOf(com.pedropaulo.minhas_financas.exception.EntidadeNaoProcessavelException.class)
                .hasMessage("Lançamentos efetivados ou cancelados não podem ser editados ou deletados.");
    }

    @Test
    void deveLancarErroAoValidarStatusLancamentoQuandoEstiverCancelado() throws Exception {
        Long id = 1L;
        String email = "usuario@teste.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = criarUsuario(5L, email);
        Lancamento lancamento = new Lancamento();
        lancamento.setId(id);
        lancamento.setStatusLancamento(StatusLancamento.CANCELADO);
        lancamento.setUsuario(usuario);

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(id, usuario.getId())).willReturn(Optional.of(lancamento));

        assertThatThrownBy(() -> service.validarStatusLancamento(id, authentication))
                .isInstanceOf(com.pedropaulo.minhas_financas.exception.EntidadeNaoProcessavelException.class)
                .hasMessage("Lançamentos efetivados ou cancelados não podem ser editados ou deletados.");
    }

    @Test
    void resolverCategoriasDoUsuario_quandoListaNulaOuVazia_retornaVazioENaoInterage() throws Exception {
        Set<Categoria> resolvidas1 = service.resolverCategoriasDoUsuario(null, authentication);
        Set<Categoria> resolvidas2 = service.resolverCategoriasDoUsuario(Collections.emptyList(), authentication);

        assertThat(resolvidas1).isEmpty();
        assertThat(resolvidas2).isEmpty();

        then(usuarioService).shouldHaveNoInteractions();
        then(categoriaService).shouldHaveNoInteractions();
    }

    @Test
    void resolverCategoriasDoUsuario_misturaDeExistenteNovoEBranco_resolveEChamaSalvarParaNovo() throws Exception {
        String email = "u@t.com";
        authentication = new TestingAuthenticationToken(email, null);
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail(email);

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);

        Categoria existente = Categoria.builder().id(10L).nome("Mercado").usuario(usuario).build();
        given(categoriaService.buscarPorNome(argThat(categoriaArg -> categoriaArg != null
                && usuario.equals(categoriaArg.getUsuario()) && "Mercado".equalsIgnoreCase(categoriaArg.getNome()))))
                .willReturn(List.of(existente));

        given(categoriaService.buscarPorNome(argThat(categoriaArg -> categoriaArg != null
                && usuario.equals(categoriaArg.getUsuario()) && "Transporte".equalsIgnoreCase(categoriaArg.getNome()))))
                .willThrow(new RegraNegocioException("nenhuma encontrada"));

        Categoria criada = Categoria.builder().id(11L).nome("Transporte").usuario(usuario).build();
        given(categoriaService.salvar(any(CategoriaDTO.class), eq(authentication))).willReturn(criada);

        List<String> nomes = Arrays.asList("Mercado", "  ", "Transporte");

        Set<Categoria> out = service.resolverCategoriasDoUsuario(nomes, authentication);

        assertThat(out).extracting(Categoria::getNome).containsExactlyInAnyOrder("Mercado", "Transporte");

        then(categoriaService).should().salvar(argThat(dto -> "Transporte".equals(dto.getNome())), eq(authentication));
        then(categoriaService).shouldHaveNoMoreInteractions();
    }

    @Test
    void buscar_quandoFiltroVazio_retornaListaDoRepositorio() {
        Lancamento filtro = new Lancamento();
        filtro.setId(1L);

        given(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).willReturn(List.of(filtro));

        List<Lancamento> out = service.buscar(new Lancamento(), Collections.emptyList());

        assertThat(out).hasSize(1).containsExactly(filtro);
        then(repository).should().findAll(any(org.springframework.data.jpa.domain.Specification.class));
    }

    @Test
    void buscar_quandoTodosFiltrosEComCategorias_aplicaEspecificacaoEDevolveResultado() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);

        Lancamento filtro = new Lancamento();
        filtro.setUsuario(usuario);
        filtro.setDescricao("Aluguel");
        filtro.setMes(10);
        filtro.setAno(2025);
        filtro.setValor(BigDecimal.valueOf(1200));
        filtro.setTipoLancamento(TipoLancamento.DESPESA);
        filtro.setStatusLancamento(StatusLancamento.PENDENTE);

        List<Long> categoriaIds = Arrays.asList(1L, 2L);

        Lancamento resultado = new Lancamento();
        resultado.setId(42L);

        given(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .willReturn(List.of(resultado));

        List<Lancamento> out = service.buscar(filtro, categoriaIds);

        assertThat(out).hasSize(1).containsExactly(resultado);
        then(repository).should().findAll(any(org.springframework.data.jpa.domain.Specification.class));
    }
}
