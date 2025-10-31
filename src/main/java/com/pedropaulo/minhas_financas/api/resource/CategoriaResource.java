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

    @GetMapping
    public ResponseEntity<List<Categoria>> buscar(@RequestParam(value = "nomeCategoria", required = false) String nomeCategoria, Authentication authentication) throws RegraNegocioException {
        String email = authentication.getName();
        Usuario usuario = usuarioService.obterIdUsuarioPorEmail(email);
        Categoria categoriaFiltro = new Categoria();
        categoriaFiltro.setUsuario(usuario);

        if (nomeCategoria != null && !nomeCategoria.isEmpty()) {
            categoriaFiltro.setNome(nomeCategoria);
        }
        try{
            List<Categoria> categorias = categoriaService.buscarPorNome(categoriaFiltro);
            return ResponseEntity.ok(categorias);
        }catch (RegraNegocioException e){
            return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/criar")
    public ResponseEntity criar(@RequestBody CategoriaDTO dto, Authentication authentication){
        try{
            Categoria entidade = categoriaService.converterDTO(dto, authentication);
            entidade = categoriaService.salvar(entidade);
            return new ResponseEntity(entidade, HttpStatus.CREATED);
        }catch (RegraNegocioException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
