package com.pedropaulo.minhas_financas.api.resource;

import com.pedropaulo.minhas_financas.api.dto.LancamentoDTO;
import com.pedropaulo.minhas_financas.api.dto.LancamentoStatusDTO;
import com.pedropaulo.minhas_financas.api.dto.exportacao.exportLancamentosDTO;
import com.pedropaulo.minhas_financas.api.dto.exportacao.exportLancamentosSheetsDTO;
import com.pedropaulo.minhas_financas.api.dto.exportacao.exportSheetsErrosDTO;
import com.pedropaulo.minhas_financas.api.dto.exportacao.exportSheetsResultadoDTO;
import com.pedropaulo.minhas_financas.api.dto.importacao.ImportResultadoDTO;
import com.pedropaulo.minhas_financas.exception.EntidadeNaoProcessavelException;
import com.pedropaulo.minhas_financas.exception.RegraNegocioException;
import com.pedropaulo.minhas_financas.model.entity.Lancamento;
import com.pedropaulo.minhas_financas.model.entity.Usuario;
import com.pedropaulo.minhas_financas.model.enums.StatusLancamento;
import com.pedropaulo.minhas_financas.model.enums.TipoLancamento;
import com.pedropaulo.minhas_financas.service.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/lancamentos")
@RequiredArgsConstructor
public class LancamentoResource {

	private final LancamentoService service;

	private final UsuarioService usuarioService;

	private final LancamentoCsvImportService importService;

    private final LancamentoExportService exportService;

    private final GoogleSheetsExport sheetsExport;

	@PostMapping()
	public ResponseEntity salvar(@RequestBody LancamentoDTO dto, Authentication authentication) {
		try {
			Lancamento entidade = service.converterDTO(dto, authentication);
			entidade = service.salvar(entidade);
			return new ResponseEntity(entidade, HttpStatus.CREATED);
		}
		catch (RegraNegocioException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("{id}")
	public ResponseEntity atualizar(@PathVariable Long id, @RequestBody LancamentoDTO dto,
			Authentication authentication) {
		try {
			Lancamento lancamentoAtualizado = service.atualizar(id, authentication, dto);
			return ResponseEntity.ok(lancamentoAtualizado);
		}
		catch (EntidadeNaoProcessavelException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
		}
		catch (RegraNegocioException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("{id}/atualizar-status")
	public ResponseEntity atualizarStatus(@PathVariable Long id, @RequestBody LancamentoStatusDTO dto,
			Authentication authentication) {
		try {
			service.atualizarStatus(id, authentication, StatusLancamento.valueOf(dto.getStatus()));
			return new ResponseEntity(HttpStatus.CREATED);
		}
		catch (EntidadeNaoProcessavelException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
		}
		catch (RegraNegocioException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("{id}")
	public ResponseEntity deletar(@PathVariable Long id, Authentication authentication) {
		try {
			service.deletar(id, authentication);
			return new ResponseEntity(HttpStatus.NO_CONTENT);
		}
		catch (EntidadeNaoProcessavelException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
		}
		catch (RegraNegocioException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping
	public ResponseEntity<List<Lancamento>> buscar(
			@RequestParam(value = "descricao", required = false) String descricao,
			@RequestParam(value = "mes", required = false) Integer mes,
			@RequestParam(value = "ano", required = false) Integer ano,
			@RequestParam(value = "valor", required = false) BigDecimal valor,
			@RequestParam(value = "tipo_lancamento", required = false) TipoLancamento tipoLancamento,
			@RequestParam(value = "status_lancamento", required = false) StatusLancamento status,
			@RequestParam(value = "categoriaId", required = false) List<Long> categoriaIds,
			Authentication authentication) throws RegraNegocioException {
		String email = authentication.getName();
		Usuario usuario = usuarioService.obterIdUsuarioPorEmail(email);

		Lancamento lancamentoFiltro = new Lancamento();
		lancamentoFiltro.setDescricao(descricao);
		lancamentoFiltro.setMes(mes);
		lancamentoFiltro.setAno(ano);
		lancamentoFiltro.setValor(valor);
		lancamentoFiltro.setTipoLancamento(tipoLancamento);
		lancamentoFiltro.setStatusLancamento(status);
		lancamentoFiltro.setUsuario(usuario);

		List<Lancamento> lancamentos = service.buscar(lancamentoFiltro, categoriaIds);
		return ResponseEntity.ok(lancamentos);
	}

	@GetMapping("{id}")
	public ResponseEntity<?> obterLancamento(@PathVariable("id") Long id, Authentication authentication) {
		try {
			Lancamento lancamento = service.obterPorIdLancamento(id, authentication);
			return new ResponseEntity(lancamento, HttpStatus.OK);
		}
		catch (RegraNegocioException e) {
			return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@PostMapping(value = "/upload", consumes = "multipart/form-data")
	public ResponseEntity<ImportResultadoDTO> importarLancamentos(@RequestParam("file") MultipartFile file,
			Authentication authentication) {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		try (var in = file.getInputStream()) {
			final int TAMANHO_LOTE = 1000;
			Long usuarioAutenticadoId = usuarioService.obterIdUsuarioPorEmail(authentication.getName()).getId();
			ImportResultadoDTO resultado = importService.importar(in, TAMANHO_LOTE, usuarioAutenticadoId);

			HttpStatus status = resultado.getTotalFalha() > 0 ? HttpStatus.MULTI_STATUS : HttpStatus.OK;
			return new ResponseEntity<>(resultado, status);

		}
		catch (Exception e) {
			ImportResultadoDTO erro = new ImportResultadoDTO();
			erro.addFalha(0, "Erro ao processar arquivo: " + e.getMessage(), "");
			return new ResponseEntity<>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    //nesse caso é válido usar post pq os ids vão ser passados via body (json)
    @PostMapping(value = "/export/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> exportJson(@RequestBody exportLancamentosDTO dto) {
        List<Long> ids = dto == null ? List.of() : dto.getIdsRequisitados();
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        StreamingResponseBody stream = os -> exportService.streamJsonByIds(os, ids);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=lancamentos_JSON.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(stream);
    }

    @PostMapping(value = "/export/csv", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportCsv(@RequestBody exportLancamentosDTO dto) {
        List<Long> ids = dto == null ? List.of() : dto.getIdsRequisitados();
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        StreamingResponseBody stream = os -> exportService.streamCsvByIds(os, ids);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=lancamentos_CSV.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(stream);
    }

    @PostMapping(value = "/export/sheets", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exportToGoogleSheets(@RequestBody exportLancamentosSheetsDTO dto) {
        List<Long> ids = (dto == null) ? List.of() : dto.getIdsRequisitados();
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            var created = sheetsExport.createSheetFromCsv(ids, dto.getNomePlanilha(), dto.getFolderId());
      return ResponseEntity.ok(
          new exportSheetsResultadoDTO(created.id(), created.webViewLink(), created.webContentLink()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new exportSheetsErrosDTO(e.getMessage()));
        }
    }

}
