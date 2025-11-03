package com.pedropaulo.minhas_financas.api.resource;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.model.repository.UsuarioRepository;
import com.pedropaulo.minhas_financas.service.CategoriaService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaResource {

	private final UsuarioService usuarioService;

	private final CategoriaService categoriaService;

	@GetMapping()
	public ResponseEntity<List<Categoria>> buscar(
			@RequestParam(value = "nomeCategoria", required = false) String nomeCategoria,
			Authentication authentication) throws RegraNegocioException {
		String email = authentication.getName();
		Usuario usuario = usuarioService.obterIdUsuarioPorEmail(email);
		Categoria categoriaFiltro = new Categoria();
		categoriaFiltro.setUsuario(usuario);

		try {
			List<Categoria> categorias = categoriaService.buscarPorNome(categoriaFiltro);
			return ResponseEntity.ok(categorias);
		}
		catch (RegraNegocioException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<Categoria> obterPorId(@PathVariable("id") Long id, Authentication authentication) {
		try {
			return categoriaService.obterPorIdCategoria(id, authentication)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
		}
		catch (RegraNegocioException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@PostMapping
	public ResponseEntity criar(@RequestBody CategoriaDTO dto, Authentication authentication, Categoria categoria) {
		try {
			Categoria entidade = categoriaService.salvar(dto, authentication);
			return ResponseEntity.ok(entidade);
		}
		catch (RegraNegocioException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity atualizar(@RequestBody CategoriaDTO dto, Authentication authentication,
			@PathVariable("id") Long id) throws RegraNegocioException {
		try {
			categoriaService.atualizar(id, authentication, dto);
			return new ResponseEntity(HttpStatus.CREATED);
		}
		catch (RegraNegocioException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity deletar(@PathVariable Long id, Authentication authentication) throws RegraNegocioException {
		try {
			categoriaService.deletar(id, authentication);
			return new ResponseEntity(HttpStatus.NO_CONTENT);
		}
		catch (RegraNegocioException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

}