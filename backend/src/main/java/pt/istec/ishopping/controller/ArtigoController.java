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
import pt.istec.ishopping.dto.ArtigoAtualizarPedido;
import pt.istec.ishopping.dto.ArtigoCriarPedido;
import pt.istec.ishopping.dto.ArtigoResposta;
import pt.istec.ishopping.service.ArtigoService;

@RestController
@RequestMapping("/api/artigos")
public class ArtigoController {

    private final ArtigoService artigoService;

    public ArtigoController(ArtigoService artigoService) {
        this.artigoService = artigoService;
    }

    @GetMapping
    public List<ArtigoResposta> listar(@RequestParam(required = false) Integer idTipoArtigo) {
        return artigoService.listar(idTipoArtigo);
    }

    @GetMapping("/{id}")
    public ArtigoResposta obter(@PathVariable Integer id) {
        return artigoService.obter(id);
    }

    @PostMapping
    public ResponseEntity<ArtigoResposta> criar(@Valid @RequestBody ArtigoCriarPedido pedido) {
        ArtigoResposta criado = artigoService.criar(pedido);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(criado);
    }

    @PutMapping("/{id}")
    public ArtigoResposta atualizar(@PathVariable Integer id, @Valid @RequestBody ArtigoAtualizarPedido pedido) {
        return artigoService.atualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Integer id) {
        artigoService.apagar(id);
    }
}
