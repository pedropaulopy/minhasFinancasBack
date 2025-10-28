package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.exception.AutenticacaoException;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.repository.UsuarioRepository;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class UsuarioServiceTest {
        @SuppressWarnings("removal")
        @SpyBean
        UsuarioService service;
        @SuppressWarnings("removal")
        @MockBean
        UsuarioRepository repository;

//        @Before
//        public void setUp(){
//            repository = Mockito.mock(UsuarioRepository.class);
//            service = new UsuarioServiceImpl(repository);
//        }

        @Test
        public void deveValidarEmail() throws RegraNegocioException {
            Mockito.when(repository.existsByEmail("email@email.com")).thenReturn(false);
            service.validarEmail("email@email.com");
        }

        @Test
        @Disabled
        public void deveValidarEmailRetornaErro() throws RegraNegocioException {
            Mockito.when(repository.existsByEmail(Mockito.anyString())).thenReturn(true);
            service.validarEmail("email@email.com");
        }

    @Test
    public void deveSalvarUsuario() throws RegraNegocioException {
        Mockito.doNothing().when(service).validarEmail(Mockito.anyString());

        Usuario usuarioSalvo = Usuario.builder()
                .id(1L)
                .nome("nome")
                .email("email@email.com")
                .senha("senha-criptografada")
                .build();

        Mockito.when(repository.save(Mockito.any(Usuario.class))).thenReturn(usuarioSalvo);

        Usuario usuarioParaSalvar = Usuario.builder()
                .nome("nome")
                .email("email@email.com")
                .senha("senha123") // senha em texto puro para ser criptografada no método real
                .build();

        Usuario result = service.salvarUsuario(usuarioParaSalvar);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(1L);
        Assertions.assertThat(result.getNome()).isEqualTo("nome");
        Assertions.assertThat(result.getEmail()).isEqualTo("email@email.com");
        Assertions.assertThat(result.getSenha()).isNotEqualTo("senha123"); // deve ter sido criptografada
    }


    @Test
    public void deveAutenticarComSucesso() throws RegraNegocioException {
        String email = "email@emai.com";
        String senha = "123";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String senhaCriptografada = encoder.encode(senha);

        Usuario usuario = Usuario.builder()
                .id(1L)
                .email(email)
                .senha(senhaCriptografada)
                .build();

        Mockito.when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));

        Usuario result = service.autenticar(email, senha);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getEmail()).isEqualTo(email);
        Assertions.assertThat(encoder.matches(senha, result.getSenha())).isTrue();
    }

        @Disabled
        @Test
        public void naoDeveSalvarUsuarioComEmailJaCadastrado() throws RegraNegocioException {
            String email = "email@email.com";
            Usuario usuario = Usuario.builder().email(email).build();
            Mockito.doThrow(RegraNegocioException.class).when(service).validarEmail(email);

            service.salvarUsuario(usuario);

            Mockito.verify(repository, Mockito.never()).save(usuario);

        }

        @Disabled
        @Test
        public void deveLancarErroQuandoNaoExistirUsuarioCadastradoComOEmailInformado() throws RegraNegocioException {
            Mockito.when(repository.findByEmail(Mockito.anyString())).thenReturn(Optional.empty());
            service.autenticar("email.com", "123");
        }

        @Disabled
        @Test
        public void deveLancarErroQuandoExistirUsuarioCadastradoComOEmailInformadoMasSenhaErrada() throws RegraNegocioException {
            String email = "email@emai.com";
            String senha = "123";
            Usuario usuario = Usuario.builder().email(email).senha(senha).id(1L).build();
            Mockito.when(repository.findByEmail(Mockito.anyString())).thenReturn(Optional.of(usuario));
            service.autenticar("email.com", "456");
        }
}
