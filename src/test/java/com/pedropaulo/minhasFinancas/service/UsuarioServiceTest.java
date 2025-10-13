package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.repository.UsuarioRepository;
import com.pedropaulo.minhasFinancas.service.UsuarioService;
import com.pedropaulo.minhasFinancas.service.impl.UsuarioServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class UsuarioServiceTest {
        UsuarioService service;
        UsuarioRepository repository;

        @Before
        public void setUp(){
            repository = Mockito.mock(UsuarioRepository.class);
            service = new UsuarioServiceImpl(repository);
        }


        @Test(expected = Test.None.class)
        public void deveValidarEmail() throws RegraNegocioException {
            Mockito.when(repository.existsByEmail("email@email.com")).thenReturn(false);
            service.validarEmail("email@email.com");
        }

        @Test(expected = RegraNegocioException.class)
        public void deveValidarEmailRetornaErro() throws RegraNegocioException {
            Mockito.when(repository.existsByEmail(Mockito.anyString())).thenReturn(true);
            service.validarEmail("email@email.com");
        }


}
