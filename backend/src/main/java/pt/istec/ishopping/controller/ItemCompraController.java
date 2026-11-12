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
import pt.istec.ishopping.dto.ItemCompraAtualizarPedido;
import pt.istec.ishopping.dto.ItemCompraResposta;
import pt.istec.ishopping.dto.ItemNaoPrevistoCriarPedido;
import pt.istec.ishopping.dto.ItemPrevistoCriarPedido;
import pt.istec.ishopping.service.ItemCompraService;

@RestController
@RequestMapping("/api/compras/{idCompra}/itens")
public class ItemCompraController {

    private final ItemCompraService itemCompraService;

    public ItemCompraController(ItemCompraService itemCompraService) {
        this.itemCompraService = itemCompraService;
    }

    @GetMapping
    public List<ItemCompraResposta> listar(@PathVariable Integer idCompra) {
        return itemCompraService.listar(idCompra);
    }

    @GetMapping("/{idItem}")
    public ItemCompraResposta obter(@PathVariable Integer idCompra, @PathVariable Integer idItem) {
        return itemCompraService.obter(idCompra, idItem);
    }

    @PostMapping("/previstos")
    public ResponseEntity<ItemCompraResposta> criarPrevisto(@PathVariable Integer idCompra,
            @Valid @RequestBody ItemPrevistoCriarPedido pedido) {
        return criado(idCompra, itemCompraService.criarPrevisto(idCompra, pedido));
    }

    @PostMapping("/nao-previstos")
    public ResponseEntity<ItemCompraResposta> criarNaoPrevisto(@PathVariable Integer idCompra,
            @Valid @RequestBody ItemNaoPrevistoCriarPedido pedido) {
        return criado(idCompra, itemCompraService.criarNaoPrevisto(idCompra, pedido));
    }

    @PutMapping("/{idItem}")
    public ItemCompraResposta atualizar(@PathVariable Integer idCompra, @PathVariable Integer idItem,
            @Valid @RequestBody ItemCompraAtualizarPedido pedido) {
        return itemCompraService.atualizar(idCompra, idItem, pedido);
    }

    @DeleteMapping("/{idItem}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Integer idCompra, @PathVariable Integer idItem) {
        itemCompraService.apagar(idCompra, idItem);
    }

    private ResponseEntity<ItemCompraResposta> criado(Integer idCompra, ItemCompraResposta criado) {
        URI localizacao = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/compras/{idCompra}/itens/{idItem}")
                .buildAndExpand(idCompra, criado.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(criado);
    }
}
