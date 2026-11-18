package pt.istec.ishopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.dto.EstatisticaCompraFechadaResposta;
import pt.istec.ishopping.dto.EstatisticaMesResposta;
import pt.istec.ishopping.exception.PedidoInvalidoException;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.repository.CompraRepository;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.repository.OrcamentoRepository;
import pt.istec.ishopping.repository.ResumoItensCompra;

@Service
public class EstatisticaService {

    private static final ResumoItensCompra SEM_ITENS = new ResumoItensCompra(null, 0L, 0L, 0L, null);

    private final OrcamentoRepository orcamentoRepository;
    private final CompraRepository compraRepository;
    private final ItemCompraRepository itemCompraRepository;
    private final GastoMensalService gastoMensal;

    public EstatisticaService(OrcamentoRepository orcamentoRepository, CompraRepository compraRepository,
            ItemCompraRepository itemCompraRepository, GastoMensalService gastoMensal) {
        this.orcamentoRepository = orcamentoRepository;
        this.compraRepository = compraRepository;
        this.itemCompraRepository = itemCompraRepository;
        this.gastoMensal = gastoMensal;
    }

    @Transactional(readOnly = true)
    public List<EstatisticaMesResposta> meses(Integer ano) {
        int anoPedido = ano == null ? gastoMensal.hoje().getYear() : ano;
        if (anoPedido < 2000 || anoPedido > 2100) {
            throw new PedidoInvalidoException("O ano tem de estar entre 2000 e 2100.");
        }
        Map<Integer, BigDecimal> orcamentos = orcamentoRepository.findByAno(anoPedido).stream()
                .collect(Collectors.toMap(Orcamento::getMes, Orcamento::getValor));
        Map<Integer, BigDecimal> gastos = gastoMensal.porMesDoAno(anoPedido);

        List<EstatisticaMesResposta> meses = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            BigDecimal orcamento = orcamentos.get(mes) == null
                    ? null
                    : orcamentos.get(mes).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gasto = gastos.get(mes);
            meses.add(new EstatisticaMesResposta(mes, orcamento, gasto,
                    orcamento == null ? null : orcamento.subtract(gasto)));
        }
        return meses;
    }

    @Transactional(readOnly = true)
    public List<EstatisticaCompraFechadaResposta> comprasFechadas() {
        List<Compra> compras = compraRepository.findByFechadaTrueOrderByDataFechadaDescIdDesc();
        if (compras.isEmpty()) {
            return List.of();
        }
        // Uma so consulta agregada para os itens de todas as compras da lista
        Map<Integer, ResumoItensCompra> resumos = itemCompraRepository
                .resumirPorCompra(compras.stream().map(Compra::getId).toList()).stream()
                .collect(Collectors.toMap(ResumoItensCompra::idCompra, Function.identity()));
        return compras.stream()
                .map(compra -> resumir(compra, resumos.getOrDefault(compra.getId(), SEM_ITENS)))
                .toList();
    }

    private EstatisticaCompraFechadaResposta resumir(Compra compra, ResumoItensCompra resumo) {
        long total = resumo.totalItens();
        BigDecimal gasto = (resumo.totalGasto() == null ? BigDecimal.ZERO : resumo.totalGasto())
                .setScale(2, RoundingMode.HALF_UP);
        // A dos nao previstos e o complemento da dos previstos, para somarem sempre 100
        int percentagemPrevistos = total == 0
                ? 0
                : BigDecimal.valueOf(resumo.itensPrevistos() * 100)
                        .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP).intValue();
        int percentagemNaoPrevistos = total == 0 ? 0 : 100 - percentagemPrevistos;
        return new EstatisticaCompraFechadaResposta(compra.getId(), compra.getNome(), compra.getDataCriacao(),
                compra.getDataFechada(), gasto, total, resumo.itensPrevistos(), resumo.itensNaoPrevistos(),
                percentagemPrevistos, percentagemNaoPrevistos);
    }
}
