package com.pedropaulo.minhas_financas.model.repository;

import com.pedropaulo.minhas_financas.model.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRepositoryTest {

	@Autowired
	UsuarioRepository repository;

	@Autowired
	TestEntityManager entityManager;

	private static final String EMAIL = "email@email.com";

	private static final String NOME = "usuario";

	private static final String SENHA = "123";

	private Usuario novoUsuario() {
		return Usuario.builder().nome(NOME).email(EMAIL).senha(SENHA).build();
	}

	private Usuario persistido() {
		Usuario u = novoUsuario();
		entityManager.persist(u);
		entityManager.flush();
		entityManager.clear();
		return u;
	}

	@Test
	void deveRetornarVerdadeiroQuandoEmailExistir() {
		persistido();
		boolean exists = repository.existsByEmail(EMAIL);
		assertThat(exists).isTrue();
	}

	@Test
	void deveRetornarFalsoQuandoEmailNaoExistir() {
		boolean exists = repository.existsByEmail(EMAIL);
		assertThat(exists).isFalse();
	}

	@Test
	void devePersistirUsuario() {
		Usuario salvo = repository.save(novoUsuario());
		entityManager.flush();
		assertThat(salvo.getId()).isNotNull();
	}

	@Test
	void deveBuscarPorEmailQuandoExistir() {
		persistido();
		Optional<Usuario> result = repository.findByEmail(EMAIL);
		assertThat(result).isPresent();
		assertThat(result.get().getEmail()).isEqualTo(EMAIL);
	}

	@Test
	void deveRetornarVazioQuandoEmailNaoExistir() {
		Optional<Usuario> result = repository.findByEmail(EMAIL);
		assertThat(result).isNotPresent();
	}

}
