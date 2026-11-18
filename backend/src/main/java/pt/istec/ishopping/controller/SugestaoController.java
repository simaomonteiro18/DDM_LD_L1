package pt.istec.ishopping.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.istec.ishopping.dto.SugestaoArtigoResposta;
import pt.istec.ishopping.dto.SugestaoOrcamentoResposta;
import pt.istec.ishopping.service.SugestaoService;

@RestController
@RequestMapping("/api/sugestoes")
public class SugestaoController {

    private final SugestaoService sugestaoService;

    public SugestaoController(SugestaoService sugestaoService) {
        this.sugestaoService = sugestaoService;
    }

    @GetMapping("/orcamento")
    public SugestaoOrcamentoResposta orcamento(@RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes) {
        return sugestaoService.orcamento(ano, mes);
    }

    // A semana e validada no servico, para a mensagem de erro ser especifica quando falta
    @GetMapping("/lista-compras")
    public List<SugestaoArtigoResposta> listaCompras(@RequestParam(required = false) Integer semana) {
        return sugestaoService.listaCompras(semana);
    }
}
