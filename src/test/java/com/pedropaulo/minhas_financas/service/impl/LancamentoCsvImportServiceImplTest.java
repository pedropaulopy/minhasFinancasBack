package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.api.dto.importacao.ImportResultadoDTO;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.model.repository.UsuarioRepository;
import com.pedropaulo.minhas_financas.service.CategoriaService;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LancamentoCsvImportServiceImplTest {

	@Mock
	CategoriaService categoriaService;

	@Mock
	LancamentoService lancamentoService;

	@Mock
	CategoriaRepository categoriaRepository;

	@Mock
	UsuarioRepository usuarioRepository;

	static class NoOpTransactionTemplate extends TransactionTemplate {

		NoOpTransactionTemplate() {
			super(null);
		}

		@Override
		public <T> T execute(TransactionCallback<T> action) {
			// >>> alteração: usar SimpleTransactionStatus em vez de
			// mock(TransactionStatus.class)
			return action.doInTransaction(new SimpleTransactionStatus());
		}

	}

	private TransactionTemplate txTemplate;

	private EntityManager emProxy;

	private TypedQuery<Categoria> typedQueryProxy;

	private final AtomicReference<List<Categoria>> queryResultRef = new AtomicReference<>(Collections.emptyList());

	LancamentoCsvImportServiceImpl service;

	@BeforeEach
	void setup() throws Exception {
		txTemplate = new NoOpTransactionTemplate();

		service = new LancamentoCsvImportServiceImpl(categoriaService, lancamentoService, txTemplate,
				categoriaRepository, usuarioRepository);
	}

	private static InputStream csv(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void importar_ok_persisteComCategoriasNovas() throws Exception {
		String csv = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Compra mercado,123,DESPESA,EFETIVADO,99,10/11/2025,Comida|Essencial\n"
				+ "Aluguel,2500,DESPESA,PENDENTE,99,01/11/2025,Alugel|Casa\n";

		queryResultRef.set(Collections.emptyList());

		ImportResultadoDTO resumo = service.importar(csv(csv), 99L);

		assertEquals(2, resumo.getTotalLidas());
		assertEquals(2, resumo.getTotalSucesso());
		assertEquals(0, resumo.getTotalFalha());

		verify(lancamentoService, times(1)).salvarTodos(anyList());
	}

	@Test
	void importar_ok_semCategorias_naoCriaNemConsulta() throws Exception {
		String csv = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Salário,10000,RECEITA,EFETIVADO,7,05/11/2025,\n";

		ImportResultadoDTO resumo = service.importar(csv(csv), 7L);

		assertEquals(1, resumo.getTotalLidas());
		assertEquals(1, resumo.getTotalSucesso());
		assertEquals(0, resumo.getTotalFalha());

		verify(lancamentoService, times(1)).salvarTodos(anyList());
	}

	@Test
	void importar_comLinhaInvalida_adicionaFalhaENaoQuebra() throws Exception {
		String csv = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Compra inválida,0,DESPESA,EFETIVADO,5,10/11/2025,Food\n"
				+ "Venda,300,RECEITA,PENDENTE,5,12/11/2025,Vendas\n";

		queryResultRef.set(Collections.emptyList());

		ImportResultadoDTO resumo = service.importar(csv(csv), 5L);

		assertEquals(2, resumo.getTotalLidas());
		assertEquals(1, resumo.getTotalSucesso());
		assertEquals(1, resumo.getTotalFalha());
		assertEquals(1, resumo.getErros().size());
		assertTrue(resumo.getErros().get(0).motivo.contains("Valor inválido"));

		verify(lancamentoService, times(1)).salvarTodos(anyList());
	}

	@Test
	void importar_cabecalhoInvalido_lancaIAE() {
		String csv = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC\n" + "Qualquer,10,RECEITA,EFETIVADO,1,01/01/2025\n";

		assertThrows(IllegalArgumentException.class, () -> service.importar(csv(csv), 1L));

		verifyNoInteractions(lancamentoService);
	}

	@Test
	void importar_usuarioDiferente_contabilizaFalha() throws Exception {
		String csv = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Teste,10,RECEITA,EFETIVADO,123,01/11/2025,Cat\n";

		ImportResultadoDTO resumo = service.importar(csv(csv), 999L);

		assertEquals(1, resumo.getTotalLidas());
		assertEquals(0, resumo.getTotalSucesso());
		assertEquals(1, resumo.getTotalFalha());
		assertTrue(resumo.getErros().get(0).motivo.contains("diferente do usuário autenticado"));

		verifyNoInteractions(lancamentoService);
	}

	@Test
	void importar_reutilizaCategoriasExistentes_semCriarNovas() throws Exception {
		String csv = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Compra,50,DESPESA,EFETIVADO,42,02/11/2025,Comida|Essencial\n";

		Categoria c1 = new Categoria();
		c1.setNome("Comida");
		Categoria c2 = new Categoria();
		c2.setNome("Essencial");
		queryResultRef.set(Arrays.asList(c1, c2));

		ImportResultadoDTO resumo = service.importar(csv(csv), 42L);

		assertEquals(1, resumo.getTotalSucesso());
		assertEquals(0, resumo.getTotalFalha());

		verify(lancamentoService, times(1)).salvarTodos(anyList());
	}

	@Test
	void importar_csvVazio_naoChamaSalvar() throws Exception {
		String csv = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n";

		ImportResultadoDTO resumo = service.importar(csv(csv), 1L);

		assertEquals(0, resumo.getTotalLidas());
		assertEquals(0, resumo.getTotalSucesso());
		assertEquals(0, resumo.getTotalFalha());

		verifyNoInteractions(lancamentoService);
	}

	@Test
	void importar_csvApenasComLinhasInvalidas_naoChamaSalvar() throws Exception {
		String csv = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Invalido 1,0,DESPESA,EFETIVADO,1,01/01/2025,Cat\n"
				+ "Invalido 2,-10,DESPESA,EFETIVADO,1,01/01/2025,Cat\n";

		ImportResultadoDTO resumo = service.importar(csv(csv), 1L);

		assertEquals(2, resumo.getTotalLidas());
		assertEquals(0, resumo.getTotalSucesso());
		assertEquals(2, resumo.getTotalFalha());

		verifyNoInteractions(lancamentoService);
	}

	@Test
	void importar_erroAoSalvar_lancaExcecao() {
		String csv = "DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA\n"
				+ "Compra,50,DESPESA,EFETIVADO,42,02/11/2025,Comida\n";

		doThrow(new RuntimeException("Erro de banco de dados")).when(lancamentoService).salvarTodos(anyList());

		assertThrows(RuntimeException.class, () -> service.importar(csv(csv), 42L));
	}

}
