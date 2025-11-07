package com.pedropaulo.minhas_financas.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface LancamentoExportService {

    void exportarJsonPorIds(OutputStream os, List<Long> ids) throws IOException;

    void exportarCsvPorIds(OutputStream os, List<Long> ids) throws IOException;
}
