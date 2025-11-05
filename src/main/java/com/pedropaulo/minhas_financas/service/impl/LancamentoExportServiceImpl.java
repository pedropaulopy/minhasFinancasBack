package com.pedropaulo.minhas_financas.service.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
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
    public void streamJsonByIds(OutputStream outputStreams, List<Long> ids) throws IOException {
        List<Long> clean = sanitizeIds(ids);

        JsonFactory jf = new JsonFactory();
        try (JsonGenerator gen = jf.createGenerator(outputStreams)) {
            gen.writeStartArray();

            for (List<Long> chunk : chunksOf(clean, CHUNK_SIZE)) {
                List<Lancamento> lote = lancamentoRepository.findAllByIdInOrderByIdAsc(chunk);
                for (Lancamento l : lote) writeJson(gen, l);
                gen.flush();
            }

            gen.writeEndArray();
        }
    }

    private void writeJson(JsonGenerator gen, Lancamento lancamento) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("ID LANÇAMENTO", lancamento.getId());
        if (lancamento.getDescricao() != null) gen.writeStringField("DESCRIÇÃO", lancamento.getDescricao());
        gen.writeNumberField("VALOR", numberSafe(lancamento.getValor()));
        if (lancamento.getAno() != null) gen.writeNumberField("ANO", lancamento.getAno());
        if (lancamento.getMes() != null) gen.writeNumberField("MÊS", lancamento.getMes());
        if (lancamento.getTipoLancamento() != null) gen.writeStringField("TIPO", lancamento.getTipoLancamento().name());
        if (lancamento.getStatusLancamento() != null) gen.writeStringField("STATUS", lancamento.getStatusLancamento().name());
        String mm = String.format("%02d", lancamento.getMes());
        gen.writeStringField("DATA", mm + "/" + lancamento.getAno());
        gen.writeEndObject();
    }

    @Transactional(readOnly = true)
    public void streamCsvByIds(OutputStream outputStream, List<Long> ids) throws IOException {
        List<Long> clean = sanitizeIds(ids);

        try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.write("ID LANÇAMENTO,DESCRIÇÃO,VALOR,ANO,MÊS,TIPO,STATUS,DATA");
            writer.newLine();

            for (List<Long> chunk : chunksOf(clean, CHUNK_SIZE)) {
                List<Lancamento> lote = lancamentoRepository.findAllByIdInOrderByIdAsc(chunk);
                for (Lancamento lancamento : lote) {
                    String dataStr = String.format("%02d/%d", lancamento.getMes(), lancamento.getAno());
                    writer.write(String.join(",",
                            csv(lancamento.getId()),
                            csv(lancamento.getDescricao()),
                            csv(numberSafe(lancamento.getValor())),
                            csv(lancamento.getAno()),
                            csv(lancamento.getMes()),
                            csv(lancamento.getTipoLancamento() == null ? null : lancamento.getTipoLancamento().name()),
                            csv(lancamento.getStatusLancamento() == null ? null : lancamento.getStatusLancamento().name()),
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

    private double numberSafe(BigDecimal valor) { return valor == null ? 0.0 : valor.doubleValue(); }

    private String csv(Object object) {
        String s = (object == null) ? "" : String.valueOf(object);
        boolean precisaAspas = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (s.contains("\"")) s = s.replace("\"", "\"\"");
        return precisaAspas ? "\"" + s + "\"" : s;
    }
}
