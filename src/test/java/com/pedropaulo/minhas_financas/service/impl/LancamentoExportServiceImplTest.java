package com.pedropaulo.minhas_financas.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class LancamentoExportServiceImplTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private LancamentoRepository lancamentoRepository;

	private LancamentoExportServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new LancamentoExportServiceImpl(lancamentoRepository);
	}

	@Test
	void streamJsonByIds_deveExportarJsonComCamposEListasFormatados_eAplicarSanitizacaoEChunking() throws Exception {
		List<Long> idsEntrada = new ArrayList<>();
		idsEntrada.add(1L);
		idsEntrada.add(null);
		idsEntrada.add(1L);
		for (long i = 2; i <= 2001; i++)
			idsEntrada.add(i);

		AtomicInteger chamada = new AtomicInteger(0);
		when(lancamentoRepository.findAllByIdInOrderByIdAsc(anyList())).thenAnswer(invocation -> {
			int idx = chamada.incrementAndGet();
			if (idx == 1) {
				Lancamento l1 = novoLancamentoBasico(10L, "Desc A", BigDecimal.valueOf(123.45), 3, 2024,
						TipoLancamento.RECEITA, StatusLancamento.EFETIVADO);
				l1.setCategorias(new LinkedHashSet<>(Arrays.asList(categoria("Aluguel"), categoria("Moradia"))));
				return List.of(l1);
			}
			else {
				Lancamento l2 = novoLancamentoBasico(20L, null, null, null, null, null, null);
				l2.setCategorias(new LinkedHashSet<>(Collections.singletonList(categoria(" "))));
				return List.of(l2);
			}
		});

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		service.streamJsonByIds(out, idsEntrada);

		String json = out.toString(StandardCharsets.UTF_8);
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String, Object>> lista = mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
		});

		assertThat(lista).hasSize(2);

		Map<String, Object> primeiro = lista.get(0);
		assertThat(primeiro.get("id")).isEqualTo(10);
		assertThat(primeiro.get("descricao")).isEqualTo("Desc A");
		assertThat(primeiro.get("valor")).isEqualTo(123.45);
		assertThat(primeiro.get("ano")).isEqualTo(2024);
		assertThat(primeiro.get("mes")).isEqualTo(3);
		assertThat(primeiro.get("tipoLancamento")).isEqualTo("RECEITA");
		assertThat(primeiro.get("statusLancamento")).isEqualTo("EFETIVADO");
		assertThat(primeiro.get("data")).isEqualTo("03/2024");

		List<String> categorias = asStringList(primeiro.get("categorias"));
		assertThat(categorias).containsExactlyInAnyOrder("Aluguel", "Moradia");

		Map<String, Object> segundo = lista.get(1);
		assertThat(segundo.get("id")).isEqualTo(20);
		assertThat(segundo.get("descricao")).isNull();
		assertThat(segundo.get("valor")).isEqualTo(0.0);
		assertThat(segundo.get("ano")).isNull();
		assertThat(segundo.get("mes")).isNull();
		assertThat(segundo.get("tipoLancamento")).isNull();
		assertThat(segundo.get("statusLancamento")).isNull();
		assertThat(segundo.get("data")).isEqualTo("null/null");
		assertThat(asStringList(segundo.get("categorias"))).isEmpty();

		ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
		verify(lancamentoRepository, times(2)).findAllByIdInOrderByIdAsc(captor.capture());
		List<List<Long>> chunks = captor.getAllValues();
		assertThat(chunks.get(0).size()).isEqualTo(2000);
		assertThat(chunks.get(1).size()).isEqualTo(1);
		Set<Long> todosIdsConsultados = new LinkedHashSet<>();
		chunks.forEach(todosIdsConsultados::addAll);
		assertThat(todosIdsConsultados).doesNotContainNull();
		assertThat(Collections.frequency(new ArrayList<>(todosIdsConsultados), 1L)).isEqualTo(1);
		verifyNoMoreInteractions(lancamentoRepository);
	}

	@Test
	void streamCsvByIds_deveExportarCsvComCabecalho_formatacaoDeUsuarioCategoriasEQuotes_eAplicarSanitizacaoEChunking()
			throws Exception {
		List<Long> idsEntrada = new ArrayList<>();
		idsEntrada.add(null);
		idsEntrada.add(5L);
		for (long i = 6; i <= 2005; i++)
			idsEntrada.add(i);

		AtomicInteger chamada = new AtomicInteger(0);
		when(lancamentoRepository.findAllByIdInOrderByIdAsc(anyList())).thenAnswer(invocation -> {
			int idx = chamada.incrementAndGet();
			if (idx == 1) {
				Lancamento comTudo = novoLancamentoBasico(100L, "Item, com vírgula e \"aspas\"",
						BigDecimal.valueOf(99.9), 12, 2023, TipoLancamento.DESPESA, StatusLancamento.PENDENTE);
				Usuario usuarioEmail = new Usuario();
				usuarioEmail.setEmail("user@dominio.com");
				comTudo.setUsuario(usuarioEmail);
				comTudo.setCategorias(new LinkedHashSet<>(Arrays.asList(categoria("Mercado"), categoria("Casa"))));
				return List.of(comTudo);
			}
			else {
				Lancamento semEmailComNome = novoLancamentoBasico(101L, "Sem email", BigDecimal.ZERO, 1, 2022, null,
						null);
				Usuario usuarioNome = new Usuario();
				usuarioNome.setNome("Pedro Paulo");
				semEmailComNome.setUsuario(usuarioNome);

				Lancamento semNomeComId = novoLancamentoBasico(102L, "Sem nome", null, 2, 2022, null, null);
				Usuario usuarioId = new Usuario();
				usuarioId.setId(77L);
				semNomeComId.setUsuario(usuarioId);

				Lancamento semUsuario = novoLancamentoBasico(103L, "Sem usuario", BigDecimal.valueOf(1), 3, 2022,
						TipoLancamento.RECEITA, StatusLancamento.EFETIVADO);

				Lancamento categoriasRepetidas = novoLancamentoBasico(104L, "Cats", BigDecimal.valueOf(2), 4, 2022,
						null, null);
				categoriasRepetidas
					.setCategorias(new LinkedHashSet<>(Arrays.asList(categoria("A"), categoria("A"), categoria("B"))));

				return Arrays.asList(semEmailComNome, semNomeComId, semUsuario, categoriasRepetidas);
			}
		});

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		service.streamCsvByIds(out, idsEntrada);

		String csv = out.toString(StandardCharsets.UTF_8);
		List<String> linhas = Arrays.asList(csv.split("\\R"));
		assertThat(linhas.get(0)).isEqualTo("DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA");

		String l1 = linhas.get(1);
		assertThat(l1).contains("\"Item, com vírgula e \"\"aspas\"\"\"");
		assertThat(l1).contains(",99.9,");
		assertThat(l1).contains(",DESPESA,");
		assertThat(l1).contains(",PENDENTE,");
		assertThat(l1).contains(",user@dominio.com,");
		assertThat(l1).contains(",12/2023,");
		assertThat(l1).endsWith("Mercado|Casa");

		String l2 = linhas.get(2);
		String[] c2 = l2.split(",", -1);
		assertThat(c2).containsExactly("Sem email", "0.0", "", "", "Pedro Paulo", "01/2022", "");

		String l3 = linhas.get(3);
		String[] c3 = l3.split(",", -1);
		assertThat(c3).containsExactly("Sem nome", "0.0", "", "", "77", "02/2022", "");

		String l4 = linhas.get(4);
		String[] c4 = l4.split(",", -1);
		assertThat(c4).containsExactly("Sem usuario", "1.0", "RECEITA", "EFETIVADO", "", "03/2022", "");

		String l5 = linhas.get(5);
		String[] c5 = l5.split(",", -1);
		assertThat(c5).containsExactly("Cats", "2.0", "", "", "", "04/2022", "A|B");

		ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
		verify(lancamentoRepository, times(2)).findAllByIdInOrderByIdAsc(captor.capture());
		List<List<Long>> chunks = captor.getAllValues();
		assertThat(chunks.get(0).size()).isEqualTo(2000);
		assertThat(chunks.get(1).size()).isEqualTo(1);
		Set<Long> todosIdsConsultados = new LinkedHashSet<>();
		chunks.forEach(todosIdsConsultados::addAll);
		assertThat(todosIdsConsultados).doesNotContainNull();
		verifyNoMoreInteractions(lancamentoRepository);
	}

	// ----------------- Helpers -----------------

	private static List<String> asStringList(Object value) {
		if (value == null)
			return List.of();
		if (value instanceof Collection<?> coll) {
			List<String> out = new ArrayList<>(coll.size());
			for (Object o : coll)
				out.add(String.valueOf(o));
			return out;
		}
		if (value.getClass().isArray()) {
			int len = java.lang.reflect.Array.getLength(value);
			List<String> out = new ArrayList<>(len);
			for (int i = 0; i < len; i++)
				out.add(String.valueOf(java.lang.reflect.Array.get(value, i)));
			return out;
		}
		// fallback: valor único
		return List.of(String.valueOf(value));
	}

	private static Lancamento novoLancamentoBasico(Long id, String descricao, BigDecimal valor, Integer mes,
			Integer ano, TipoLancamento tipo, StatusLancamento status) {
		Lancamento lancamento = new Lancamento();
		lancamento.setId(id);
		lancamento.setDescricao(descricao);
		lancamento.setValor(valor);
		lancamento.setMes(mes);
		lancamento.setAno(ano);
		lancamento.setTipoLancamento(tipo);
		lancamento.setStatusLancamento(status);
		return lancamento;
	}

	private static Categoria categoria(String nome) {
		Categoria c = new Categoria();
		c.setNome(nome);
		return c;
	}

}
