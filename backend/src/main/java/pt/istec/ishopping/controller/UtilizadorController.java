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
import pt.istec.ishopping.dto.UtilizadorAtualizarPedido;
import pt.istec.ishopping.dto.UtilizadorCriarPedido;
import pt.istec.ishopping.dto.UtilizadorResposta;
import pt.istec.ishopping.service.UtilizadorService;

@RestController
@RequestMapping("/api/utilizadores")
public class UtilizadorController {

    private final UtilizadorService utilizadorService;

    public UtilizadorController(UtilizadorService utilizadorService) {
        this.utilizadorService = utilizadorService;
    }

    @GetMapping
    public List<UtilizadorResposta> listar() {
        return utilizadorService.listar();
    }

    @GetMapping("/{id}")
    public UtilizadorResposta obter(@PathVariable Integer id) {
        return utilizadorService.obter(id);
    }

    @PostMapping
    public ResponseEntity<UtilizadorResposta> criar(@Valid @RequestBody UtilizadorCriarPedido pedido) {
        UtilizadorResposta criado = utilizadorService.criar(pedido);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(criado);
    }

    @PutMapping("/{id}")
    public UtilizadorResposta atualizar(@PathVariable Integer id, @Valid @RequestBody UtilizadorAtualizarPedido pedido) {
        return utilizadorService.atualizar(id, pedido);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Integer id) {
        utilizadorService.apagar(id);
    }
}
