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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LancamentoExportServiceImpl implements LancamentoExportService {

	private static final int TAMANHO_LOTE = 2000;

	private final LancamentoRepository lancamentoRepository;

	@Override
	@Transactional(readOnly = true)
	public void exportarJsonPorIds(OutputStream outputStream, List<Long> ids) throws IOException {
		List<Long> idsHigienizados = higienizarIds(ids);
		JsonFactory jf = new JsonFactory();
		try (JsonGenerator gen = jf.createGenerator(outputStream)) {
			gen.writeStartArray();
			for (List<Long> loteIds : particionar(idsHigienizados, TAMANHO_LOTE)) {
				escreverJsonDoLote(gen, loteIds);
				gen.flush();
			}
			gen.writeEndArray();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void exportarCsvPorIds(OutputStream outputStream, List<Long> ids) throws IOException {
		List<Long> idsHigienizados = higienizarIds(ids);
		try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
			writer.write("DESC,VALOR_LANC,TIPO,STATUS,USUARIO,DATA_LANC,CATEGORIA");
			writer.newLine();
			for (List<Long> loteIds : particionar(idsHigienizados, TAMANHO_LOTE)) {
				escreverCsvDoLote(writer, loteIds);
				writer.flush();
			}
		}
	}

	private void escreverJsonDoLote(JsonGenerator gen, List<Long> idsLote) throws IOException {
		List<Lancamento> lote = lancamentoRepository.findAllByIdInOrderByIdAsc(idsLote);
		for (Lancamento l : lote) {
			escreverJson(gen, l);
		}
	}

	private void escreverJson(JsonGenerator gen, Lancamento lancamento) throws IOException {
		gen.writeStartObject();
		gen.writeNumberField("id", lancamento.getId());
		if (lancamento.getDescricao() != null) {
			gen.writeStringField("descricao", lancamento.getDescricao());
		}
		gen.writeNumberField("valor", numeroSeguro(lancamento.getValor()));
		if (lancamento.getAno() != null) {
			gen.writeNumberField("ano", lancamento.getAno());
		}
		if (lancamento.getMes() != null) {
			gen.writeNumberField("mes", lancamento.getMes());
		}
		if (lancamento.getTipoLancamento() != null) {
			gen.writeStringField("tipoLancamento", lancamento.getTipoLancamento().name());
		}
		if (lancamento.getStatusLancamento() != null) {
			gen.writeStringField("statusLancamento", lancamento.getStatusLancamento().name());
		}
		gen.writeStringField("data", dataSegura(lancamento.getMes(), lancamento.getAno()));
		escreverArrayCategorias(gen, "categorias", lancamento.getCategorias());
		gen.writeEndObject();
	}

	private void escreverArrayCategorias(JsonGenerator gen, String nomeCampo, Iterable<Categoria> categorias)
			throws IOException {
		gen.writeArrayFieldStart(nomeCampo);
		if (categorias != null) {
			for (Categoria categoria : categorias) {
				if (categoria != null && categoria.getNome() != null && !categoria.getNome().isBlank()) {
					gen.writeString(categoria.getNome());
				}
			}
		}
		gen.writeEndArray();
	}

	private void escreverCsvDoLote(BufferedWriter writer, List<Long> idsLote) throws IOException {
		List<Lancamento> lote = lancamentoRepository.findAllByIdInOrderByIdAsc(idsLote);
		for (Lancamento lancamento : lote) {
			String dataStr = dataSegura(lancamento.getMes(), lancamento.getAno());
			String usuarioStr = resolverUsuario(lancamento);
			String categoriasStr = resolverCategorias(lancamento);

			writer.write(String.join(",", csv(lancamento.getDescricao()), csv(numeroSeguro(lancamento.getValor())),
					csv(lancamento.getTipoLancamento() == null ? null : lancamento.getTipoLancamento().name()),
					csv(lancamento.getStatusLancamento() == null ? null : lancamento.getStatusLancamento().name()),
					csv(usuarioStr), csv(dataStr), csv(categoriasStr)));
			writer.newLine();
		}
	}

	private String resolverUsuario(Lancamento lancamento) {
		if (lancamento.getUsuario() == null)
			return "";
		if (lancamento.getUsuario().getEmail() != null && !lancamento.getUsuario().getEmail().isBlank()) {
			return lancamento.getUsuario().getEmail();
		}
		if (lancamento.getUsuario().getNome() != null && !lancamento.getUsuario().getNome().isBlank()) {
			return lancamento.getUsuario().getNome();
		}
		return lancamento.getUsuario().getId() != null ? String.valueOf(lancamento.getUsuario().getId()) : "";
	}

	private String resolverCategorias(Lancamento lancamento) {
		if (lancamento.getCategorias() == null || lancamento.getCategorias().isEmpty())
			return "";
		return lancamento.getCategorias()
			.stream()
			.filter(Objects::nonNull)
			.map(Categoria::getNome)
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(s -> !s.isBlank())
			.distinct()
			.reduce((a, b) -> a + "|" + b)
			.orElse("");
	}

	private List<Long> higienizarIds(List<Long> ids) {
		if (ids == null || ids.isEmpty())
			return List.of();
		return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
	}

	private List<List<Long>> particionar(List<Long> fonte, int tamanho) {
		List<List<Long>> out = new ArrayList<>();
		for (int i = 0; i < fonte.size(); i += tamanho) {
			out.add(fonte.subList(i, Math.min(i + tamanho, fonte.size())));
		}
		return out;
	}

	private double numeroSeguro(BigDecimal valor) {
		return valor == null ? 0.0 : valor.doubleValue();
	}

	private String dataSegura(Integer mes, Integer ano) {
		String mm = (mes == null) ? "null" : String.format("%02d", mes);
		String aa = (ano == null) ? "null" : String.valueOf(ano);
		return mm + "/" + aa;
	}

	private String csv(Object valor) {
		String s = (valor == null) ? "" : String.valueOf(valor);
		boolean precisaAspas = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
		if (s.contains("\""))
			s = s.replace("\"", "\"\"");
		return precisaAspas ? "\"" + s + "\"" : s;
	}

}
