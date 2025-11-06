package com.pedropaulo.minhas_financas.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface LancamentoExportService {

	void streamJsonByIds(OutputStream os, List<Long> ids) throws IOException;

	void streamCsvByIds(OutputStream os, List<Long> ids) throws IOException;

}
