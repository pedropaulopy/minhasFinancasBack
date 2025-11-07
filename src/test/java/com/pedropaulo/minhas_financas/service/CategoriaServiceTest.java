package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import com.pedropaulo.minhas_financas.service.impl.CategoriaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

	private static final String EMAIL = "usuario@teste.com";

	private static final Long ID_USUARIO = 1L;

	private static final Long ID_CATEGORIA = 10L;

	@Mock
	CategoriaRepository repository;

	@Mock
	UsuarioService usuarioService;

	@Mock
	LancamentoRepository lancamentoRepository;

	@InjectMocks
	CategoriaServiceImpl service;

	@Captor
	ArgumentCaptor<Categoria> categoriaCaptor;

	private Authentication auth() {
		return new TestingAuthenticationToken(EMAIL, null);
	}

	private Usuario criarUsuario() {
		Usuario usuario = new Usuario();
		usuario.setId(ID_USUARIO);
		usuario.setEmail(EMAIL);
		return usuario;
	}

	private Categoria criarCategoria() {
		Categoria categoria = new Categoria();
		categoria.setId(ID_CATEGORIA);
		categoria.setNome("Lazer");
		categoria.setUsuario(criarUsuario());
		return categoria;
	}

	@Test
	void buscarPorNome_comNomeValido_retornaLista() throws RegraNegocioException {
		Categoria filtro = new Categoria();
		filtro.setNome("   Lazer   ");
		Categoria categoriaEncontrada = criarCategoria();
		given(repository.findAll(any(Example.class))).willReturn(List.of(categoriaEncontrada));

		List<Categoria> resultado = service.buscarPorNome(filtro);

		assertThat(resultado).containsExactly(categoriaEncontrada);
		ArgumentCaptor<Example> exampleCaptor = ArgumentCaptor.forClass(Example.class);
		then(repository).should().findAll(exampleCaptor.capture());
		assertThat(exampleCaptor.getValue().getProbe()).extracting("nome").isEqualTo("Lazer");
	}

	@Test
	void buscarPorNome_comNomeVazio_retornaLista() throws RegraNegocioException {
		Categoria filtro = new Categoria();
		filtro.setNome("   ");
		Categoria categoriaEncontrada = criarCategoria();
		given(repository.findAll(any(Example.class))).willReturn(List.of(categoriaEncontrada));

		List<Categoria> resultado = service.buscarPorNome(filtro);

		assertThat(resultado).containsExactly(categoriaEncontrada);
		ArgumentCaptor<Example> exampleCaptor = ArgumentCaptor.forClass(Example.class);
		then(repository).should().findAll(exampleCaptor.capture());
		assertThat(exampleCaptor.getValue().getProbe()).extracting("nome").isNull();
	}

	@Test
	void buscarPorNome_comFiltroNulo_retornaLista() throws RegraNegocioException {
		Categoria categoriaEncontrada = criarCategoria();
		given(repository.findAll(any(Example.class))).willReturn(List.of(categoriaEncontrada));

		List<Categoria> resultado = service.buscarPorNome(null);

		assertThat(resultado).containsExactly(categoriaEncontrada);
		ArgumentCaptor<Example> exampleCaptor = ArgumentCaptor.forClass(Example.class);
		then(repository).should().findAll(exampleCaptor.capture());
		assertThat(exampleCaptor.getValue().getProbe()).extracting("nome").isNull();
	}

	@Test
	void buscarPorNome_quandoNaoEncontra_lancaExcecao() throws RegraNegocioException {
		given(repository.findAll(any(Example.class))).willReturn(Collections.emptyList());
		Categoria filtro = new Categoria();

		assertThatThrownBy(() -> service.buscarPorNome(filtro)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Nenhum lançamento encontrado para este nome.");
	}

	@Test
	void validar_quandoDuplicado_lancaExcecao() throws RegraNegocioException {
		Categoria categoria = criarCategoria();
		given(repository.findByNomeIgnoreCaseAndUsuario(categoria.getNome(), categoria.getUsuario()))
			.willReturn(Optional.of(new Categoria()));

		assertThatThrownBy(() -> service.validar(categoria)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Uma categoria com esse nome já existe");
	}

	@Test
	void validar_quandoNomeNulo_lancaExcecao() throws RegraNegocioException {
		Categoria categoria = criarCategoria();
		categoria.setNome(null);
		given(repository.findByNomeIgnoreCaseAndUsuario(null, categoria.getUsuario())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.validar(categoria)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Insira uma nome válido.");
	}

	@Test
	void validar_quandoNomeVazio_lancaExcecao() throws RegraNegocioException {
		Categoria categoria = criarCategoria();
		categoria.setNome("  ");
		given(repository.findByNomeIgnoreCaseAndUsuario("  ", categoria.getUsuario())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.validar(categoria)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Insira uma nome válido.");
	}

	@Test
	void validar_quandoValido_naoLancaNada() throws RegraNegocioException {
		Categoria categoria = criarCategoria();
		given(repository.findByNomeIgnoreCaseAndUsuario(categoria.getNome(), categoria.getUsuario()))
			.willReturn(Optional.empty());

		assertThatCode(() -> service.validar(categoria)).doesNotThrowAnyException();
	}

	@Test
	void salvar_comDadosValidos_salvaERetornaCategoria() throws RegraNegocioException {
		Usuario usuario = criarUsuario();
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);
		given(repository.findByNomeIgnoreCaseAndUsuario(anyString(), any(Usuario.class))).willReturn(Optional.empty());
		Categoria categoriaSalva = criarCategoria();
		given(repository.save(any(Categoria.class))).willReturn(categoriaSalva);

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Viagem");

		Categoria resultado = service.salvar(dto, auth());

		assertThat(resultado).isEqualTo(categoriaSalva);
		then(repository).should().save(categoriaCaptor.capture());
		assertThat(categoriaCaptor.getValue().getNome()).isEqualTo("Viagem");
		assertThat(categoriaCaptor.getValue().getUsuario()).isEqualTo(usuario);
	}

	@Test
	void salvar_comNomeDuplicado_lancaExcecao() throws RegraNegocioException {
		Usuario usuario = criarUsuario();
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);
		given(repository.findByNomeIgnoreCaseAndUsuario("Viagem", usuario)).willReturn(Optional.of(new Categoria()));

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Viagem");

		assertThatThrownBy(() -> service.salvar(dto, auth())).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Uma categoria com esse nome já existe");
		then(repository).should(never()).save(any());
	}

	@Test
	void atualizar_quandoNaoExiste_lancaExcecao() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.empty());

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Novo Nome");

		assertThatThrownBy(() -> service.atualizar(ID_CATEGORIA, auth(), dto)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Nenhuma categoria foi encontrada para o ID fornecido");
	}

	@Test
	void atualizar_comNomeInvalido_lancaExcecao() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.of(criarCategoria()));

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome(" ");

		assertThatThrownBy(() -> service.atualizar(ID_CATEGORIA, auth(), dto)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Insira um nome válido.");
		then(repository).should(never()).save(any());
	}

	@Test
	void atualizar_comNomeDuplicado_lancaExcecao() throws RegraNegocioException {
		Usuario usuario = criarUsuario();
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(usuario);
		Categoria categoriaExistente = criarCategoria();
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.of(categoriaExistente));
		given(repository.findByNomeIgnoreCaseAndUsuario("Nome Duplicado", usuario))
			.willReturn(Optional.of(new Categoria()));

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Nome Duplicado");

		assertThatThrownBy(() -> service.atualizar(ID_CATEGORIA, auth(), dto)).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Uma categoria com esse nome já existe");
		then(repository).should(never()).save(any());
	}

	@Test
	void atualizar_comMesmoNome_salvaComSucesso() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		Categoria categoriaExistente = criarCategoria();
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.of(categoriaExistente));
		given(repository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("lazer"); // mesmo nome, case diferente

		Categoria resultado = service.atualizar(ID_CATEGORIA, auth(), dto);

		assertThat(resultado.getNome()).isEqualTo("lazer");
		then(repository).should(times(1)).save(any(Categoria.class));
		then(repository).should(never()).findByNomeIgnoreCaseAndUsuario(anyString(), any());
	}

	@Test
	void atualizar_comNovoNome_salvaComSucesso() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		Categoria categoriaExistente = criarCategoria();
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.of(categoriaExistente));
		given(repository.findByNomeIgnoreCaseAndUsuario("Novo Nome", categoriaExistente.getUsuario()))
			.willReturn(Optional.empty());
		given(repository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Novo Nome");

		Categoria resultado = service.atualizar(ID_CATEGORIA, auth(), dto);

		assertThat(resultado.getNome()).isEqualTo("Novo Nome");
		then(repository).should(times(1)).save(categoriaCaptor.capture());
		assertThat(categoriaCaptor.getAllValues().get(0).getNome()).isEqualTo("Novo Nome");
	}

	@Test
	void converterDTO_mapeiaCamposCorretamente() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Casa");

		Categoria categoria = service.converterDTO(dto, auth());

		assertThat(categoria.getNome()).isEqualTo("Casa");
		assertThat(categoria.getUsuario()).isEqualTo(criarUsuario());
	}

	@Test
	void obterPorIdCategoria_quandoEncontra_retornaOptionalComCategoria() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		Categoria categoria = criarCategoria();
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.of(categoria));

		Optional<Categoria> resultado = service.obterPorIdCategoria(ID_CATEGORIA, auth());

		assertThat(resultado).contains(categoria);
	}

	@Test
	void obterPorIdCategoria_quandoNaoEncontra_retornaOptionalVazio() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.empty());

		Optional<Categoria> resultado = service.obterPorIdCategoria(ID_CATEGORIA, auth());

		assertThat(resultado).isEmpty();
	}

	@Test
	void deletar_quandoEmUso_lancaExcecao() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.of(criarCategoria()));
		given(lancamentoRepository.existsByCategorias_Id(ID_CATEGORIA)).willReturn(true);
		given(lancamentoRepository.countByCategorias_Id(ID_CATEGORIA)).willReturn(3L);

		assertThatThrownBy(() -> service.deletar(ID_CATEGORIA, auth())).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Não é possível excluir: a categoria está vinculada a 3 lançamento(s).");
		then(repository).should(never()).delete(any());
	}

	@Test
	void deletar_quandoNaoEmUso_excluiComSucesso() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		Categoria categoria = criarCategoria();
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.of(categoria));
		given(lancamentoRepository.existsByCategorias_Id(ID_CATEGORIA)).willReturn(false);

		assertThatCode(() -> service.deletar(ID_CATEGORIA, auth())).doesNotThrowAnyException();

		then(repository).should().delete(categoria);
	}

	@Test
	void deletar_quandoCategoriaNaoExiste_lancaExcecao() throws RegraNegocioException {
		given(usuarioService.obterIdUsuarioPorEmail(EMAIL)).willReturn(criarUsuario());
		given(repository.findByIdAndUsuario_Id(ID_CATEGORIA, ID_USUARIO)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.deletar(ID_CATEGORIA, auth())).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Categoria não encontrada para o ID fornecido.");
		then(repository).should(never()).delete(any());
	}

}