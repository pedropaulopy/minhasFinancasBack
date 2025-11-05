package com.pedropaulo.minhas_financas.service.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.repository.LancamentoRepository;
import com.pedropaulo.minhas_financas.service.LancamentoExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LancamentoExportServiceImpl implements LancamentoExportService {

    private static final int CHUNK_SIZE = 2000;

    private final LancamentoRepository lancamentoRepository;

    @Transactional(readOnly = true)
    public void streamJsonByIds(OutputStream os, List<Long> ids) throws IOException {
        List<Long> clean = sanitizeIds(ids);

        JsonFactory jf = new JsonFactory();
        try (JsonGenerator gen = jf.createGenerator(os)) {
            gen.writeStartArray();

            for (List<Long> chunk : chunksOf(clean, CHUNK_SIZE)) {
                List<Lancamento> lote = lancamentoRepository.findAllByIdInOrderByIdAsc(chunk);
                for (Lancamento l : lote) writeJson(gen, l);
                gen.flush();
            }

            gen.writeEndArray();
        }
    }

    private void writeJson(JsonGenerator gen, Lancamento l) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("ID_LANC", l.getId());
        if (l.getDescricao() != null) gen.writeStringField("DESC", l.getDescricao());
        gen.writeNumberField("VALOR", numberSafe(l.getValor()));
        if (l.getAno() != null) gen.writeNumberField("ANO", l.getAno());
        if (l.getMes() != null) gen.writeNumberField("MES", l.getMes());
        if (l.getTipoLancamento() != null) gen.writeStringField("TIPO", l.getTipoLancamento().name());
        if (l.getStatusLancamento() != null) gen.writeStringField("STATUS", l.getStatusLancamento().name());
        String mm = String.format("%02d", l.getMes());
        gen.writeStringField("DATA", mm + "/" + l.getAno());
        gen.writeEndObject();
    }

    @Transactional(readOnly = true)
    public void streamCsvByIds(OutputStream os, List<Long> ids) throws IOException {
        List<Long> clean = sanitizeIds(ids);

        try (var writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.write("id,descricao,valor,ano,mes,tipo,status,data");
            writer.newLine();

            for (List<Long> chunk : chunksOf(clean, CHUNK_SIZE)) {
                List<Lancamento> lote = lancamentoRepository.findAllByIdInOrderByIdAsc(chunk);
                for (Lancamento l : lote) {
                    String dataStr = String.format("%02d/%d", l.getMes(), l.getAno());
                    writer.write(String.join(",",
                            csv(l.getId()),
                            csv(l.getDescricao()),
                            csv(numberSafe(l.getValor())),
                            csv(l.getAno()),
                            csv(l.getMes()),
                            csv(l.getTipoLancamento() == null ? null : l.getTipoLancamento().name()),
                            csv(l.getStatusLancamento() == null ? null : l.getStatusLancamento().name()),
                            csv(dataStr)
                    ));
                    writer.newLine();
                }
                writer.flush();
            }
        }
    }

    private List<Long> sanitizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        for (Long id : ids) if (id != null) set.add(id);
        return new ArrayList<>(set);
    }

    private List<List<Long>> chunksOf(List<Long> source, int size) {
        List<List<Long>> out = new ArrayList<>();
        for (int i = 0; i < source.size(); i += size) {
            out.add(source.subList(i, Math.min(i + size, source.size())));
        }
        return out;
    }

    private double numberSafe(BigDecimal v) { return v == null ? 0.0 : v.doubleValue(); }

    private String csv(Object o) {
        String s = (o == null) ? "" : String.valueOf(o);
        boolean precisaAspas = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (s.contains("\"")) s = s.replace("\"", "\"\"");
        return precisaAspas ? "\"" + s + "\"" : s;
    }
}
