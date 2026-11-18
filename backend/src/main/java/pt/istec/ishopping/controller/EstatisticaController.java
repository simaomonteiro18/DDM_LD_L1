package pt.istec.ishopping.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.istec.ishopping.dto.EstatisticaCompraFechadaResposta;
import pt.istec.ishopping.dto.EstatisticaMesResposta;
import pt.istec.ishopping.service.EstatisticaService;

@RestController
@RequestMapping("/api/estatisticas")
public class EstatisticaController {

    private final EstatisticaService estatisticaService;

    public EstatisticaController(EstatisticaService estatisticaService) {
        this.estatisticaService = estatisticaService;
    }

    @GetMapping("/meses")
    public List<EstatisticaMesResposta> meses(@RequestParam(required = false) Integer ano) {
        return estatisticaService.meses(ano);
    }

    @GetMapping("/compras-fechadas")
    public List<EstatisticaCompraFechadaResposta> comprasFechadas() {
        return estatisticaService.comprasFechadas();
    }
}
