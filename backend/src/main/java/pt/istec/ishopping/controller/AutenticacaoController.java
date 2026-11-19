package pt.istec.ishopping.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.istec.ishopping.dto.LoginPedido;
import pt.istec.ishopping.dto.LoginResposta;
import pt.istec.ishopping.service.AutenticacaoService;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/login")
    public LoginResposta login(@Valid @RequestBody LoginPedido pedido) {
        return autenticacaoService.login(pedido);
    }
}
