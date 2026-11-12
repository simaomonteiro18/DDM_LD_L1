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
import pt.istec.ishopping.dto.CompraAtualizarPedido;
import pt.istec.ishopping.dto.CompraCriarPedido;
import pt.istec.ishopping.dto.CompraResposta;
import pt.istec.ishopping.service.CompraService;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping
    public List<CompraResposta> listar(@RequestParam(required = false) Boolean fechada) {
        return compraService.listar(fechada);
    }

    @GetMapping("/{id}")
    public CompraResposta obter(@PathVariable Integer id) {
        return compraService.obter(id);
    }

    @PostMapping
    public ResponseEntity<CompraResposta> criar(@Valid @RequestBody CompraCriarPedido pedido) {
        CompraResposta criado = compraService.criar(pedido);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(criado);
    }

    @PutMapping("/{id}")
    public CompraResposta atualizar(@PathVariable Integer id, @Valid @RequestBody CompraAtualizarPedido pedido) {
        return compraService.atualizar(id, pedido);
    }

    @PostMapping("/{id}/fechar")
    public CompraResposta fechar(@PathVariable Integer id) {
        return compraService.fechar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Integer id) {
        compraService.apagar(id);
    }
}
