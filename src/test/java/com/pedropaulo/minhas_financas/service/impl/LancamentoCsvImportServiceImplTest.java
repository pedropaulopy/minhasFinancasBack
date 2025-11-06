package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.api.dto.importacao.ImportResultadoDTO;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.model.repository.UsuarioRepository;
import com.pedropaulo.minhas_financas.service.CategoriaService;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LancamentoCsvImportServiceImplTest {

	static class NoOpTransactionTemplate extends TransactionTemplate {

		NoOpTransactionTemplate() {
			super(null);
		}

		@Override
		public <T> T execute(TransactionCallback<T> action) {
			return action.doInTransaction(new SimpleTransactionStatus());
		}

	}

	@Mock
	LancamentoService lancamentoService;

	@Mock
	CategoriaRepository categoriaRepository;

	@Mock
	UsuarioRepository usuarioRepository;

	@Mock
	CategoriaService categoriaService;

	private TransactionTemplate txTemplate;

	private LancamentoCsvImportServiceImpl service;

	@BeforeEach
	void setup() throws NoSuchFieldException, IllegalAccessException {
		txTemplate = new NoOpTransactionTemplate();
		service = new LancamentoCsvImportServiceImpl(categoriaService, lancamentoService, txTemplate,
				categoriaRepository, usuarioRepository);

		EntityManager emStub = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class[] { EntityManager.class }, (proxy, method, args) -> null);

		Field field = LancamentoCsvImportServiceImpl.class.getDeclaredField("entityManager");
		field.setAccessible(true);
		field.set(service, emStub);
	}

	@Test
	void importar_ok_persisteComCategoriasNovas() throws Exception {
		when(usuarioRepository.getById(anyLong())).thenAnswer(inv -> {
			Long id = inv.getArgument(0);
			Usuario u = new Usuario();
			u.setId(id);
			return u;
		});

		String content = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Compra mercado,123,DESPESA,EFETIVADO,99,10/11/2025,Comida|Essencial\n"
				+ "Aluguel,2500,DESPESA,PENDENTE,99,01/11/2025,Alugel|Casa\n";

		when(categoriaRepository.findByNomeIgnoreCaseAndUsuario(anyString(), any(Usuario.class)))
			.thenReturn(Optional.empty());

		ImportResultadoDTO resumo = service.importar(csv(content), 99L);

		assertEquals(2, resumo.getTotalLidas());
		assertEquals(2, resumo.getTotalSucesso());
		assertEquals(0, resumo.getTotalFalha());
		verify(lancamentoService, times(1)).salvarTodos(anyList());
	}

	@Test
	void importar_ok_semCategorias_naoCriaNemConsulta() throws Exception {
		when(usuarioRepository.getById(anyLong())).thenAnswer(inv -> {
			Long id = inv.getArgument(0);
			Usuario u = new Usuario();
			u.setId(id);
			return u;
		});

		String content = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Salário,10000,RECEITA,EFETIVADO,7,05/11/2025,\n";

		ImportResultadoDTO resumo = service.importar(csv(content), 7L);

		assertEquals(1, resumo.getTotalLidas());
		assertEquals(1, resumo.getTotalSucesso());
		assertEquals(0, resumo.getTotalFalha());
		verify(lancamentoService, times(1)).salvarTodos(anyList());
		verifyNoInteractions(categoriaRepository);
	}

	@Test
	void importar_comLinhaInvalida_adicionaFalhaENaoQuebra() throws Exception {
		when(usuarioRepository.getById(anyLong())).thenAnswer(inv -> {
			Long id = inv.getArgument(0);
			Usuario u = new Usuario();
			u.setId(id);
			return u;
		});

		String content = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Compra inválida,0,DESPESA,EFETIVADO,5,10/11/2025,Food\n"
				+ "Venda,300,RECEITA,PENDENTE,5,12/11/2025,Vendas\n";

		when(categoriaRepository.findByNomeIgnoreCaseAndUsuario(anyString(), any(Usuario.class)))
			.thenReturn(Optional.empty());

		ImportResultadoDTO resumo = service.importar(csv(content), 5L);

		assertEquals(2, resumo.getTotalLidas());
		assertEquals(1, resumo.getTotalSucesso());
		assertEquals(1, resumo.getTotalFalha());
		assertEquals(1, resumo.getErros().size());
		assertTrue(resumo.getErros().get(0).getMotivo().contains("Valor inválido"));
		verify(lancamentoService, times(1)).salvarTodos(anyList());
	}

	@Test
	void importar_cabecalhoInvalido_lancaIAE() {
		String content = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC\n"
				+ "Qualquer,10,RECEITA,EFETIVADO,1,01/01/2025\n";

		assertThrows(IllegalArgumentException.class, () -> service.importar(csv(content), 1L));
		verifyNoInteractions(lancamentoService);
	}

	@Test
	void importar_reutilizaCategoriasExistentes_semCriarNovas() throws Exception {
		when(usuarioRepository.getById(anyLong())).thenAnswer(inv -> {
			Long id = inv.getArgument(0);
			Usuario u = new Usuario();
			u.setId(id);
			return u;
		});

		String content = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Compra,50,DESPESA,EFETIVADO,42,02/11/2025,Comida|Essencial\n";

		Categoria c1 = new Categoria();
		c1.setNome("Comida");
		Categoria c2 = new Categoria();
		c2.setNome("Essencial");

		when(categoriaRepository.findByNomeIgnoreCaseAndUsuario(eq("Comida"), any(Usuario.class)))
			.thenReturn(Optional.of(c1));
		when(categoriaRepository.findByNomeIgnoreCaseAndUsuario(eq("Essencial"), any(Usuario.class)))
			.thenReturn(Optional.of(c2));

		ImportResultadoDTO resumo = service.importar(csv(content), 42L);

		assertEquals(1, resumo.getTotalSucesso());
		assertEquals(0, resumo.getTotalFalha());
		verify(lancamentoService, times(1)).salvarTodos(anyList());
	}


    private static InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
