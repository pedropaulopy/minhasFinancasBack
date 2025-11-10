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
import com.pedropaulo.minhas_financas.service.LancamentoCsvImportService;
import com.pedropaulo.minhas_financas.service.LancamentoService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class LancamentoCsvImportServiceImpl implements LancamentoCsvImportService {

    private static final int CHUNK_SIZE = 10_000;
    private static final int IO_BUFFER_SIZE = 1 << 20;

    private static final String H_DESC       = "DESC";
    private static final String H_VALOR_LANC = "VALOR_LANC";
    private static final String H_TIPO       = "TIPO";
    private static final String H_STATUS     = "STATUS";
    private static final String H_USUARIO    = "USUARIO";
    private static final String H_DATA_LANC  = "DATA_LANC";
    private static final String H_CATEGORIA  = "CATEGORIA";

    private final LancamentoService lancamentoService;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransactionTemplate txTemplate;

    // cache global de categorias por nome normalizado
    private final Map<String, Categoria> categoriaCache = new HashMap<>(2048);

    @PersistenceContext
    private EntityManager em;

    private static final DateTimeFormatter DTF_DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public LancamentoCsvImportServiceImpl(
            LancamentoService lancamentoService,
            CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.lancamentoService   = lancamentoService;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository   = usuarioRepository;
        this.txTemplate          = transactionTemplate;
    }

    // Estrutura leve para guardar dados parseados sem anexar entidades
    private static final class LinhaParseada {
        Lancamento lancamento;           // com usuarioId temporário
        Long usuarioId;                  // ID a ser resolvido na transação
        Set<String> nomesCategorias;     // nomes a resolver na transação
        long linha;                      // número da linha para erro
    }

    @Override
    public ImportResultadoDTO importar(InputStream csvStream, Long usuarioAutenticadoId) throws RegraNegocioException {
        Objects.requireNonNull(csvStream, "csvStream não pode ser nulo");

        ImportResultadoDTO resumo = new ImportResultadoDTO();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8), IO_BUFFER_SIZE);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(br)) {

            final List<LinhaParseada> buffer = new ArrayList<>(CHUNK_SIZE);
            long linhaAbsoluta = 1; // conta o cabeçalho

            for (CSVRecord rec : parser) {
                linhaAbsoluta++;
                resumo.incLida();

                try {
                    LinhaParseada lp = mapearLinha(rec, usuarioAutenticadoId, linhaAbsoluta);
                    buffer.add(lp);
                } catch (Exception e) {
                    resumo.addFalha(linhaAbsoluta, safeMessage(e), joinRaw(rec));
                }

                if (buffer.size() >= CHUNK_SIZE) {
                    processarEPersistirLote(buffer, resumo);
                }
            }

            if (!buffer.isEmpty()) {
                processarEPersistirLote(buffer, resumo);
            }
        } catch (Exception e) {
            throw new RegraNegocioException("Falha ao importar CSV: ".concat(e.getMessage()).concat(String.valueOf(e)));
        }

        return resumo;
    }

    private void processarEPersistirLote(List<LinhaParseada> buffer, ImportResultadoDTO resumo) {

        txTemplate.execute(status -> {
            try {
                // 1) Resolver categorias do lote DENTRO da transação
                Map<String, Categoria> categoriasDoLote = resolverCategoriasDentroTransacao(buffer);

                // 2) Trocar IDs por referências gerenciadas e setar categorias
                List<Lancamento> prontos = new ArrayList<>(buffer.size());
                for (LinhaParseada lp : buffer) {
                    Lancamento l = lp.lancamento;

                    // Usuario como referência gerenciada (sem query)
                    if (lp.usuarioId == null) {
                        throw new RegraNegocioException("Usuário não informado para a linha " + lp.linha);
                    }
                    l.setUsuario(em.getReference(Usuario.class, lp.usuarioId));

                    // Categorias
                    if (lp.nomesCategorias != null && !lp.nomesCategorias.isEmpty()) {
                        Set<Categoria> cats = new LinkedHashSet<>(lp.nomesCategorias.size());
                        for (String nomeRaw : lp.nomesCategorias) {
                            String key = normalizaNomeCategoria(nomeRaw);
                            Categoria c = categoriasDoLote.get(key);
                            if (c != null) cats.add(c);
                        }
                        l.setCategorias(cats);
                    }

                    prontos.add(l);
                }

                // 3) Persistir em lote
                lancamentoService.salvarTodos(prontos);
                for (int i = 0; i < prontos.size(); i++) resumo.incSucesso();

            } catch (Exception e) {
                // Se falhar o lote, tentar item a item (para registrar quais quebram)
                status.setRollbackOnly();
                persistirComSplit(buffer, resumo);
            }
            return null;
        });

        buffer.clear();
    }

    private void persistirComSplit(List<LinhaParseada> buffer, ImportResultadoDTO resumo) {
        for (LinhaParseada lp : buffer) {
            txTemplate.execute(s2 -> {
                try {
                    Lancamento l = lp.lancamento;
                    l.setUsuario(em.getReference(Usuario.class, lp.usuarioId));

                    // resolver categorias por linha (usando o mesmo cache transacional)
                    Map<String, Categoria> cats = resolverCategoriasDentroTransacao(Collections.singletonList(lp));
                    if (lp.nomesCategorias != null && !lp.nomesCategorias.isEmpty()) {
                        Set<Categoria> set = new LinkedHashSet<>(lp.nomesCategorias.size());
                        for (String n : lp.nomesCategorias) {
                            Categoria c = cats.get(normalizaNomeCategoria(n));
                            if (c != null) set.add(c);
                        }
                        l.setCategorias(set);
                    }

                    lancamentoService.salvar(l);
                    resumo.incSucesso();
                } catch (Exception ex) {
                    resumo.addFalha(lp.linha, safeMessage(ex), lRaw(lp.lancamento));
                    s2.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private Map<String, Categoria> resolverCategoriasDentroTransacao(List<LinhaParseada> linhas) {
        // nomes distintos normalizados do conjunto
        Set<String> nomesDistintos = new LinkedHashSet<>();
        for (LinhaParseada lp : linhas) {
            if (lp.nomesCategorias != null) {
                for (String n : lp.nomesCategorias) {
                    String key = normalizaNomeCategoria(n);
                    if (!key.isEmpty()) nomesDistintos.add(key);
                }
            }
        }
        if (nomesDistintos.isEmpty()) return Collections.emptyMap();

        // faltar no cache global?
        Set<String> faltantes = new LinkedHashSet<>();
        for (String nome : nomesDistintos) {
            if (!categoriaCache.containsKey(nome)) faltantes.add(nome);
        }

        // resolver faltantes no repositório dentro da transação
        for (String nome : faltantes) {
            Categoria existente = categoriaRepository.findByNomeIgnoreCase(nome);
            if (existente == null) {
                // política: criar automaticamente
                Categoria nova = new Categoria();
                nova.setNome(nome);
                existente = categoriaRepository.save(nova);
            }
            categoriaCache.put(nome, existente);
        }

        // montar retorno apenas do conjunto do lote
        Map<String, Categoria> result = new HashMap<>(nomesDistintos.size());
        for (String nome : nomesDistintos) {
            Categoria c = categoriaCache.get(nome);
            if (c != null) result.put(nome, c);
        }
        return result;
    }

    private LinhaParseada mapearLinha(CSVRecord rec, Long usuarioAutenticadoId, long linhaAbsoluta) throws RegraNegocioException {
        LinhaParseada lp = new LinhaParseada();
        lp.linha = linhaAbsoluta;

        Lancamento l = new Lancamento();

        // Descrição
        l.setDescricao(getOrNull(rec, H_DESC));

        // Valor
        l.setValor(parseValorMonetario(getOrNull(rec, H_VALOR_LANC)));

        // Tipo
        String rawTipo = getOrNull(rec, H_TIPO);
        l.setTipoLancamento(TipoLancamento.valueOf(rawTipo.trim().toUpperCase(Locale.ROOT)));

        // Status
        String rawStatus = getOrNull(rec, H_STATUS);
        l.setStatusLancamento(StatusLancamento.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT)));

        // Data
        l.setDataCadastro(parseData(getOrNull(rec, H_DATA_LANC)));

        // Usuário (somente ID aqui)
        String rawUsuario = getOrNull(rec, H_USUARIO);
        if (rawUsuario != null && !rawUsuario.isBlank()) {
            lp.usuarioId = Long.parseLong(rawUsuario.trim());
            // valida existência básica sem anexar entidade
            if (!usuarioRepository.existsById(lp.usuarioId)) {
                throw new RegraNegocioException("Usuário não encontrado: id=" + lp.usuarioId);
            }
        } else {
            if (usuarioAutenticadoId == null) {
                throw new RegraNegocioException("Usuário não informado (coluna USUARIO vazia e sem autenticado).");
            }
            lp.usuarioId = usuarioAutenticadoId;
        }

        // Categorias (nomes a resolver depois)
        lp.nomesCategorias = extrairNomesCategorias(getOrNull(rec, H_CATEGORIA));

        lp.lancamento = l;
        return lp;
    }

    private static String getOrNull(CSVRecord rec, String header) {
        try {
            return rec.isMapped(header) ? rec.get(header) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static LocalDate parseData(String raw) throws RegraNegocioException {
        if (raw == null || raw.isBlank()) {
            throw new RegraNegocioException("DATA_LANC vazio/ausente");
        }
        try {
            return LocalDate.parse(raw.trim(), DTF_DDMMYYYY);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Data inválida: " + raw);
        }
    }

    private static BigDecimal parseValorMonetario(String raw) throws RegraNegocioException {
        if (raw == null || raw.isBlank()) {
            throw new RegraNegocioException("VALOR_LANC vazio/ausente");
        }
        String s = raw.trim();
        s = s.replace("R$", "").replace("$", "").replaceAll("\\s+", "");
        if (s.contains(",") && s.contains(".")) s = s.replace(".", "").replace(",", ".");
        else if (s.contains(",")) s = s.replace(",", ".");
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Valor monetário inválido: " + raw);
        }
    }

    private static String normalizaNomeCategoria(String nome) {
        return nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> extrairNomesCategorias(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptySet();
        String[] parts = raw.split("\\|");
        Set<String> out = new LinkedHashSet<>(parts.length);
        for (String p : parts) {
            String n = p.trim();
            if (!n.isEmpty()) out.add(n);
        }
        return out;
    }

    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }

    private static String joinRaw(CSVRecord rec) {
        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < rec.size(); i++) {
            if (i > 0) sb.append(',');
            String v = rec.get(i);
            if (v != null) sb.append(v);
        }
        return sb.toString();
    }

    private static String lRaw(Lancamento l) {
        return l == null ? "" :
                String.valueOf(l.getDescricao());
    }
}
