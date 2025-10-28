package com.pedropaulo.minhasFinancas.model.repository;

import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)@ActiveProfiles("test")
public class UsuarioRepositoryTest {
    @Autowired
    UsuarioRepository repository;

    @Autowired
    TestEntityManager entityManager;

    public static Usuario criaUsuario() {
        return Usuario.builder()
                .nome("usuario")
                .email("email@email.com")
                .senha("123")
                .build();
    }

    @Test
    public void verificaExistenciaEmail() {
        Usuario usuario = criaUsuario();
        entityManager.persist(usuario);

        boolean exists = repository.existsByEmail("email@email.com");

        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void retornaFalsoQuandoNaoHouverUsuarioCadastradoComEmail() {

        boolean exists = repository.existsByEmail("email@email.com");

        Assertions.assertThat(exists).isFalse();
    }

    @Test
    public void devePersistirUsuarioNaBaseDeDados() {
        Usuario usuario = criaUsuario();
        Usuario usuarioSalvo = entityManager.persist(usuario);
        Assertions.assertThat(usuarioSalvo.getId()).isNotNull();
    }

    @Test
    public void deveBuscarUsuarioPorEmail() {
        Usuario usuario = criaUsuario();
        Usuario usuarioSalvo = entityManager.persist(usuario);
        Assertions.assertThat(repository.findByEmail("email@email.com")).isPresent();
    }

    @Test
    public void deveRetornarVazioQuandoUsuarioNaoExistirPorEmail() {
        Optional<Usuario> exists = repository.findByEmail("email@email.com");
        Assertions.assertThat(exists.isPresent()).isFalse();
    }
}
