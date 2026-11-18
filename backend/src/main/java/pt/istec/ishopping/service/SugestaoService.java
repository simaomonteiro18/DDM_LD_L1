package pt.istec.ishopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.dto.SugestaoArtigoResposta;
import pt.istec.ishopping.dto.SugestaoOrcamentoResposta;
import pt.istec.ishopping.dto.TipoArtigoResposta;
import pt.istec.ishopping.exception.PedidoInvalidoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.repository.ItemAdquirido;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.repository.OrcamentoRepository;

@Service
public class SugestaoService {

    private static final int MESES_CONSIDERADOS = 6;
    private static final Collator ORDEM_NOMES = Collator.getInstance(Locale.of("pt", "PT"));

    private final OrcamentoRepository orcamentoRepository;
    private final ItemCompraRepository itemCompraRepository;
    private final GastoMensalService gastoMensal;

    public SugestaoService(OrcamentoRepository orcamentoRepository, ItemCompraRepository itemCompraRepository,
            GastoMensalService gastoMensal) {
        this.orcamentoRepository = orcamentoRepository;
        this.itemCompraRepository = itemCompraRepository;
        this.gastoMensal = gastoMensal;
    }

    // Media dos orcamentos dos ate 6 meses mais recentes anteriores ao mes alvo
    @Transactional(readOnly = true)
    public SugestaoOrcamentoResposta orcamento(Integer ano, Integer mes) {
        YearMonth seguinte = YearMonth.from(gastoMensal.hoje()).plusMonths(1);
        int anoAlvo = ano == null ? seguinte.getYear() : ano;
        int mesAlvo = mes == null ? seguinte.getMonthValue() : mes;
        if (anoAlvo < 2000 || anoAlvo > 2100) {
            throw new PedidoInvalidoException("O ano tem de estar entre 2000 e 2100.");
        }
        if (mesAlvo < 1 || mesAlvo > 12) {
            throw new PedidoInvalidoException("O mês tem de estar entre 1 e 12.");
        }
        List<Orcamento> anteriores = orcamentoRepository.findAnterioresA(anoAlvo, mesAlvo,
                PageRequest.of(0, MESES_CONSIDERADOS));
        if (anteriores.isEmpty()) {
            throw new RecursoNaoEncontradoException("Não existem orçamentos anteriores para sugerir um valor.");
        }
        BigDecimal soma = anteriores.stream()
                .map(Orcamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal media = soma.divide(BigDecimal.valueOf(anteriores.size()), 2, RoundingMode.HALF_UP);
        return new SugestaoOrcamentoResposta(anoAlvo, mesAlvo, media, anteriores.size());
    }

    // Artigos adquiridos nas compras fechadas criadas nessa semana do mes (dia no fuso de Lisboa)
    @Transactional(readOnly = true)
    public List<SugestaoArtigoResposta> listaCompras(Integer semana) {
        if (semana == null) {
            throw new PedidoInvalidoException("A semana é obrigatória.");
        }
        if (semana < 1 || semana > 4) {
            throw new PedidoInvalidoException("A semana tem de estar entre 1 e 4.");
        }
        Map<Integer, Acumulado> porArtigo = new LinkedHashMap<>();
        for (ItemAdquirido item : itemCompraRepository.findItensAdquiridosDeComprasFechadas()) {
            if (semanaDoMes(gastoMensal.dataEmLisboa(item.dataCriacaoCompra()).getDayOfMonth()) == semana) {
                porArtigo.computeIfAbsent(item.idArtigo(), id -> new Acumulado(item)).juntar(item);
            }
        }
        List<SugestaoArtigoResposta> sugestoes = new ArrayList<>();
        for (Acumulado acumulado : porArtigo.values()) {
            sugestoes.add(acumulado.resposta());
        }
        sugestoes.sort(Comparator.comparingLong(SugestaoArtigoResposta::vezes).reversed()
                .thenComparing(SugestaoArtigoResposta::nomeArtigo, ORDEM_NOMES)
                .thenComparing(SugestaoArtigoResposta::idArtigo));
        return sugestoes;
    }

    // Semana 1 = dias 1 a 7, 2 = 8 a 14, 3 = 15 a 21, 4 = dia 22 ate ao fim do mes
    static int semanaDoMes(int dia) {
        return Math.min((dia - 1) / 7 + 1, 4);
    }

    private static final class Acumulado {

        private final ItemAdquirido primeiro;
        private final Set<Integer> compras = new HashSet<>();
        private long somaQuantidades;
        private int itens;

        Acumulado(ItemAdquirido primeiro) {
            this.primeiro = primeiro;
        }

        void juntar(ItemAdquirido item) {
            compras.add(item.idCompra());
            somaQuantidades += item.quantidadeAdquirida();
            itens++;
        }

        SugestaoArtigoResposta resposta() {
            int media = BigDecimal.valueOf(somaQuantidades)
                    .divide(BigDecimal.valueOf(itens), 0, RoundingMode.HALF_UP).intValue();
            return new SugestaoArtigoResposta(primeiro.idArtigo(), primeiro.nomeArtigo(),
                    new TipoArtigoResposta(primeiro.idTipoArtigo(), primeiro.nomeTipoArtigo()),
                    compras.size(), Math.max(media, 1));
        }
    }
}
