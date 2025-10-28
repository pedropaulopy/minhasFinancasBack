package com.pedropaulo.minhasFinancas.api.resource;

import com.pedropaulo.minhasFinancas.api.dto.LancamentoDTO;
import com.pedropaulo.minhasFinancas.api.dto.LancamentoStatusDTO;
import com.pedropaulo.minhasFinancas.exception.RegraNegocioException;
import com.pedropaulo.minhasFinancas.model.entity.Lancamento;
import com.pedropaulo.minhasFinancas.model.entity.Usuario;
import com.pedropaulo.minhasFinancas.model.enums.StatusLancamento;
import com.pedropaulo.minhasFinancas.model.enums.TipoLancamento;
import com.pedropaulo.minhasFinancas.service.LancamentoService;
import com.pedropaulo.minhasFinancas.service.UsuarioService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lancamentos")
@RequiredArgsConstructor
public class LancamentoResource {
  private final LancamentoService service;
  private final UsuarioService usuarioService;

  @PostMapping("/salvar")
  public ResponseEntity salvar(@RequestBody LancamentoDTO dto) {
    try {
      Lancamento entidade = service.converterDTO(dto);
      entidade = service.salvar(entidade);
      return new ResponseEntity(entidade, HttpStatus.CREATED);
    } catch (RegraNegocioException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PutMapping("{id}/atualizar")
  public ResponseEntity atualizar(@PathVariable Long id, @RequestBody LancamentoDTO dto){
      try {
          Lancamento lancamentoAtualizado = service.atualizar(id, dto);
          return ResponseEntity.ok(lancamentoAtualizado);

      } catch (RegraNegocioException e) {
          return ResponseEntity.badRequest().body(e.getMessage());
      }
  }

  @PutMapping("{id}/atualizar_status")
  public ResponseEntity atualizarStatus(
      @PathVariable Long id, @RequestBody LancamentoStatusDTO dto){
        try{
            service.atualizarStatus(id, StatusLancamento.valueOf(dto.getStatus()));
            return new ResponseEntity(HttpStatus.CREATED);
        }catch(RegraNegocioException e){
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

  @DeleteMapping("{id}/deletar")
  public ResponseEntity deletar(@PathVariable Long id) {
      try{
          service.deletar(id);
          return new ResponseEntity(HttpStatus.NO_CONTENT);
      }catch (RegraNegocioException e){
          return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
      }
  }

  @GetMapping("/buscar")
  public List<Lancamento> buscar(
      @RequestParam(value = "descricao", required = false) String descricao,
      @RequestParam(value = "mes", required = false) Integer mes,
      @RequestParam(value = "ano", required = false) Integer ano,
      @RequestParam(value = "valor", required = false) BigDecimal valor,
      @RequestParam(value = "tipo_lancamento", required = false) TipoLancamento tipoLancamento,
      @RequestParam(value = "status_lancamento", required = false) StatusLancamento status,
      @RequestParam("usuario") Long idUsuario)
      throws RegraNegocioException {

    Lancamento lancamentoFiltro = new Lancamento();
    lancamentoFiltro.setDescricao(descricao);
    lancamentoFiltro.setMes(mes);
    lancamentoFiltro.setAno(ano);
    lancamentoFiltro.setValor(valor);
    lancamentoFiltro.setTipoLancamento(tipoLancamento);
    lancamentoFiltro.setStatusLancamento(status);
    usuarioService.obterPorId(idUsuario);
    return service.buscar(lancamentoFiltro);
  }

  @GetMapping("{id}/buscar")
  public ResponseEntity<?> obterLancamento(@PathVariable("id") Long id){
      try{
          service.obterPorIdLancamento(id);
          return new ResponseEntity(HttpStatus.OK);
      }catch(RegraNegocioException e){
          return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND);
      }
  }
}
