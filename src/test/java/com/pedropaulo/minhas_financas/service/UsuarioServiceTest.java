package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.exception.AutenticacaoException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.UsuarioRepository;
import com.pedropaulo.minhas_financas.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class UsuarioServiceTest {

	@Mock
	UsuarioRepository repository;

	@Mock
	PasswordEncoder passwordEncoder;

	UsuarioServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new UsuarioServiceImpl(repository, passwordEncoder);
	}

	@Test
	void deveValidarEmail() throws Exception {
		given(repository.existsByEmail("email@email.com")).willReturn(false);
		service.validarEmail("email@email.com");
		then(repository).should().existsByEmail("email@email.com");
	}

	@Test
	void deveValidarEmailRetornaErro() {
		given(repository.existsByEmail(anyString())).willReturn(true);

		Throwable erro = catchThrowable(() -> service.validarEmail("email@email.com"));

		assertThat(erro).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Um usuário já foi cadastrado com este email.");
		then(repository).should().existsByEmail("email@email.com");
	}

	@Test
	void deveSalvarUsuario() throws Exception {
		given(repository.existsByEmail("email@email.com")).willReturn(false);
		given(passwordEncoder.encode("senha123")).willReturn("senha-criptografada");

		Usuario usuarioParaSalvar = Usuario.builder().nome("nome").email("email@email.com").senha("senha123").build();

		Usuario usuarioSalvo = Usuario.builder()
			.id(1L)
			.nome("nome")
			.email("email@email.com")
			.senha("senha-criptografada")
			.build();

		given(repository.save(any(Usuario.class))).willReturn(usuarioSalvo);

		Usuario result = service.salvarUsuario(usuarioParaSalvar);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getNome()).isEqualTo("nome");
		assertThat(result.getEmail()).isEqualTo("email@email.com");
		assertThat(result.getSenha()).isEqualTo("senha-criptografada");

		ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
		then(repository).should().save(cap.capture());
		assertThat(cap.getValue().getSenha()).isEqualTo("senha-criptografada");
	}

	@Test
	void naoDeveSalvarUsuarioComEmailJaCadastrado() {
		given(repository.existsByEmail("email@email.com")).willReturn(true);

		Usuario usuario = Usuario.builder().email("email@email.com").build();

		Throwable erro = catchThrowable(() -> service.salvarUsuario(usuario));

		assertThat(erro).isInstanceOf(RegraNegocioException.class);
		then(repository).should(never()).save(any());
	}

	@Test
	void deveAutenticarComSucesso() throws Exception {
		String email = "email@email.com";
		String senha = "123";
		String senhaDoBanco = "hash";

		Usuario usuario = Usuario.builder().id(1L).email(email).senha(senhaDoBanco).build();

		given(repository.findByEmail(email)).willReturn(Optional.of(usuario));
		given(passwordEncoder.matches(senha, senhaDoBanco)).willReturn(true);

		Usuario result = service.autenticar(email, senha);

		assertThat(result).isNotNull();
		assertThat(result.getEmail()).isEqualTo(email);
		then(passwordEncoder).should().matches(senha, senhaDoBanco);
	}

	@Test
	void deveLancarErroQuandoNaoExistirUsuarioCadastradoComOEmailInformado() {
		given(repository.findByEmail(anyString())).willReturn(Optional.empty());

		Throwable erro = catchThrowable(() -> service.autenticar("email.com", "123"));

		assertThat(erro).isInstanceOf(AutenticacaoException.class)
			.hasMessage("Usuário não encontrado para o email informado.");
	}

	@Test
	void deveLancarErroQuandoSenhaErrada() {
		String email = "email@email.com";
		Usuario usuario = Usuario.builder().id(1L).email(email).senha("hash").build();

		given(repository.findByEmail(email)).willReturn(Optional.of(usuario));
		given(passwordEncoder.matches("456", "hash")).willReturn(false);

		Throwable erro = catchThrowable(() -> service.autenticar(email, "456"));

		assertThat(erro).isInstanceOf(AutenticacaoException.class).hasMessage("Senha inválida.");
		then(passwordEncoder).should().matches("456", "hash");
	}

	@Test
	void deveObterUsuarioPorId() throws Exception {
		Long id = 1L;
		Usuario usuarioMock = Usuario.builder()
			.id(id)
			.nome("usuario teste")
			.email("teste@email.com")
			.senha("123")
			.build();

		given(repository.findById(id)).willReturn(Optional.of(usuarioMock));

		Optional<Usuario> result = service.obterPorId(id);

		assertThat(result).isPresent().contains(usuarioMock);
		then(repository).should().findById(id);
	}

	@Test
	void deveLancarErroAoNaoEncontrarUsuarioPorId() {
		Long id = 1L;
		given(repository.findById(id)).willReturn(Optional.empty());

		Throwable erro = catchThrowable(() -> service.obterPorId(id));

		assertThat(erro).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Usuário não encontrado para o ID informado.");
		then(repository).should().findById(id);
	}

	@Test
	void deveLancarErroAoNaoEncontrarUsuarioPorEmail() {
		String email = "teste@email.com";
		given(repository.findByEmail(email)).willReturn(Optional.empty());

		Throwable erro = catchThrowable(() -> service.obterIdUsuarioPorEmail(email));

		assertThat(erro).isInstanceOf(RegraNegocioException.class)
			.hasMessage("Usuário não encontrado para o email informado");
		then(repository).should().findByEmail(email);
	}

}
