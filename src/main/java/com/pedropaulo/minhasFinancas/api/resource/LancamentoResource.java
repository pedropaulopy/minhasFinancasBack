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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lancamentos")
@RequiredArgsConstructor
public class LancamentoResource {
    private final LancamentoService service;
    private final UsuarioService usuarioService;



    private Lancamento converter(LancamentoDTO dto) throws RegraNegocioException {
        Lancamento lancamento = new Lancamento();
        lancamento.setDescricao(dto.getDescricao());
        lancamento.setMes(dto.getMes());
        lancamento.setAno(dto.getAno());
        lancamento.setValor(dto.getValor());

        Usuario usuario = usuarioService.obterPorId(dto.getUsuario()).orElseThrow(() -> new RegraNegocioException("Usuário não encontrado com o ID informado."));

        lancamento.setUsuario(usuario);
        lancamento.setTipoLancamento(TipoLancamento.valueOf(dto.getTipoLancamento()));
        lancamento.setStatusLancamento(StatusLancamento.valueOf(dto.getStatusLancamento()));
        return lancamento;
    }
    @PostMapping("/salvar")
    public ResponseEntity salvar(@RequestBody LancamentoDTO dto){
        try{
            Lancamento entidade = converter(dto);
            entidade = service.salvar(entidade);
            return new ResponseEntity(entidade, HttpStatus.CREATED);
        }catch(RegraNegocioException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("atualizar/{id}")
    public ResponseEntity atualizar(@PathVariable Long id, @RequestBody LancamentoDTO dto){
        return service.obterPorId(id).map(entity ->{
            try {
                Lancamento lancamento = converter(dto);
                lancamento.setId(entity.getId());
                service.atualizar(lancamento);
                return new ResponseEntity(lancamento, HttpStatus.OK);
            } catch (RegraNegocioException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElseGet(() -> ResponseEntity.badRequest().body("Lançamento não encontrado."));
    }

    @PutMapping("atualizar_status/{id}")
    public ResponseEntity atualizarStatus(@PathVariable Long id, @RequestBody LancamentoStatusDTO dto){
        return service.obterPorId(id).map(entity ->{
            StatusLancamento statusSelecionado = StatusLancamento.valueOf(dto.getStatus());
            if(statusSelecionado == null){
                return ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
            }
            try {
                entity.setStatusLancamento(statusSelecionado);
                service.atualizar(entity);
                return new ResponseEntity(entity, HttpStatus.OK);
            } catch (RegraNegocioException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElseGet(() -> ResponseEntity.badRequest().body("Lançamento não encontrado."));
    }

    @DeleteMapping("deletar/{id}")
    public ResponseEntity deletar(@PathVariable Long id, @RequestBody LancamentoDTO dto){
        return service.obterPorId(id).map(entity ->{
            service.deletar(entity);
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }).orElseGet(() ->
                new ResponseEntity("Lançamento não encontrado.", HttpStatus.BAD_REQUEST));
    }

    @GetMapping("/buscar")
    public List<Lancamento> buscar(
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam(value = "valor", required = false) BigDecimal valor,
            @RequestParam(value = "tipo_lancamento",  required = false) TipoLancamento tipoLancamento,
            @RequestParam(value = "status_lancamento", required = false)  StatusLancamento status,
            @RequestParam("usuario") Long idUsuario) throws RegraNegocioException {

        Lancamento lancamentoFiltro = new Lancamento();
        lancamentoFiltro.setDescricao(descricao);
        lancamentoFiltro.setMes(mes);
        lancamentoFiltro.setAno(ano);
        lancamentoFiltro.setValor(valor);
        lancamentoFiltro.setTipoLancamento(tipoLancamento);
        lancamentoFiltro.setStatusLancamento(status);
        Optional<Usuario> usuario = usuarioService.obterPorId(idUsuario);
        if(usuario.isPresent()){
            lancamentoFiltro.setUsuario(usuario.get());
            return service.buscar(lancamentoFiltro);
        }
        else{
            throw new RegraNegocioException("Usuário não encontrado para o ID informado.");
        }
    }
}
