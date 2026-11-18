package pt.istec.ishopping.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import pt.istec.ishopping.dto.OrcamentoAtualizarPedido;
import pt.istec.ishopping.dto.OrcamentoCriarPedido;
import pt.istec.ishopping.dto.OrcamentoDisponivelResposta;
import pt.istec.ishopping.dto.OrcamentoResposta;
import pt.istec.ishopping.service.OrcamentoService;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @GetMapping
    public List<OrcamentoResposta> listar(@RequestParam(required = false) Integer ano) {
        return orcamentoService.listar(ano);
    }

    @GetMapping("/disponivel")
    public OrcamentoDisponivelResposta disponivel(@RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes) {
        return orcamentoService.disponivel(ano, mes);
    }

    @GetMapping("/{id}")
    public OrcamentoResposta obter(@PathVariable Integer id) {
        return orcamentoService.obter(id);
    }

    @PostMapping
    public ResponseEntity<OrcamentoResposta> criar(@Valid @RequestBody OrcamentoCriarPedido pedido) {
        OrcamentoResposta criado = orcamentoService.criar(pedido);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(criado);
    }

    @PutMapping("/{id}")
    public OrcamentoResposta atualizar(@PathVariable Integer id, @Valid @RequestBody OrcamentoAtualizarPedido pedido) {
        return orcamentoService.atualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Integer id) {
        orcamentoService.apagar(id);
    }
}
