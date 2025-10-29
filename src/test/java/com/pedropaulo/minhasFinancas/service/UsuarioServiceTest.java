package com.pedropaulo.minhasFinancas.service;

import com.pedropaulo.minhasFinancas.exception.AutenticacaoException;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.repository.UsuarioRepository;
import java.util.Optional;

import com.pedropaulo.minhasFinancas.service.impl.UsuarioServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class UsuarioServiceTest {
        UsuarioServiceImpl service; // Testando a implementação
        @Mock
        UsuarioRepository repository;

        @Mock
        PasswordEncoder passwordEncoder;

        @BeforeEach
        public void setUp(){
        service = Mockito.spy(new UsuarioServiceImpl(repository, passwordEncoder));
        }

        @Test
        public void deveValidarEmail() throws RegraNegocioException {
            Mockito.when(repository.existsByEmail("email@email.com")).thenReturn(false);
            service.validarEmail("email@email.com");
        }

        @Test
        public void deveValidarEmailRetornaErro(){
            Mockito.when(repository.existsByEmail(Mockito.anyString())).thenReturn(true);
            Throwable erro = Assertions.catchThrowable(() -> service.validarEmail("email@email.com"));

            Assertions.assertThat(erro)
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Um usuário já foi cadastrado com este email.");
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
                .senha("senha123")
                .build();

        Usuario result = service.salvarUsuario(usuarioParaSalvar);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(1L);
        Assertions.assertThat(result.getNome()).isEqualTo("nome");
        Assertions.assertThat(result.getEmail()).isEqualTo("email@email.com");
        Assertions.assertThat(result.getSenha()).isNotEqualTo("senha123");
    }


    @Test
    public void deveAutenticarComSucesso() throws RegraNegocioException {
        String email = "email@email.com";
        String senha = "123";
        String senhaDoBanco = "hash-salvo-no-banco";

        Usuario usuario = Usuario.builder()
                .id(1L)
                .email(email)
                .senha(senhaDoBanco)
                .build();

        Mockito.when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));

        Mockito.when(passwordEncoder.matches(senha, senhaDoBanco)).thenReturn(true);

        Usuario result = service.autenticar(email, senha);


        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getEmail()).isEqualTo(email);
    }

        @Test
        public void naoDeveSalvarUsuarioComEmailJaCadastrado() throws RegraNegocioException {
            String email = "email@email.com";
            Usuario usuario = Usuario.builder().email(email).build();
            Mockito.doThrow(RegraNegocioException.class).when(service).validarEmail(email);

            Throwable erro = Assertions.catchThrowable(() -> service.salvarUsuario(usuario));

            Assertions.assertThat(erro).isInstanceOf(RegraNegocioException.class);
            Mockito.verify(repository, Mockito.never()).save(usuario);

        }


        @Test
        public void deveLancarErroQuandoNaoExistirUsuarioCadastradoComOEmailInformado(){
            Mockito.when(repository.findByEmail(Mockito.anyString())).thenReturn(Optional.empty());
            Throwable erro = Assertions.catchThrowable(() -> service.autenticar("email.com", "123"));

            Assertions.assertThat(erro)
                    .isInstanceOf(AutenticacaoException.class)
                    .hasMessage("Usuário não encontrado para o email informado.");
        }

        @Test
        public void deveLancarErroQuandoExistirUsuarioCadastradoComOEmailInformadoMasSenhaErrada() throws RegraNegocioException {
            String email = "email@emai.com";
            String senhaCorreta = "123";
            String senhaDigitadaErrada = "456";

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String senhaCriptografada = encoder.encode(senhaCorreta);

            Usuario usuario = Usuario.builder()
                    .email(email)
                    .senha(senhaCriptografada)
                    .id(1L).build();

            Mockito.when(repository.findByEmail(email)).thenReturn(Optional.of(usuario));

            Throwable erro = Assertions.catchThrowable(() -> service.autenticar(email, senhaDigitadaErrada));

            Assertions.assertThat(erro)
                    .isInstanceOf(AutenticacaoException.class)
                    .hasMessage("Senha inválida.");
        }

    @Test
    public void deveObterUsuarioPorId() throws RegraNegocioException {
        Long id = 1L;
        Usuario usuarioMock = Usuario.builder()
                .id(id)
                .nome("usuario teste")
                .email("teste@email.com")
                .senha("123")
                .build();

        Mockito.when(repository.findById(id)).thenReturn(Optional.of(usuarioMock));

        Optional<Usuario> result = service.obterPorId(id);

        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get()).isEqualTo(usuarioMock);
        Mockito.verify(repository, Mockito.times(2)).findById(id);
    }

    @Test
    public void deveLancarErroAoNaoEncontrarUsuarioPorId() {
        Long id = 1L;
        Mockito.when(repository.findById(id)).thenReturn(Optional.empty());

        Throwable erro = Assertions.catchThrowable(() -> service.obterPorId(id));

        Assertions.assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Usuário não encontrado para o ID informado.");

        Mockito.verify(repository, Mockito.times(1)).findById(id);
    }

    @Test
    public void deveLancarErroAoNaoEncontrarUsuarioPorEmail() {
        Long id = 1L;
        Usuario usuarioMock = Usuario.builder()
                .id(id)
                .nome("usuario teste")
                .email("teste@email.com")
                .senha("123")
                .build();

    Throwable erro =
        Assertions.catchThrowable(() -> service.obterIdUsuarioPorEmail(usuarioMock.getEmail()));

        Assertions.assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Usuário não encontrado para o email informado");

    Mockito.verify(repository, Mockito.times(1)).findByEmail(usuarioMock.getEmail());
    }
}
