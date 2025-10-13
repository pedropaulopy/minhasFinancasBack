package com.pedropaulo.minhasFinancas.model.repository;

import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.repository.UsuarioRepository;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class UsuarioRepositoryTest {
    @Autowired
    UsuarioRepository repository;

    @Test
    public void verificaExistenciaEmail() {
        Usuario usuario = Usuario.builder().nome("usuario").email("usuario@email.com").build();
        repository.save(usuario);

        boolean exists = repository.existsByEmail("usuario@email.com");

        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void retornaFalsoQuandoNaoHouverUsuarioCadastradoComEmail() {
        repository.deleteAll();

        boolean exists = repository.existsByEmail("usuario@email.com");

        Assertions.assertThat(exists).isFalse();
    }
}
