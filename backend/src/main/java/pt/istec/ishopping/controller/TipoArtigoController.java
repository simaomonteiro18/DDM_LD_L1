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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import pt.istec.ishopping.dto.TipoArtigoAtualizarPedido;
import pt.istec.ishopping.dto.TipoArtigoCriarPedido;
import pt.istec.ishopping.dto.TipoArtigoResposta;
import pt.istec.ishopping.service.TipoArtigoService;

@RestController
@RequestMapping("/api/tipos-artigo")
public class TipoArtigoController {

    private final TipoArtigoService tipoArtigoService;

    public TipoArtigoController(TipoArtigoService tipoArtigoService) {
        this.tipoArtigoService = tipoArtigoService;
    }

    @GetMapping
    public List<TipoArtigoResposta> listar() {
        return tipoArtigoService.listar();
    }

    @GetMapping("/{id}")
    public TipoArtigoResposta obter(@PathVariable Integer id) {
        return tipoArtigoService.obter(id);
    }

    @PostMapping
    public ResponseEntity<TipoArtigoResposta> criar(@Valid @RequestBody TipoArtigoCriarPedido pedido) {
        TipoArtigoResposta criado = tipoArtigoService.criar(pedido);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(criado);
    }

    @PutMapping("/{id}")
    public TipoArtigoResposta atualizar(@PathVariable Integer id, @Valid @RequestBody TipoArtigoAtualizarPedido pedido) {
        return tipoArtigoService.atualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Integer id) {
        tipoArtigoService.apagar(id);
    }
}
