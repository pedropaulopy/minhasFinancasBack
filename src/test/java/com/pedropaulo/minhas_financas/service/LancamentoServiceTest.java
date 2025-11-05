package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.api.dto.CategoriaDTO;
import com.pedropaulo.minhas_financas.api.dto.LancamentoDTO;
import com.pedropaulo.minhas_financas.api.dto.LancamentoDTOFactory;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import com.pedropaulo.minhas_financas.service.impl.LancamentoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static com.pedropaulo.minhas_financas.service.testUtils.AuthMocks.auth;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class LancamentoServiceTest {

    @Mock
    LancamentoRepository repository;

    @Mock
    UsuarioService usuarioService;

    @Mock
    CategoriaService categoriaService;

    LancamentoServiceImpl service;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        service = new LancamentoServiceImpl(repository, usuarioService, categoriaService);
    }

    private Usuario criarUsuario(Long id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        return usuario;
    }

    private Lancamento lancamentoValido(Usuario usuario) {
        return Lancamento.builder()
                .ano(2025)
                .mes(11)
                .descricao("Lançamento teste")
                .valor(BigDecimal.valueOf(100))
                .tipoLancamento(TipoLancamento.DESPESA)
                .statusLancamento(StatusLancamento.PENDENTE)
                .dataCadastro(LocalDate.now())
                .usuario(usuario)
                .build();
    }

    @Test
    void deveAtualizarUmLancamento() throws Exception {
        Long id = 1L;
        String email = "usuario@teste.com";
        authentication = auth(email);
        Usuario usuario = criarUsuario(10L, email);

        Lancamento existente = lancamentoValido(usuario);
        existente.setId(id);

        LancamentoDTO dto = LancamentoDTOFactory.create(id, "Lançamento teste", 11, 2025,
                BigDecimal.valueOf(100), TipoLancamento.DESPESA.name(), StatusLancamento.PENDENTE.name());
        dto.setCategorias(Collections.emptyList());

        given(usuarioService.obterIdUsuarioPorEmail(email)).willReturn(usuario);
        given(repository.findLancamentoByIdAndUsuarioId(id, usuario.getId())).willReturn(Optional.of(existente));
        given(repository.save(existente)).willReturn(existente);

        Lancamento atualizado = service.atualizar(id, authentication, dto);

        then(repository).should().save(existente);
        assertThat(atualizado).isEqualTo(existente);
    }

}
