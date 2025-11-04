package com.pedropaulo.minhas_financas.api.resource;

import com.pedropaulo.minhas_financas.api.dto.TokenDTO;
import com.pedropaulo.minhas_financas.api.dto.UsuarioDTO;
import com.pedropaulo.minhas_financas.exception.AutenticacaoException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import com.pedropaulo.minhas_financas.service.impl.JwtServiceImpl;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioResource {

	private final UsuarioService service;

	private final LancamentoService lancamentoService;

	private final JwtServiceImpl jwtService;

	@PostMapping
	public ResponseEntity salvar(@RequestBody UsuarioDTO dto) {
		Usuario usuario = Usuario.builder().nome(dto.getNome()).email(dto.getEmail()).senha(dto.getSenha()).build();
		try {
			Usuario usuarioSalvo = service.salvarUsuario(usuario);
			return new ResponseEntity(usuarioSalvo, HttpStatus.CREATED);
		}
		catch (RegraNegocioException error) {
			return ResponseEntity.badRequest().body(error.getMessage());
		}
	}

	@PostMapping("/autenticar")
	public ResponseEntity<?> autenticar(@RequestBody UsuarioDTO dto) {
		try {
			Usuario usuarioAutenticado = service.autenticar(dto.getEmail(), dto.getSenha());
			String token = jwtService.gerarToken(usuarioAutenticado);
			TokenDTO tokenDTO = new TokenDTO(usuarioAutenticado.getNome(), token);
			return new ResponseEntity(tokenDTO, HttpStatus.OK);
		}
		catch (AutenticacaoException | RegraNegocioException error) {
			return ResponseEntity.badRequest().body(error.getMessage());
		}
	}

	// id que vem da autenticacao eh usado no lugar da url
	@GetMapping
	public ResponseEntity obterSaldo(Authentication authtentication) {

		try {
			String email = authtentication.getName();
			Usuario usuario = service.obterIdUsuarioPorEmail(email);
			Long idUsuario = usuario.getId();
			BigDecimal saldo = lancamentoService.obterSaldoPorUsuario(idUsuario);
			return new ResponseEntity(saldo, HttpStatus.OK);
		}
		catch (RegraNegocioException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

}