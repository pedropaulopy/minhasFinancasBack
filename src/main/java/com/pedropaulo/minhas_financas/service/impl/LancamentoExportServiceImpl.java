package com.pedropaulo.minhas_financas.service.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.pedropaulo.minhas_financas.model.entity.Categoria;
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
				for (Lancamento l : lote)
					writeJson(gen, l);
				gen.flush();
			}

			gen.writeEndArray();
		}
	}

	private void writeJson(JsonGenerator gen, Lancamento lancamento) throws IOException {
		gen.writeStartObject();
		gen.writeNumberField("id", lancamento.getId());

		if (lancamento.getDescricao() != null)
			gen.writeStringField("descricao", lancamento.getDescricao());

		gen.writeNumberField("valor", numberSafe(lancamento.getValor()));

		if (lancamento.getAno() != null)
			gen.writeNumberField("ano", lancamento.getAno());

		if (lancamento.getMes() != null)
			gen.writeNumberField("mes", lancamento.getMes());

		if (lancamento.getTipoLancamento() != null)
			gen.writeStringField("tipoLancamento", lancamento.getTipoLancamento().name());

		if (lancamento.getStatusLancamento() != null)
			gen.writeStringField("statusLancamento", lancamento.getStatusLancamento().name());

		String mm = String.format("%02d", lancamento.getMes());
		gen.writeStringField("data", mm + "/" + lancamento.getAno());

		gen.writeArrayFieldStart("categorias");
		if (lancamento.getCategorias() != null && !lancamento.getCategorias().isEmpty()) {
			for (var categoria : lancamento.getCategorias()) {
				if (categoria != null && categoria.getNome() != null && !categoria.getNome().isBlank()) {
					gen.writeString(categoria.getNome());
				}
			}
		}
		gen.writeEndArray();

		gen.writeEndObject();
	}

	@Transactional(readOnly = true)
	public void streamCsvByIds(OutputStream outputStream, List<Long> ids) throws IOException {
		List<Long> clean = sanitizeIds(ids);

		try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
			writer.write("DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA");
			writer.newLine();

			for (List<Long> chunk : chunksOf(clean, CHUNK_SIZE)) {
				List<Lancamento> lote = lancamentoRepository.findAllByIdInOrderByIdAsc(chunk);
				for (Lancamento lancamento : lote) {
					String dataStr = String.format("%02d/%d", lancamento.getMes(), lancamento.getAno());

					String usuarioStr = "";
					if (lancamento.getUsuario() != null) {
						if (lancamento.getUsuario().getEmail() != null
								&& !lancamento.getUsuario().getEmail().isBlank()) {
							usuarioStr = lancamento.getUsuario().getEmail();
						}
						else if (lancamento.getUsuario().getNome() != null
								&& !lancamento.getUsuario().getNome().isBlank()) {
							usuarioStr = lancamento.getUsuario().getNome();
						}
						else if (lancamento.getUsuario().getId() != null) {
							usuarioStr = String.valueOf(lancamento.getUsuario().getId());
						}
					}

					String categoriasStr = "";
					if (lancamento.getCategorias() != null && !lancamento.getCategorias().isEmpty()) {
						categoriasStr = lancamento.getCategorias()
							.stream()
							.filter(c -> c != null && c.getNome() != null && !c.getNome().isBlank())
							.map(Categoria::getNome)
							.distinct()
							.reduce((a, b) -> a + "|" + b)
							.orElse("");
					}

					writer.write(String.join(",", csv(lancamento.getDescricao()),
							csv(numberSafe(lancamento.getValor())),
							csv(lancamento.getTipoLancamento() == null ? null : lancamento.getTipoLancamento().name()),
							csv(lancamento.getStatusLancamento() == null ? null
									: lancamento.getStatusLancamento().name()),
							csv(usuarioStr), csv(dataStr), csv(categoriasStr)));
					writer.newLine();
				}
				writer.flush();
			}
		}
	}

	private List<Long> sanitizeIds(List<Long> ids) {
		if (ids == null || ids.isEmpty())
			return List.of();
		LinkedHashSet<Long> set = new LinkedHashSet<>();
		for (Long id : ids)
			if (id != null)
				set.add(id);
		return new ArrayList<>(set);
	}

	private List<List<Long>> chunksOf(List<Long> source, int size) {
		List<List<Long>> out = new ArrayList<>();
		for (int i = 0; i < source.size(); i += size) {
			out.add(source.subList(i, Math.min(i + size, source.size())));
		}
		return out;
	}

	private double numberSafe(BigDecimal valor) {
		return valor == null ? 0.0 : valor.doubleValue();
	}

	private String csv(Object object) {
		String s = (object == null) ? "" : String.valueOf(object);
		boolean precisaAspas = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
		if (s.contains("\""))
			s = s.replace("\"", "\"\"");
		return precisaAspas ? "\"" + s + "\"" : s;
	}

}
