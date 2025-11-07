package com.pedropaulo.minhas_financas.resource;

import com.pedropaulo.minhas_financas.api.dto.TokenDTO;
import com.pedropaulo.minhas_financas.api.dto.UsuarioDTO;
import com.pedropaulo.minhas_financas.exception.AutenticacaoException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import com.pedropaulo.minhas_financas.service.JwtService;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioResourceTest {

	private final UsuarioService service;

	private final LancamentoService lancamentoService;

	private final JwtService jwtService; // use a interface para compatibilidade com o
											// SecurityConfig e testes

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Usuario> salvar(@RequestBody UsuarioDTO dto) {
		Usuario usuario = Usuario.builder().nome(dto.getNome()).email(dto.getEmail()).senha(dto.getSenha()).build();
		try {
			Usuario usuarioSalvo = service.salvarUsuario(usuario);
			return ResponseEntity.status(HttpStatus.CREATED).body(usuarioSalvo);
		}
		catch (RegraNegocioException error) {
			return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(null);
		}
	}

	@PostMapping(path = "/autenticar", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> autenticar(@RequestBody UsuarioDTO dto) {
		try {
			Usuario usuarioAutenticado = service.autenticar(dto.getEmail(), dto.getSenha());
			String token = jwtService.gerarToken(usuarioAutenticado);
			TokenDTO tokenDTO = new TokenDTO(usuarioAutenticado.getNome(), token);
			return ResponseEntity.ok(tokenDTO);
		}
		catch (AutenticacaoException | RegraNegocioException error) {
			// Os testes esperam texto puro com a mensagem do erro e HTTP 400
			return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(error.getMessage());
		}
	}

	// id que vem da autenticacao eh usado no lugar da url
	@GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> obterSaldo(Authentication authentication) {
		try {
			String email = authentication.getName();
			Usuario usuario = service.obterIdUsuarioPorEmail(email);
			Long idUsuario = usuario.getId();
			BigDecimal saldo = lancamentoService.obterSaldoPorUsuario(idUsuario);

			// Os testes validam "1000" como texto; garantir string sem casas decimais
			// desnecessárias
			String body = saldo.stripTrailingZeros().toPlainString();
			return ResponseEntity.ok(body);
		}
		catch (RegraNegocioException e) {
			// Os testes esperam 404 e mensagem em texto puro
			return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
		}
	}

}
