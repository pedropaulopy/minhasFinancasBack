package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import com.pedropaulo.minhas_financas.service.impl.CategoriaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Example;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class CategoriaServiceTest {

	private static final String EMAIL = "usuario@teste.com";

	@Mock
	CategoriaRepository repository;

	@Mock
	UsuarioService usuarioService;

	@Mock
	LancamentoRepository lancamentoRepository;

	CategoriaServiceImpl service;

	private Authentication auth(String email) {
		return new TestingAuthenticationToken(email, null);
	}

	@BeforeEach
	void setUp() {
		service = new CategoriaServiceImpl(repository, usuarioService, lancamentoRepository);
	}

	@Test
	void buscarPorNome_quandoEncontra_retornaLista() throws Exception {
		Categoria filtro = new Categoria();
		Categoria categoriaEncontrada = new Categoria();
		categoriaEncontrada.setId(1L);
		categoriaEncontrada.setNome("Saúde");
		given(repository.findAll(any(Example.class))).willReturn(java.util.List.of(categoriaEncontrada));

		assertThat(service.buscarPorNome(filtro)).containsExactly(categoriaEncontrada);
		then(repository).should().findAll(any(Example.class));
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void buscarPorNome_quandoNaoEncontra_lancaExcecao() throws Exception {
		given(repository.findAll(any(Example.class))).willReturn(java.util.List.of());
		Categoria filtro = new Categoria();

		Throwable t = catchThrowable(() -> service.buscarPorNome(filtro));
		assertThat(t).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Nenhum lançamento encontrado para este nome.");
		then(repository).should().findAll(any(Example.class));
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void validar_quandoDuplicado_lancaExcecao() throws Exception {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		Categoria categoria = new Categoria();
		categoria.setNome("Lazer");
		categoria.setUsuario(usuario);
		given(repository.findByNomeIgnoreCaseAndUsuario("Lazer", usuario)).willReturn(Optional.of(new Categoria()));

		Throwable erro = catchThrowable(() -> service.validar(categoria));
		assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Uma categoria com esse nome já existe");
		then(repository).should().findByNomeIgnoreCaseAndUsuario("Lazer", usuario);
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void validar_quandoNomeInvalido_lancaExcecao() throws Exception {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		Categoria categoria = new Categoria();
		categoria.setUsuario(usuario);
		categoria.setNome("  ");
		given(repository.findByNomeIgnoreCaseAndUsuario(anyString(), any())).willReturn(Optional.empty());

		Throwable erro = catchThrowable(() -> service.validar(categoria));
		assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Insira uma nome válido.");
		then(repository).should().findByNomeIgnoreCaseAndUsuario(anyString(), any());
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void validar_quandoValido_naoLanca() throws Exception {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		Categoria categoria = new Categoria();
		categoria.setUsuario(usuario);
		categoria.setNome("Educação");
		given(repository.findByNomeIgnoreCaseAndUsuario("Educação", usuario)).willReturn(Optional.empty());

		assertThatCode(() -> service.validar(categoria)).doesNotThrowAnyException();
		then(repository).should().findByNomeIgnoreCaseAndUsuario("Educação", usuario);
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void salvar_invocaValidarESalva() throws Exception {
		Authentication authentication = auth(EMAIL);
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Viagem");

		given(repository.findByNomeIgnoreCaseAndUsuario("Viagem", usuario)).willReturn(Optional.empty());

		Categoria categoriaSalva = new Categoria();
		categoriaSalva.setId(5L);
		categoriaSalva.setNome("Viagem");
		categoriaSalva.setUsuario(usuario);
		given(repository.save(any(Categoria.class))).willReturn(categoriaSalva);

		Categoria resultado = service.salvar(dto, authentication);

		assertThat(resultado).isEqualTo(categoriaSalva);

		ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
		then(repository).should().save(captor.capture());
		assertThat(captor.getValue().getNome()).isEqualTo("Viagem");
		assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);

		then(repository).should().findByNomeIgnoreCaseAndUsuario("Viagem", usuario);
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void atualizar_quandoNaoExiste_lancaExcecao() throws Exception {
		Authentication authentication = auth(EMAIL);

		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);
		given(repository.findByIdAndUsuario_Id(99L, 1L)).willReturn(Optional.empty());

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Novo");
		Throwable t = catchThrowable(() -> service.atualizar(99L, authentication, dto));

		assertThat(t).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Nenhuma categoria foi encontrada para o ID fornecido");
		then(repository).should().findByIdAndUsuario_Id(99L, 1L);
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void atualizar_quandoNomeInvalido_lancaExcecao() throws Exception {
		Authentication authentication = auth(EMAIL);

		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);

		Categoria categoriaExistente = new Categoria();
		categoriaExistente.setId(3L);
		categoriaExistente.setUsuario(usuario);
		categoriaExistente.setNome("Antigo");
		given(repository.findByIdAndUsuario_Id(3L, 1L)).willReturn(Optional.of(categoriaExistente));

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome(" ");

		Throwable t = catchThrowable(() -> service.atualizar(3L, authentication, dto));
		assertThat(t).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um nome válido.");
		then(repository).should().findByIdAndUsuario_Id(3L, 1L);
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void atualizar_sucesso_salvaEDepoisRetornaSalvo() throws Exception {
		Authentication authentication = auth(EMAIL);

		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);

		Categoria categoriaExistente = new Categoria();
		categoriaExistente.setId(3L);
		categoriaExistente.setUsuario(usuario);
		categoriaExistente.setNome("Antigo");

		given(repository.findByIdAndUsuario_Id(3L, 1L)).willReturn(Optional.of(categoriaExistente));
		given(repository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Novo");

		Categoria resultado = service.atualizar(3L, authentication, dto);

		assertThat(resultado.getNome()).isEqualTo("Novo");

		then(repository).should().findByIdAndUsuario_Id(3L, 1L);
		then(repository).should(times(2)).save(any(Categoria.class));
		then(repository).should(never()).findByNomeIgnoreCaseAndUsuario(anyString(), any());
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void converterDTO_mapeiaCampos() throws Exception {
		Authentication authentication = auth(EMAIL);

		Usuario usuario = new Usuario();
		usuario.setId(2L);
		usuario.setEmail(EMAIL);
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Casa");

		Categoria categoria = service.converterDTO(dto, authentication);

		assertThat(categoria.getNome()).isEqualTo("Casa");
		assertThat(categoria.getUsuario()).isEqualTo(usuario);
		then(usuarioService).should().obterIdUsuarioPorEmail(EMAIL);
		then(repository).shouldHaveNoInteractions();
	}

	@Test
	void obterPorIdCategoria_repassaIdDoUsuario() throws Exception {
		Authentication authentication = auth(EMAIL);

		Usuario usuario = new Usuario();
		usuario.setId(7L);
		usuario.setEmail(EMAIL);
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);

		Categoria categoria = new Categoria();
		categoria.setId(15L);
		categoria.setUsuario(usuario);
		given(repository.findByIdAndUsuario_Id(15L, 7L)).willReturn(Optional.of(categoria));

		Optional<Categoria> resultado = service.obterPorIdCategoria(15L, authentication);

		assertThat(resultado).contains(categoria);
		then(repository).should().findByIdAndUsuario_Id(15L, 7L);
		then(repository).shouldHaveNoMoreInteractions();
	}

	@Test
	void deletar_quandoEmUso_lancaExcecaoComQuantidade() throws Exception {
		Authentication authentication = auth(EMAIL);

		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);

		Categoria categoria = new Categoria();
		categoria.setId(20L);
		categoria.setUsuario(usuario);
		categoria.setNome("X");
		given(repository.findByIdAndUsuario_Id(20L, 1L)).willReturn(Optional.of(categoria));
		given(lancamentoRepository.existsByCategorias_Id(20L)).willReturn(true);
		given(lancamentoRepository.countByCategorias_Id(20L)).willReturn(3L);

		Throwable erro = catchThrowable(() -> service.deletar(20L, authentication));
		assertThat(erro).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Não é possível excluir: a categoria está vinculada a 3 lançamento(s).");
		then(repository).should(never()).delete(any());
		then(lancamentoRepository).should().existsByCategorias_Id(20L);
		then(lancamentoRepository).should().countByCategorias_Id(20L);
		then(lancamentoRepository).shouldHaveNoMoreInteractions();
	}

	@Test
	void deletar_quandoNaoEmUso_exclui() throws Exception {
		Authentication authentication = auth(EMAIL);

		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);

		Categoria categoria = new Categoria();
		categoria.setId(21L);
		categoria.setUsuario(usuario);
		categoria.setNome("Y");
		given(repository.findByIdAndUsuario_Id(21L, 1L)).willReturn(Optional.of(categoria));
		given(lancamentoRepository.existsByCategorias_Id(21L)).willReturn(false);

		service.deletar(21L, authentication);

		then(repository).should().delete(categoria);
		then(lancamentoRepository).should().existsByCategorias_Id(21L);
		then(repository).shouldHaveNoMoreInteractions();
		then(lancamentoRepository).shouldHaveNoMoreInteractions();
	}

}
