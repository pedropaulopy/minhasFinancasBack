package com.pedropaulo.minhas_financas.api.dto;

import com.pedropaulo.minhas_financas.api.dto.importacao.AuxiliarLinhaErro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class AuxiliarLinhaErroDTOTest {

	@Test
	@DisplayName("Constrói e expõe valores via getters")
	void constructorAndGetters() {
		long linha = 42L;
		String motivo = "Campo inválido";
		String raw = "42;abc;123";

		AuxiliarLinhaErro dto = new AuxiliarLinhaErro(linha, motivo, raw);

		assertEquals(linha, dto.getLinha());
		assertEquals(motivo, dto.getMotivo());
		assertEquals(raw, dto.getRaw());
	}

	@Test
	@DisplayName("Permite strings nulas e mantém o valor primitivo")
	void allowsNullStrings() {
		AuxiliarLinhaErro dto = new AuxiliarLinhaErro(7L, null, null);

		assertEquals(7L, dto.getLinha());
		assertNull(dto.getMotivo());
		assertNull(dto.getRaw());
	}

	@Test
	@DisplayName("É imutável: campos são final e não há setters gerados")
	void isImmutable_NoSettersAndFinalFields() throws NoSuchFieldException {
		// Campos são final
		Field fLinha = AuxiliarLinhaErro.class.getDeclaredField("linha");
		Field fMotivo = AuxiliarLinhaErro.class.getDeclaredField("motivo");
		Field fRaw = AuxiliarLinhaErro.class.getDeclaredField("raw");

		assertTrue(Modifier.isFinal(fLinha.getModifiers()), "linha deve ser final");
		assertTrue(Modifier.isFinal(fMotivo.getModifiers()), "motivo deve ser final");
		assertTrue(Modifier.isFinal(fRaw.getModifiers()), "raw deve ser final");

		assertThrows(NoSuchMethodException.class,
				() -> AuxiliarLinhaErro.class.getDeclaredMethod("setLinha", long.class));
		assertThrows(NoSuchMethodException.class,
				() -> AuxiliarLinhaErro.class.getDeclaredMethod("setMotivo", String.class));
		assertThrows(NoSuchMethodException.class,
				() -> AuxiliarLinhaErro.class.getDeclaredMethod("setRaw", String.class));
	}

}
