package com.pedropaulo.minhas_financas.api.resource;

import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.model.repository.UsuarioRepository;
import com.pedropaulo.minhas_financas.service.CategoriaService;
import com.pedropaulo.minhas_financas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaResource {

    private final CategoriaRepository categoriaRepository;
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

        List<Categoria> categorias = categoriaService.buscarPorNome(categoriaFiltro);
        return  ResponseEntity.ok(categorias);
    }
}
