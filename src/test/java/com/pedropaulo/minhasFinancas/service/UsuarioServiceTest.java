package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.repository.UsuarioRepository;
import com.pedropaulo.minhasFinancas.service.UsuarioService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class UsuarioServiceTest {
        @Autowired
        UsuarioService service;

        @Autowired
        UsuarioRepository repository;

        @Test(expected = Test.None.class)
        public void deveValidarEmail() throws RegraNegocioException {
            repository.deleteAll();
            service.validarEmail("email@email.com");
        }

        @Test(expected = RegraNegocioException.class)
        public void deveValidarEmailRetornaErro() throws RegraNegocioException {
            Usuario usuario = Usuario.builder().nome("Pedro").email("email@email.com").build();
            repository.save(usuario);
            service.validarEmail("email@email.com");
        }
}
