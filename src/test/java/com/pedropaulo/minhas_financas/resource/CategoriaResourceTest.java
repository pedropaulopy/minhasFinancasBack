package com.pedropaulo.minhas_financas.resource;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.api.resource.CategoriaResource;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.service.CategoriaService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class CategoriaResourceTest {

	private static final String EMAIL = "usuario@teste.com";

	@Mock
	private UsuarioService usuarioService;

	@Mock
	private CategoriaService categoriaService;

	@Mock
	private Authentication authentication;

	private CategoriaResource resource;

	@BeforeEach
	void setUp() {
		resource = new CategoriaResource(usuarioService, categoriaService);
	}

	@Test
	void buscar_semNomeCategoria_retornaListaOk() throws Exception {
		when(authentication.getName()).thenReturn(EMAIL);
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuario);

		Categoria categoria1 = new Categoria();
		categoria1.setId(10L);
		categoria1.setNome("Alimentação");
		categoria1.setUsuario(usuario);
		Categoria categoria2 = new Categoria();
		categoria2.setId(11L);
		categoria2.setNome("Transporte");
		categoria2.setUsuario(usuario);
		when(categoriaService.buscarPorNome(any(Categoria.class))).thenReturn(Arrays.asList(categoria1, categoria2));

		ResponseEntity<List<Categoria>> resp = resource.buscar(null, authentication);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resp.getBody()).containsExactly(categoria1, categoria2);

		ArgumentCaptor<Categoria> cap = ArgumentCaptor.forClass(Categoria.class);
		verify(categoriaService).buscarPorNome(cap.capture());
		assertThat(cap.getValue().getUsuario()).isEqualTo(usuario);
		assertThat(cap.getValue().getNome()).isNull();
	}

	@Test
	void buscar_comNomeCategoria_retornaListaOk() throws Exception {
		when(authentication.getName()).thenReturn(EMAIL);
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuario);
		when(categoriaService.buscarPorNome(any(Categoria.class))).thenReturn(Collections.emptyList());

		ResponseEntity<List<Categoria>> resp = resource.buscar("Mercado", authentication);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resp.getBody()).isEmpty();

		ArgumentCaptor<Categoria> cap = ArgumentCaptor.forClass(Categoria.class);
		verify(categoriaService).buscarPorNome(cap.capture());
		assertThat(cap.getValue().getUsuario()).isEqualTo(usuario);
		assertThat(cap.getValue().getNome()).isEqualTo("Mercado");
	}

	@Test
	void buscar_quandoServiceLancaRegraNegocio_retorna404ComMensagem() throws Exception {
		when(authentication.getName()).thenReturn(EMAIL);
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(EMAIL);
		when(usuarioService.obterIdUsuarioPorEmail(EMAIL)).thenReturn(usuario);
		when(categoriaService.buscarPorNome(any(Categoria.class)))
			.thenThrow(new RegraNegocioException("erro qualquer"));

		ResponseEntity<?> resp = resource.buscar("x", authentication);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(resp.getBody()).isEqualTo("erro qualquer");
	}

	@Test
	void obterPorId_quandoExiste_retorna200ComCorpo() throws Exception {
		Categoria cat = new Categoria();
		cat.setId(99L);
		cat.setNome("Lazer");
		when(categoriaService.obterPorIdCategoria(eq(99L), eq(authentication))).thenReturn(Optional.of(cat));

		ResponseEntity<Categoria> resp = resource.obterPorId(99L, authentication);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resp.getBody()).isEqualTo(cat);
	}

	@Test
	void obterPorId_quandoNaoExiste_retorna404() throws Exception {
		when(categoriaService.obterPorIdCategoria(eq(100L), eq(authentication))).thenReturn(Optional.empty());
		ResponseEntity<Categoria> resp = resource.obterPorId(100L, authentication);
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(resp.getBody()).isNull();
	}

	@Test
	void obterPorId_quandoServiceLanca_retorna400() throws Exception {
		when(categoriaService.obterPorIdCategoria(anyLong(), eq(authentication)))
			.thenThrow(new RegraNegocioException("falhou"));
		ResponseEntity<Categoria> resp = resource.obterPorId(1L, authentication);
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resp.getBody()).isNull();
	}

	@Test
	void criar_sucesso_retorna200ComEntidade() throws Exception {
		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Saúde");
		Categoria convertido = new Categoria();
		convertido.setNome("Saúde");
		Categoria salvo = new Categoria();
		salvo.setId(7L);
		salvo.setNome("Saúde");

		when(categoriaService.converterDTO(eq(dto), eq(authentication))).thenReturn(convertido);
		when(categoriaService.salvar(eq(convertido))).thenReturn(salvo);

		ResponseEntity<?> resp = resource.criar(dto, authentication);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resp.getBody()).isEqualTo(salvo);
		verify(categoriaService).converterDTO(dto, authentication);
		verify(categoriaService).salvar(convertido);
	}

	@Test
	void criar_quandoServiceLanca_retorna400ComMensagem() throws Exception {
		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Saúde");
		when(categoriaService.converterDTO(eq(dto), eq(authentication)))
			.thenThrow(new RegraNegocioException("categoria inválida"));

		ResponseEntity<?> resp = resource.criar(dto, authentication);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resp.getBody()).isEqualTo("categoria inválida");
		verify(categoriaService, never()).salvar(any());
	}

	@Test
	void atualizar_sucesso_retorna201Created() throws Exception {
		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Investimentos");

		when(categoriaService.atualizar(eq(5L), eq(authentication), eq(dto))).thenReturn(new Categoria()); // ou
																											// null,
																											// se
																											// você
																											// preferir

		ResponseEntity<?> resp = resource.atualizar(dto, authentication, 5L);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(resp.getBody()).isNull();
		verify(categoriaService).atualizar(5L, authentication, dto);
	}

	@Test
	void atualizar_quandoServiceLanca_retorna400ComMensagem() throws Exception {
		CategoriaDTO dto = new CategoriaDTO();
		dto.setNome("Investimentos");
		doThrow(new RegraNegocioException("erro atualização")).when(categoriaService)
			.atualizar(eq(6L), eq(authentication), eq(dto));

		ResponseEntity<?> resp = resource.atualizar(dto, authentication, 6L);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resp.getBody()).isEqualTo("erro atualização");
	}

	@Test
	void deletar_sucesso_retorna204NoContent() throws Exception {
		doNothing().when(categoriaService).deletar(eq(9L), eq(authentication));
		ResponseEntity<?> resp = resource.deletar(9L, authentication);
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(resp.getBody()).isNull();
		verify(categoriaService).deletar(9L, authentication);
	}

	@Test
	void deletar_quandoServiceLanca_retorna400ComMensagem() throws Exception {
		doThrow(new RegraNegocioException("nao pode excluir")).when(categoriaService)
			.deletar(eq(12L), eq(authentication));
		ResponseEntity<?> resp = resource.deletar(12L, authentication);
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resp.getBody()).isEqualTo("nao pode excluir");
	}

}
