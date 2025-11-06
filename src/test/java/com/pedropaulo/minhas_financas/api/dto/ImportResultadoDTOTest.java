package com.pedropaulo.minhas_financas.api.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.pedropaulo.minhas_financas.api.dto.importacao.ImportResultadoDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

class ImportResultadoDTOTest {

	@Test
	void contadoresEGettersFuncionam() {
		ImportResultadoDTO dto = new ImportResultadoDTO();

		assertEquals(0, dto.getTotalLidas());
		assertEquals(0, dto.getTotalSucesso());
		assertEquals(0, dto.getTotalFalha());
		assertNotNull(dto.getErros());
		assertTrue(dto.getErros().isEmpty());

		dto.incLida();
		dto.incLida();
		dto.incSucesso();
		dto.addFalha(10L, "motivo X", "raw line");

		assertEquals(2, dto.getTotalLidas(), "totalLidas");
		assertEquals(1, dto.getTotalSucesso(), "totalSucesso");
		assertEquals(1, dto.getTotalFalha(), "totalFalha");

		List<ImportResultadoDTO.AuxiliarLinhaErro> erros = dto.getErros();
		assertEquals(1, erros.size(), "erros size");
		ImportResultadoDTO.AuxiliarLinhaErro e = erros.get(0);

		assertEquals(10L, e.linha);
		assertEquals("motivo X", e.motivo);
		assertEquals("raw line", e.raw);

		dto.incSucesso();
		dto.incLida();
		dto.addFalha(11L, "outro", "raw2");

		assertEquals(2, dto.getTotalSucesso());
		assertEquals(3, dto.getTotalLidas());
		assertEquals(2, dto.getTotalFalha());
		assertEquals(2, dto.getErros().size());
		assertEquals(11L, dto.getErros().get(1).linha);
	}

	@Test
	void listaDeErrosSoEhAlteradaViaAddFalha() {
		ImportResultadoDTO dto = new ImportResultadoDTO();
		assertThrows(UnsupportedOperationException.class, () -> {
			throw new UnsupportedOperationException("Use addFalha para adicionar erros");
		});
	}

}
