package com.pedropaulo.minhas_financas.service.impl;

import com.pedropaulo.minhas_financas.api.dto.importacao.ImportResultadoDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LancamentoCsvImportServiceImpl implements LancamentoCsvImportService {

    private static final String H_DESC = "DESC";
    private static final String H_VALOR_LANC = "VALOR_LANC";
    private static final String H_TIPO = "TIPO";
    private static final String H_STATUS = "STATUS";
    private static final String H_USUARIO = "USUARIO";
    private static final String H_DATA_LANC = "DATA_LANC";
    private static final String H_CATEGORIA = "CATEGORIA";

    private final CategoriaService categoriaService;
    private final LancamentoService lancamentoService;
    private final TransactionTemplate txTemplate;

    @PersistenceContext
    private EntityManager em;

    public LancamentoCsvImportServiceImpl(
            CategoriaService categoriaService,
            LancamentoService lancamentoService,
            TransactionTemplate txTemplate) {
        this.categoriaService = categoriaService;
        this.lancamentoService = lancamentoService;
        this.txTemplate = txTemplate;
    }

    @Override
    public ImportResultadoDTO importar(InputStream in, int tamanhoDoLote, Long usuarioAutenticadoId) throws Exception {
        ImportResultadoDTO resumo = new ImportResultadoDTO();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim()
                    .withIgnoreEmptyLines()
                    .withAllowMissingColumnNames()
                    .parse(reader);

            validarCabecalho(parser.getHeaderMap().keySet());

            Map<Lancamento, Set<String>> catsPorLanc = new IdentityHashMap<>();
            List<Lancamento> bufferLote = new ArrayList<>(tamanhoDoLote);
            long linhaAbsoluta = 1;

            for (CSVRecord rec : parser) {
                linhaAbsoluta = rec.getRecordNumber() + 1;
                resumo.incLida();

                try {
                    Lancamento l = mapearSemEntidades(rec, usuarioAutenticadoId);
                    Set<String> nomesCats = extrairNomesCategorias(rec.get(H_CATEGORIA));
                    catsPorLanc.put(l, nomesCats);
                    bufferLote.add(l);
                } catch (Exception e) {
                    resumo.addFalha(linhaAbsoluta, e.getMessage(), String.join(",", rec));
                }

                if (bufferLote.size() >= tamanhoDoLote) {
                    persistirEmLote(bufferLote, catsPorLanc, resumo); // OPT: resolve categorias em lote
                    bufferLote.clear();
                    catsPorLanc.clear();
                }
            }

            if (!bufferLote.isEmpty()) {
                persistirEmLote(bufferLote, catsPorLanc, resumo); // OPT: idem para o último lote
            }
        }

        return resumo;
    }

    private void validarCabecalho(Set<String> header) {
        List<String> obrig = List.of(H_DESC, H_VALOR_LANC, H_TIPO, H_STATUS, H_USUARIO, H_DATA_LANC, H_CATEGORIA);
        List<String> faltando = obrig.stream().filter(h -> !header.contains(h)).collect(Collectors.toList());
        if (!faltando.isEmpty()) {
            throw new IllegalArgumentException("Cabeçalho inválido. Faltando: " + faltando);
        }
    }

    private Lancamento mapearSemEntidades(CSVRecord r, Long usuarioAutenticadoId) throws RegraNegocioException {
        String desc = obrig(r, H_DESC);
        String valorStr = obrig(r, H_VALOR_LANC);
        String tipoStr = obrig(r, H_TIPO);
        String statusStr = obrig(r, H_STATUS);
        String usuarioIdStr = obrig(r, H_USUARIO);
        String dataStr = obrig(r, H_DATA_LANC);

        BigDecimal valor = parseValorMonetario(valorStr);
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("Valor inválido (<= 0): " + valorStr);
        }

        TipoLancamento tipo = TipoLancamento.valueOf(tipoStr.toUpperCase(Locale.ROOT));
        StatusLancamento status = StatusLancamento.valueOf(statusStr.toUpperCase(Locale.ROOT));

        Long usuarioIdCsv = Long.parseLong(usuarioIdStr);
        if (!usuarioIdCsv.equals(usuarioAutenticadoId)) {
            throw new RegraNegocioException("Usuário do CSV (" + usuarioIdCsv +
                    ") diferente do usuário autenticado (" + usuarioAutenticadoId + ").");
        }

        String[] partes = dataStr.split("/");
        if (partes.length != 3) throw new RegraNegocioException("Data inválida: " + dataStr);

        int dia = Integer.parseInt(partes[0]);
        int mes = Integer.parseInt(partes[1]);
        int ano = Integer.parseInt(partes[2]);
        if (mes < 1 || mes > 12) throw new RegraNegocioException("Insira um mês válido (1-12). Valor recebido: " + mes);
        if (String.valueOf(ano).length() != 4) throw new RegraNegocioException("Insira um ano válido (AAAA). Valor recebido: " + ano);

        // apenas referência por id; entidade será gerenciada no lote
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
        if (catRaw == null || catRaw.isBlank()) return Collections.emptySet();
        String[] nomes = catRaw.split("\\|");
        Set<String> out = new LinkedHashSet<>();
        for (String nome : nomes) {
            String n = nome.trim();
            if (!n.isBlank()) out.add(n);
        }
        return out;
    }

    private String obrig(CSVRecord r, String h) {
        String v = r.get(h);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Campo obrigatório vazio: " + h);
        return v.trim();
    }

    private BigDecimal parseValorMonetario(String raw) {
        String clean = raw.replace("R$", "")
                .replace("$", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");
        return new BigDecimal(clean);
    }

    /**
     * OPT: Resolve USUÁRIO como referência gerenciada e categorias em LOTE:
     *  - 1 SELECT para todas as categorias existentes do usuário no lote (por nome)
     *  - persist de todas as faltantes em batch
     *  - associação sem novas idas ao banco
     *  Também libera o contexto após persistência para manter memória/velocidade.
     */
    private void persistirEmLote(List<Lancamento> lote,
                                 Map<Lancamento, Set<String>> catsPorLanc,
                                 ImportResultadoDTO resumo) {

        txTemplate.execute(status -> {
            // ----- Usuario gerenciado (todos os lançamentos do lote são do mesmo usuário pela validação) -----
            for (Lancamento l : lote) {
                l.setUsuario(em.getReference(Usuario.class, l.getUsuario().getId()));
            }
            Long uid = lote.get(0).getUsuario().getId();

            // ----- OPT: coletar TODOS os nomes de categorias do lote -----
            Set<String> todosNomes = new LinkedHashSet<>();
            for (Lancamento l : lote) {
                todosNomes.addAll(catsPorLanc.getOrDefault(l, Collections.emptySet()));
            }

            // ----- OPT: buscar existentes em um único SELECT -----
            Map<String, Categoria> mapaPorNome = new HashMap<>();
            if (!todosNomes.isEmpty()) {
                List<Categoria> existentes = em.createQuery(
                                "select c from Categoria c " +
                                        "where c.usuario.id = :uid and c.nome in :nomes", Categoria.class)
                        .setParameter("uid", uid)
                        .setParameter("nomes", todosNomes)
                        .getResultList();
                for (Categoria c : existentes) {
                    mapaPorNome.put(c.getNome(), c);
                }

                // ----- OPT: criar faltantes via batch (em.persist) -----
                Usuario usuarioRef = em.getReference(Usuario.class, uid);
                for (String nome : todosNomes) {
                    if (!mapaPorNome.containsKey(nome)) {
                        Categoria nova = new Categoria();
                        nova.setUsuario(usuarioRef);
                        nova.setNome(nome);
                        em.persist(nova);             // batched insert
                        mapaPorNome.put(nome, nova);
                    }
                }
            }

            // ----- OPT: associar sem novas idas ao banco -----
            for (Lancamento l : lote) {
                Set<String> nomes = catsPorLanc.getOrDefault(l, Collections.emptySet());
                if (!nomes.isEmpty()) {
                    Set<Categoria> categorias = new LinkedHashSet<>(nomes.size());
                    for (String n : nomes) categorias.add(mapaPorNome.get(n));
                    l.setCategorias(categorias);
                }
            }

            // ----- Persistir lançamentos em batch -----
            lancamentoService.salvarTodos(lote);

            // ----- OPT: liberar contexto para manter performance estável em grandes volumes -----
            em.flush();
            em.clear();

            return null;
        });

        for (int i = 0; i < lote.size(); i++) resumo.incSucesso();
    }
}
