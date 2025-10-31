package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Example;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoriaServiceImplTest {

	@Mock
	CategoriaRepository repository;

	@Mock
	UsuarioService usuarioService;

	@Mock
	LancamentoRepository lancamentoRepository;

	@Mock
	Authentication authentication;

	CategoriaServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CategoriaServiceImpl(repository, usuarioService, lancamentoRepository);
		when(authentication.getName()).thenReturn("usuario@teste.com");
	}

	@Test
	void buscarOuCriarCategorias_quandoListaNulaOuVazia_retornaVazio() throws Exception {
		assertThat(service.buscarOuCriarCategorias(null, authentication)).isEmpty();
		assertThat(service.buscarOuCriarCategorias(Collections.emptyList(), authentication)).isEmpty();
	}

	@Test
	void buscarOuCriarCategorias_quandoExistemAlgumas_criaOutrasSalvaETrazTodas() throws Exception {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail("usuario@teste.com");
		when(usuarioService.obterIdUsuarioPorEmail("usuario@teste.com")).thenReturn(usuario);

		Categoria existente = Categoria.builder().id(10L).nome("Mercado").usuario(usuario).build();
		when(repository.findByNomeAndUsuario(eq("Mercado"), eq(usuario))).thenReturn(Optional.of(existente));
		when(repository.findByNomeAndUsuario(eq("Transporte"), eq(usuario))).thenReturn(Optional.empty());

		Categoria criado = Categoria.builder().id(11L).nome("Transporte").usuario(usuario).build();
		when(repository.save(any(Categoria.class))).thenReturn(criado);

		Set<Categoria> out = service.buscarOuCriarCategorias(Arrays.asList("Mercado", "Transporte"), authentication);

		assertThat(out).extracting(Categoria::getNome).containsExactlyInAnyOrder("Mercado", "Transporte");
		ArgumentCaptor<Categoria> cap = ArgumentCaptor.forClass(Categoria.class);
		verify(repository).save(cap.capture());
		assertThat(cap.getValue().getNome()).isEqualTo("Transporte");
		assertThat(cap.getValue().getUsuario()).isEqualTo(usuario);
	}

	@Test
	void buscarPorNome_quandoEncontra_retornaLista() throws Exception {
		Categoria filtro = new Categoria();
		Categoria c = new Categoria();
		c.setId(1L);
		c.setNome("Saúde");
		when(repository.findAll(Mockito.<Example<Categoria>>any())).thenReturn(Collections.singletonList(c));

		assertThat(service.buscarPorNome(filtro)).containsExactly(c);
	}

	@Test
	void buscarPorNome_quandoNaoEncontra_lancaExcecao() throws Exception {
		when(repository.findAll(Mockito.<Example<Categoria>>any())).thenReturn(Collections.emptyList());
		Categoria filtro = new Categoria();

		Throwable t = catchThrowable(() -> service.buscarPorNome(filtro));
		assertThat(t).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Nenhum lançamento encontrado para este nome.");
	}

	@Test
	void validar_quandoDuplicado_lancaExcecao() throws Exception {
		Usuario u = new Usuario();
		u.setId(1L);
		Categoria c = new Categoria();
		c.setNome("Lazer");
		c.setUsuario(u);
		when(repository.findByNomeIgnoreCaseAndUsuario("Lazer", u)).thenReturn(Optional.of(new Categoria()));

		Throwable t = catchThrowable(() -> service.validar(c));
		assertThat(t).isInstanceOf(RegraNegocioException.class).hasMessage("Uma categoria com esse nome já existe");
	}

	@Test
	void validar_quandoNomeInvalido_lancaExcecao() throws Exception {
		Usuario u = new Usuario();
		u.setId(1L);
		Categoria c = new Categoria();
		c.setUsuario(u);
		c.setNome("  ");
		when(repository.findByNomeIgnoreCaseAndUsuario(anyString(), any())).thenReturn(Optional.empty());

		Throwable t = catchThrowable(() -> service.validar(c));
		assertThat(t).isInstanceOf(RegraNegocioException.class).hasMessage("Insira uma nome válido.");
	}

	@Test
	void validar_quandoValido_naoLanca() throws Exception {
		Usuario u = new Usuario();
		u.setId(1L);
		Categoria c = new Categoria();
		c.setUsuario(u);
		c.setNome("Educação");
		when(repository.findByNomeIgnoreCaseAndUsuario("Educação", u)).thenReturn(Optional.empty());

		assertThatCode(() -> service.validar(c)).doesNotThrowAnyException();
	}

	@Test
	void salvar_invocaValidarESalva() throws Exception {
		Usuario u = new Usuario();
		u.setId(1L);
		Categoria c = new Categoria();
		c.setUsuario(u);
		c.setNome("Viagem");
		when(repository.findByNomeIgnoreCaseAndUsuario("Viagem", u)).thenReturn(Optional.empty());
		Categoria salvo = new Categoria();
		salvo.setId(5L);
		salvo.setNome("Viagem");
		when(repository.save(c)).thenReturn(salvo);

		Categoria out = service.salvar(c);
		assertThat(out).isEqualTo(salvo);
		verify(repository).save(c);
	}

	@Test
	void atualizar_quandoNaoExiste_lancaExcecao() throws Exception {
		when(usuarioService.obterIdUsuarioPorEmail("usuario@teste.com")).thenReturn(new Usuario() {
			{
				setId(1L);
				setEmail("usuario@teste.com");
			}
		});
		when(repository.findByIdAndUsuario_Id(99L, 1L)).thenReturn(Optional.empty());

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Novo");
		Throwable t = catchThrowable(() -> service.atualizar(99L, authentication, dto));
		assertThat(t).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Nenhuma categoria foi encontrada para o ID fornecido");
	}

	@Test
	void atualizar_quandoNomeInvalido_lancaExcecao() throws Exception {
		Usuario u = new Usuario();
		u.setId(1L);
		u.setEmail("usuario@teste.com");
		when(usuarioService.obterIdUsuarioPorEmail("usuario@teste.com")).thenReturn(u);
		Categoria existente = new Categoria();
		existente.setId(3L);
		existente.setUsuario(u);
		existente.setNome("Antigo");
		when(repository.findByIdAndUsuario_Id(3L, 1L)).thenReturn(Optional.of(existente));

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome(" ");

		Throwable t = catchThrowable(() -> service.atualizar(3L, authentication, dto));
		assertThat(t).isInstanceOf(RegraNegocioException.class).hasMessage("Insira um nome válido.");
	}

	@Test
	void atualizar_sucesso_salvaEDepoisRetornaSalvo() throws Exception {
		Usuario u = new Usuario();
		u.setId(1L);
		u.setEmail("usuario@teste.com");
		when(usuarioService.obterIdUsuarioPorEmail("usuario@teste.com")).thenReturn(u);
		Categoria existente = new Categoria();
		existente.setId(3L);
		existente.setUsuario(u);
		existente.setNome("Antigo");
		when(repository.findByIdAndUsuario_Id(3L, 1L)).thenReturn(Optional.of(existente));
		when(repository.findByNomeIgnoreCaseAndUsuario(anyString(), any())).thenReturn(Optional.empty());
		when(repository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Novo");

		Categoria out = service.atualizar(3L, authentication, dto);

		assertThat(out.getNome()).isEqualTo("Novo");
		verify(repository, times(2)).save(any(Categoria.class));
	}

	@Test
	void converterDTO_mapeiaCampos() throws Exception {
		Usuario u = new Usuario();
		u.setId(2L);
		u.setEmail("usuario@teste.com");
		when(usuarioService.obterIdUsuarioPorEmail("usuario@teste.com")).thenReturn(u);

		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Casa");
		Categoria c = service.converterDTO(dto, authentication);

		assertThat(c.getNome()).isEqualTo("Casa");
		assertThat(c.getUsuario()).isEqualTo(u);
	}

	@Test
	void obterPorIdCategoria_repassaIdDoUsuario() throws Exception {
		Usuario u = new Usuario();
		u.setId(7L);
		u.setEmail("usuario@teste.com");
		when(usuarioService.obterIdUsuarioPorEmail("usuario@teste.com")).thenReturn(u);
		Categoria c = new Categoria();
		c.setId(15L);
		c.setUsuario(u);
		when(repository.findByIdAndUsuario_Id(15L, 7L)).thenReturn(Optional.of(c));

		Optional<Categoria> out = service.obterPorIdCategoria(15L, authentication);

		assertThat(out).contains(c);
	}

	@Test
	void deletar_quandoEmUso_lancaExcecaoComQuantidade() throws Exception {
		Usuario u = new Usuario();
		u.setId(1L);
		u.setEmail("usuario@teste.com");
		when(usuarioService.obterIdUsuarioPorEmail("usuario@teste.com")).thenReturn(u);
		Categoria c = new Categoria();
		c.setId(20L);
		c.setUsuario(u);
		c.setNome("X");
		when(repository.findByIdAndUsuario_Id(20L, 1L)).thenReturn(Optional.of(c));
		when(lancamentoRepository.existsByCategorias_Id(20L)).thenReturn(true);
		when(lancamentoRepository.countByCategorias_Id(20L)).thenReturn(3L);

		Throwable t = catchThrowable(() -> service.deletar(20L, authentication));
		assertThat(t).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Não é possível excluir: a categoria está vinculada a 3 lançamento(s).");
		verify(repository, never()).delete(any());
	}

	@Test
	void deletar_quandoNaoEmUso_exclui() throws Exception {
		Usuario u = new Usuario();
		u.setId(1L);
		u.setEmail("usuario@teste.com");
		when(usuarioService.obterIdUsuarioPorEmail("usuario@teste.com")).thenReturn(u);
		Categoria c = new Categoria();
		c.setId(21L);
		c.setUsuario(u);
		c.setNome("Y");
		when(repository.findByIdAndUsuario_Id(21L, 1L)).thenReturn(Optional.of(c));
		when(lancamentoRepository.existsByCategorias_Id(21L)).thenReturn(false);

		service.deletar(21L, authentication);

		verify(repository).delete(c);
	}

}
