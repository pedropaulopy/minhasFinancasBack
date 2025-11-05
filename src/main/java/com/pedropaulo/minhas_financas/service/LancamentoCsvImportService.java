package com.pedropaulo.minhas_financas.service;

import com.pedropaulo.minhas_financas.api.dto.importacao.ImportResultadoDTO;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;

import java.io.InputStream;

public interface LancamentoCsvImportService {

	ImportResultadoDTO importar(InputStream inputStream, Long usuarioAutenticadoId) throws RegraNegocioException;

}
