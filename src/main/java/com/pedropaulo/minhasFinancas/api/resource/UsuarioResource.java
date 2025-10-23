package com.pedropaulo.minhasFinancas.api.resource;

import com.pedropaulo.minhasFinancas.api.dto.TokenDTO;
import com.pedropaulo.minhasFinancas.api.dto.UsuarioDTO;
import com.pedropaulo.minhasFinancas.exception.AutenticacaoException;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.service.JwtService;
import com.pedropaulo.minhasFinancas.service.LancamentoService;
import com.pedropaulo.minhasFinancas.service.UsuarioService;
import com.pedropaulo.minhasFinancas.service.impl.JwtServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", methods = {RequestMethod.GET, RequestMethod.POST})
public class UsuarioResource {
    private final UsuarioService service;
    private final LancamentoService lancamentoService;
    private final JwtServiceImpl jwtService;

    @PostMapping
    public ResponseEntity salvar (@RequestBody UsuarioDTO dto){
        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senha(dto.getSenha()).build();
        try{
            Usuario usuarioSalvo = service.salvarUsuario(usuario);
            return new ResponseEntity(usuarioSalvo, HttpStatus.CREATED);
        }catch (RegraNegocioException error){
            return ResponseEntity.badRequest().body(error.getMessage());
        }
    }

    @PostMapping("/autenticar")
    public ResponseEntity<?> autenticar(@RequestBody UsuarioDTO dto){
        try{
            Usuario usuarioAutenticado = service.autenticar(dto.getEmail(), dto.getSenha());
            String token = jwtService.gerarToken(usuarioAutenticado);
            TokenDTO tokenDTO = new TokenDTO(usuarioAutenticado.getNome(), token);
            return new ResponseEntity(tokenDTO, HttpStatus.OK);
        }catch (AutenticacaoException | RegraNegocioException error){
            return ResponseEntity.badRequest().body(error.getMessage());
        }
    }

    @GetMapping("saldo/{id}")
    public ResponseEntity obterSaldo(@PathVariable("id") Long idUsuario) throws RegraNegocioException {
        Optional<Usuario> usuario = service.obterPorId(idUsuario);
        if(!usuario.isPresent()){
            return new ResponseEntity("Usuário não encontrado para o ID informado.", HttpStatus.NOT_FOUND);
        }
        BigDecimal saldo = lancamentoService.obterSaldoPorUsuario(idUsuario);
        return ResponseEntity.ok(saldo);
    }
}
