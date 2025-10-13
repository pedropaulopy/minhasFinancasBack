package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.exception.AutenticacaoException;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.repository.UsuarioRepository;
import com.pedropaulo.minhasFinancas.service.UsuarioService;
import com.pedropaulo.minhasFinancas.service.impl.UsuarioServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class UsuarioServiceTest {
        @SuppressWarnings("removal")
        @SpyBean
        UsuarioService service;
        @SuppressWarnings("removal")
        @MockBean
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

        @Test
        public void deveSalvarUsuario() throws RegraNegocioException {
            Mockito.doNothing().when(service).validarEmail(Mockito.anyString());
            Usuario usuario = Usuario.builder().id(1L).nome("nome").email("email@email.com").senha("senha").build();
            Mockito.when(repository.save(Mockito.any(Usuario.class))).thenReturn(usuario);
            Usuario usuarioSalvo = service.salvarUsuario(new Usuario());
            Assertions.assertThat(usuarioSalvo).isNotNull();
            Assertions.assertThat(usuarioSalvo.getId()).isEqualTo(1L);
            Assertions.assertThat(usuarioSalvo.getNome()).isEqualTo("nome");
            Assertions.assertThat(usuarioSalvo.getEmail()).isEqualTo("email@email.com");
            Assertions.assertThat(usuarioSalvo.getSenha()).isEqualTo("senha");

        }

        @Test(expected = Test.None.class)
        public void deveAutenticarComSucesso() throws RegraNegocioException {
            String email = "email@emai.com";
            String senha = "123";
            Usuario usuario = Usuario.builder().email(email).senha(senha).id(1L).build();
            Mockito.when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));

            Usuario result = service.autenticar(email, senha);

            Assertions.assertThat(result).isNotNull();
        }

        @Test
        public void naoDeveSalvarUsuarioComEmailJaCadastrado() throws RegraNegocioException {
            String email = "email@email.com";
            Usuario usuario = Usuario.builder().email(email).build();
            Mockito.doThrow(RegraNegocioException.class).when(service).validarEmail(email);

            service.salvarUsuario(usuario);

            Mockito.verify(repository, Mockito.never()).save(usuario);

        }

        @Test(expected = AutenticacaoException.class)
        public void deveLancarErroQuandoNaoExistirUsuarioCadastradoComOEmailInformado() throws RegraNegocioException {
            Mockito.when(repository.findByEmail(Mockito.anyString())).thenReturn(Optional.empty());
            service.autenticar("email.com", "123");
        }

        @Test(expected = AutenticacaoException.class)
        public void deveLancarErroQuandoExistirUsuarioCadastradoComOEmailInformadoMasSenhaErrada() throws RegraNegocioException {
            String email = "email@emai.com";
            String senha = "123";
            Usuario usuario = Usuario.builder().email(email).senha(senha).id(1L).build();
            Mockito.when(repository.findByEmail(Mockito.anyString())).thenReturn(Optional.of(usuario));
            service.autenticar("email.com", "456");
        }
}
