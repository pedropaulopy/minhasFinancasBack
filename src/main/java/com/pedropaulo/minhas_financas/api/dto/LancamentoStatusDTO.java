package com.pedropaulo.minhas_financas.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LancamentoStatusDTO {

	private String status;

	public String getStatus() {
		return status;
	}

}
