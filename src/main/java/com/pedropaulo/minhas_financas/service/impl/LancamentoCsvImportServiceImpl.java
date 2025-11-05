package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.api.dto.importacao.ImportResultadoDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import com.pedropaulo.minhas_financas.model.repository.CategoriaRepository;
import com.pedropaulo.minhas_financas.model.repository.UsuarioRepository;
import com.pedropaulo.minhas_financas.service.CategoriaService;
import com.pedropaulo.minhas_financas.service.LancamentoCsvImportService;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LancamentoCsvImportServiceImpl implements LancamentoCsvImportService {

	private static final int TAMANHO_LOTE = 1000;

	private static final String H_DESC = "DESC";

	private static final String H_VALOR_LANC = "VALOR_LANC";

	private static final String H_TIPO = "TIPO";

	private static final String H_STATUS = "STATUS";

	private static final String H_USUARIO = "USUARIO";

	private static final String H_DATA_LANC = "DATA_LANC";

	private static final String H_CATEGORIA = "CATEGORIA";

	private final LancamentoService lancamentoService;

	private final TransactionTemplate txTemplate;

	private final CategoriaRepository categoriaRepository;

	private final UsuarioRepository usuarioRepository;

	private final CategoriaService categoriaService;

	@PersistenceContext
	private EntityManager entityManager;

	public LancamentoCsvImportServiceImpl(CategoriaService categoriaService, LancamentoService lancamentoService,
			TransactionTemplate txTemplate, CategoriaRepository categoriaRepository,
			UsuarioRepository usuarioRepository) {
		this.categoriaService = categoriaService;
		this.lancamentoService = lancamentoService;
		this.txTemplate = txTemplate;
		this.categoriaRepository = categoriaRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public ImportResultadoDTO importar(InputStream inputStream, Long usuarioAutenticadoId)
			throws RegraNegocioException {
		ImportResultadoDTO resumo = new ImportResultadoDTO();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader()
				.withTrim()
				.withIgnoreEmptyLines()
				.withAllowMissingColumnNames()
				.parse(reader);

			validarCabecalho(parser.getHeaderMap().keySet());

			Map<Lancamento, Set<String>> catsPorLanc = new IdentityHashMap<>();
			List<Lancamento> bufferLote = new ArrayList<>(TAMANHO_LOTE);
			long linhaAbsoluta;

			for (CSVRecord rec : parser) {
				linhaAbsoluta = rec.getRecordNumber() + 1;
				resumo.incLida();

				processarLinhaCsv(rec, usuarioAutenticadoId, catsPorLanc, bufferLote, resumo, linhaAbsoluta);

				if (bufferLote.size() >= TAMANHO_LOTE) {
					persistirEmLote(bufferLote, catsPorLanc, resumo);
					bufferLote.clear();
					catsPorLanc.clear();
				}
			}

			if (!bufferLote.isEmpty()) {
				persistirEmLote(bufferLote, catsPorLanc, resumo);
			}
		}
		catch (IOException e) {
			throw new RegraNegocioException("Erro ao processar o arquivo CSV: " + e.getMessage());
		}

		return resumo;
	}

	private void validarCabecalho(Set<String> header) {
		List<String> obrigatorio = List.of(H_DESC, H_VALOR_LANC, H_TIPO, H_STATUS, H_USUARIO, H_DATA_LANC, H_CATEGORIA);
		List<String> faltando = obrigatorio.stream().filter(h -> !header.contains(h)).collect(Collectors.toList());
		if (!faltando.isEmpty()) {
			throw new IllegalArgumentException("Cabeçalho inválido. Faltando: " + faltando);
		}
	}

	private Lancamento mapearSemEntidades(CSVRecord r, Long usuarioAutenticadoId) throws RegraNegocioException {
		String desc = obrigatorio(r, H_DESC);
		String valorStr = obrigatorio(r, H_VALOR_LANC);
		String tipoStr = obrigatorio(r, H_TIPO);
		String statusStr = obrigatorio(r, H_STATUS);
		String usuarioIdStr = obrigatorio(r, H_USUARIO);
		String dataStr = obrigatorio(r, H_DATA_LANC);

		BigDecimal valor = parseValorMonetario(valorStr);
		if (valor.compareTo(BigDecimal.ZERO) <= 0) {
			throw new RegraNegocioException(
					"Valor inválido (os valores não podem ser igual ou menores a zero): " + valorStr);
		}

		TipoLancamento tipo = TipoLancamento.valueOf(tipoStr.toUpperCase(Locale.ROOT));
		StatusLancamento status = StatusLancamento.valueOf(statusStr.toUpperCase(Locale.ROOT));

		Long usuarioIdCsv = Long.parseLong(usuarioIdStr);
		if (!usuarioIdCsv.equals(usuarioAutenticadoId)) {
			throw new RegraNegocioException("Usuário do CSV (" + usuarioIdCsv + ") diferente do usuário autenticado ("
					+ usuarioAutenticadoId + ").");
		}

		String[] partes = dataStr.split("/");
		if (partes.length != 3)
			throw new RegraNegocioException("Data inválida: " + dataStr);

		int mes = Integer.parseInt(partes[1]);
		int ano = Integer.parseInt(partes[2]);
		if (mes < 1 || mes > 12)
			throw new RegraNegocioException("Insira um mês válido (1-12). Valor recebido: " + mes);
		if (String.valueOf(ano).length() != 4)
			throw new RegraNegocioException("Insira um ano válido (AAAA). Valor recebido: " + ano);

		Usuario usuarioRefDetached = new Usuario();
		usuarioRefDetached.setId(usuarioIdCsv);

		Lancamento l = new Lancamento();
		l.setDescricao(desc);
		l.setValor(valor);
		l.setTipoLancamento(tipo);
		l.setStatusLancamento(status);
		l.setUsuario(usuarioRefDetached);
		l.setMes(mes);
		l.setAno(ano);
		l.setDataCadastro(LocalDate.now());
		return l;
	}

	private Set<String> extrairNomesCategorias(String catRaw) {
		if (catRaw == null || catRaw.isBlank())
			return Collections.emptySet();
		String[] nomes = catRaw.split("\\|");
		Set<String> out = new LinkedHashSet<>();
		for (String nome : nomes) {
			String n = nome.trim();
			if (!n.isBlank())
				out.add(n);
		}
		return out;
	}

	private String obrigatorio(CSVRecord r, String h) {
		String v = r.get(h);
		if (v == null || v.isBlank())
			throw new IllegalArgumentException("Campo obrigatório vazio: " + h);
		return v.trim();
	}

	private BigDecimal parseValorMonetario(String raw) {
		String clean = raw.replace("R$", "").replace("$", "").replace(" ", "").replace(".", "").replace(",", ".");
		return new BigDecimal(clean);
	}

	private void persistirEmLote(List<Lancamento> lote, Map<Lancamento, Set<String>> catsPorLanc,
			ImportResultadoDTO resumo) {
		if (lote == null || lote.isEmpty())
			return;

		txTemplate.execute(status -> {
			anexarUsuariosGerenciados(lote);
			Long uid = obterUsuarioId(lote);

			Map<String, Categoria> mapaPorNome = resolverCategorias(uid, catsPorLanc);
			aplicarCategorias(lote, catsPorLanc, mapaPorNome);

			persistirLote(lote);
			return null;
		});

		atualizarResumo(resumo, lote.size());
	}

	private void anexarUsuariosGerenciados(List<Lancamento> lote) {
		if (lote == null || lote.isEmpty())
			return;

		for (Lancamento lancamento : lote) {
			Long usuarioId = lancamento.getUsuario().getId();
			Usuario usuario = usuarioRepository.getById(usuarioId);
			lancamento.setUsuario(usuario);
		}
	}

	private Long obterUsuarioId(List<Lancamento> lote) {
		return lote.get(0).getUsuario().getId();
	}

	private Map<String, Categoria> resolverCategorias(Long uid, Map<Lancamento, Set<String>> catsPorLanc) {
		Set<String> nomes = catsPorLanc.values()
			.stream()
			.filter(Objects::nonNull)
			.flatMap(Set::stream)
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toCollection(LinkedHashSet::new));

		return nomes.isEmpty() ? Collections.emptyMap() : carregarOuCriarCategorias(uid, nomes);
	}

	private void aplicarCategorias(List<Lancamento> lote, Map<Lancamento, Set<String>> catsPorLanc,
			Map<String, Categoria> mapaPorNome) {
		if (mapaPorNome.isEmpty())
			return;

		lote.forEach(l -> {
			Set<Categoria> categorias = catsPorLanc.getOrDefault(l, Collections.emptySet())
				.stream()
				.map(mapaPorNome::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
			if (!categorias.isEmpty()) {
				l.setCategorias(categorias);
			}
		});
	}

	private void persistirLote(List<Lancamento> lote) {
		lancamentoService.salvarTodos(lote);
		entityManager.flush();
		entityManager.clear();
	}

	private void atualizarResumo(ImportResultadoDTO resumo, int quantidade) {
		for (int i = 0; i < quantidade; i++) {
			resumo.incSucesso();
		}
	}

	private Map<String, Categoria> carregarOuCriarCategorias(Long usuarioId, Set<String> nomes) {
		Map<String, Categoria> categoriasMapeadas = new HashMap<>();
		if (nomes == null || nomes.isEmpty())
			return categoriasMapeadas;

		Usuario usuario = usuarioRepository.getById(usuarioId);

		for (String raw : nomes) {
			String nome = (raw == null) ? null : raw.trim();
			if (nome == null || nome.isEmpty())
				continue;
			if (categoriasMapeadas.containsKey(nome))
				continue;

			Categoria existente = categoriaRepository.findByNomeIgnoreCaseAndUsuario(nome, usuario).orElse(null);

			if (existente != null) {
				categoriasMapeadas.put(nome, existente);
				continue;
			}

			Categoria nova = new Categoria();
			nova.setUsuario(usuario);
			nova.setNome(nome);

			Categoria salva = categoriaRepository.save(nova);
			categoriasMapeadas.put(nome, salva);
		}

		return categoriasMapeadas;
	}

	private void processarLinhaCsv(CSVRecord rec, Long usuarioAutenticadoId, Map<Lancamento, Set<String>> catsPorLanc,
			List<Lancamento> bufferLote, ImportResultadoDTO resumo, long linhaAbsoluta) {
		try {
			Lancamento l = mapearSemEntidades(rec, usuarioAutenticadoId);
			Set<String> nomesCats = extrairNomesCategorias(rec.get(H_CATEGORIA));
			catsPorLanc.put(l, nomesCats);
			bufferLote.add(l);
		}
		catch (Exception e) {
			resumo.addFalha(linhaAbsoluta, e.getMessage(), String.join(",", rec));
		}
	}

}
