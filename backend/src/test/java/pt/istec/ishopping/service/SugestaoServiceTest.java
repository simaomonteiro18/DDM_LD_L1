package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import pt.istec.ishopping.dto.SugestaoArtigoResposta;
import pt.istec.ishopping.dto.SugestaoOrcamentoResposta;
import pt.istec.ishopping.dto.TipoArtigoResposta;
import pt.istec.ishopping.exception.PedidoInvalidoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.repository.ItemAdquirido;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.repository.OrcamentoRepository;

@ExtendWith(MockitoExtension.class)
class SugestaoServiceTest {

    private static final PageRequest SEIS_MAIS_RECENTES = PageRequest.of(0, 6);

    @Mock
    private OrcamentoRepository orcamentoRepository;
    @Mock
    private ItemCompraRepository itemCompraRepository;

    private SugestaoService servico(String agora) {
        Clock relogio = Clock.fixed(Instant.parse(agora), ZoneOffset.UTC);
        return new SugestaoService(orcamentoRepository, itemCompraRepository,
                new GastoMensalService(itemCompraRepository, relogio));
    }

    private SugestaoService servico() {
        return servico("2026-11-18T10:00:00Z");
    }

    private List<Orcamento> orcamentos(String... valores) {
        return Arrays.stream(valores).map(valor -> {
            Orcamento orcamento = new Orcamento();
            orcamento.setValor(new BigDecimal(valor));
            return orcamento;
        }).toList();
    }

    private ItemAdquirido item(int idCompra, String criadaEm, int idArtigo, String nome, int quantidade) {
        return new ItemAdquirido(idCompra, Instant.parse(criadaEm), idArtigo, nome, 1, "Mercearia", quantidade);
    }

    private List<Integer> artigosDaSemana(int semana, List<ItemAdquirido> itens) {
        when(itemCompraRepository.findItensAdquiridosDeComprasFechadas()).thenReturn(itens);
        return servico().listaCompras(semana).stream().map(SugestaoArtigoResposta::idArtigo).toList();
    }

    @Test
    void orcamentoComUmOrcamentoAnteriorUsaEsseValor() {
        when(orcamentoRepository.findAnterioresA(2026, 12, SEIS_MAIS_RECENTES)).thenReturn(orcamentos("300"));

        assertThat(servico().orcamento(null, null))
                .isEqualTo(new SugestaoOrcamentoResposta(2026, 12, new BigDecimal("300.00"), 1));
    }

    @Test
    void orcamentoComTresOrcamentosFazAMediaArredondada() {
        when(orcamentoRepository.findAnterioresA(2026, 12, SEIS_MAIS_RECENTES))
                .thenReturn(orcamentos("10.00", "20.00", "25.00"));

        SugestaoOrcamentoResposta sugestao = servico().orcamento(null, null);

        assertThat(sugestao.valorSugerido()).isEqualTo(new BigDecimal("18.33"));
        assertThat(sugestao.baseadoEm()).isEqualTo(3);
    }

    @Test
    void orcamentoPedeSoOsSeisMaisRecentesEArredondaMeioParaCima() {
        when(orcamentoRepository.findAnterioresA(2026, 12, SEIS_MAIS_RECENTES))
                .thenReturn(orcamentos("0.01", "0.02", "0.02", "0.02", "0.02", "0.02"));

        SugestaoOrcamentoResposta sugestao = servico().orcamento(null, null);

        // 0,11 / 6 = 0,01833... arredonda para 0,02
        assertThat(sugestao.valorSugerido()).isEqualTo(new BigDecimal("0.02"));
        assertThat(sugestao.baseadoEm()).isEqualTo(6);
        verify(orcamentoRepository).findAnterioresA(2026, 12, SEIS_MAIS_RECENTES);
    }

    @Test
    void orcamentoSemOrcamentosAnterioresDaNaoEncontrado() {
        when(orcamentoRepository.findAnterioresA(2026, 12, SEIS_MAIS_RECENTES)).thenReturn(List.of());

        assertThatThrownBy(() -> servico().orcamento(null, null))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Não existem orçamentos anteriores para sugerir um valor.");
    }

    @Test
    void orcamentoEmDezembroSugereJaneiroDoAnoSeguinte() {
        when(orcamentoRepository.findAnterioresA(2027, 1, SEIS_MAIS_RECENTES)).thenReturn(orcamentos("100"));

        assertThat(servico("2026-12-20T10:00:00Z").orcamento(null, null).ano()).isEqualTo(2027);
    }

    @Test
    void orcamentoUsaOMesAtualDeLisboa() {
        // 23:30 UTC de 31 de marco ja e 1 de abril em Lisboa, por isso o mes alvo e maio
        when(orcamentoRepository.findAnterioresA(2026, 5, SEIS_MAIS_RECENTES)).thenReturn(orcamentos("100"));

        assertThat(servico("2026-03-31T23:30:00Z").orcamento(null, null).mes()).isEqualTo(5);
    }

    @Test
    void orcamentoComSoUmParametroCompletaComOMesAlvoPorOmissao() {
        when(orcamentoRepository.findAnterioresA(2030, 12, SEIS_MAIS_RECENTES)).thenReturn(orcamentos("1"));
        when(orcamentoRepository.findAnterioresA(2026, 3, SEIS_MAIS_RECENTES)).thenReturn(orcamentos("1"));

        assertThat(servico().orcamento(2030, null).mes()).isEqualTo(12);
        assertThat(servico().orcamento(null, 3).ano()).isEqualTo(2026);
    }

    @Test
    void orcamentoComParametrosInvalidosDaPedidoInvalido() {
        assertThatThrownBy(() -> servico().orcamento(2026, 13))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("O mês tem de estar entre 1 e 12.");
        assertThatThrownBy(() -> servico().orcamento(2026, 0))
                .isInstanceOf(PedidoInvalidoException.class);
        assertThatThrownBy(() -> servico().orcamento(1999, 5))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("O ano tem de estar entre 2000 e 2100.");
        verify(orcamentoRepository, never()).findAnterioresA(anyInt(), anyInt(), any());
    }

    @Test
    void semanaDoMesSegueOsLimitesDosDias() {
        assertThat(List.of(1, 7, 8, 14, 15, 21, 22, 28, 29, 31).stream().map(SugestaoService::semanaDoMes))
                .containsExactly(1, 1, 2, 2, 3, 3, 4, 4, 4, 4);
    }

    @Test
    void listaComprasSeparaAsComprasPelasQuatroSemanas() {
        List<ItemAdquirido> itens = List.of(
                item(1, "2026-01-07T12:00:00Z", 7, "A", 1),
                item(2, "2026-01-08T12:00:00Z", 8, "B", 1),
                item(3, "2026-01-14T12:00:00Z", 14, "C", 1),
                item(4, "2026-01-15T12:00:00Z", 15, "D", 1),
                item(5, "2026-01-21T12:00:00Z", 21, "E", 1),
                item(6, "2026-01-22T12:00:00Z", 22, "F", 1),
                item(7, "2026-01-31T12:00:00Z", 31, "G", 1));

        assertThat(artigosDaSemana(1, itens)).containsExactly(7);
        assertThat(artigosDaSemana(2, itens)).containsExactly(8, 14);
        assertThat(artigosDaSemana(3, itens)).containsExactly(15, 21);
        assertThat(artigosDaSemana(4, itens)).containsExactly(22, 31);
    }

    @Test
    void listaComprasContaODiaNoFusoDeLisboaPertoDaMeiaNoite() {
        List<ItemAdquirido> itens = List.of(
                // Verao (UTC+1): 23:30 UTC de 7 de julho ja e dia 8 em Lisboa
                item(1, "2026-07-07T23:30:00Z", 1, "Verao", 1),
                // Inverno (UTC+0): 23:30 UTC de 7 de janeiro ainda e dia 7 em Lisboa
                item(2, "2026-01-07T23:30:00Z", 2, "Inverno", 1));

        assertThat(artigosDaSemana(1, itens)).containsExactly(2);
        assertThat(artigosDaSemana(2, itens)).containsExactly(1);
    }

    @Test
    void listaComprasContaAsComprasDistintasEFazAMediaDasQuantidades() {
        when(itemCompraRepository.findItensAdquiridosDeComprasFechadas()).thenReturn(List.of(
                item(10, "2026-01-02T10:00:00Z", 1, "Arroz", 1),
                item(11, "2026-02-03T10:00:00Z", 1, "Arroz", 2),
                item(11, "2026-02-03T10:00:00Z", 1, "Arroz", 2),
                item(12, "2026-03-04T10:00:00Z", 2, "Leite", 3)));

        List<SugestaoArtigoResposta> sugestoes = servico().listaCompras(1);

        // Arroz: 2 compras distintas, media (1 + 2 + 2) / 3 = 1,67 arredonda para 2
        assertThat(sugestoes).containsExactly(
                new SugestaoArtigoResposta(1, "Arroz", new TipoArtigoResposta(1, "Mercearia"), 2, 2),
                new SugestaoArtigoResposta(2, "Leite", new TipoArtigoResposta(1, "Mercearia"), 1, 3));
    }

    @Test
    void listaComprasOrdenaPorVezesDepoisPorNomeEDepoisPorId() {
        List<ItemAdquirido> itens = List.of(
                item(1, "2026-01-02T10:00:00Z", 5, "Banana", 1),
                item(1, "2026-01-02T10:00:00Z", 9, "Massa", 1),
                item(2, "2026-02-02T10:00:00Z", 9, "Massa", 1),
                item(1, "2026-01-02T10:00:00Z", 4, "Açúcar", 1),
                item(1, "2026-01-02T10:00:00Z", 8, "Banana", 1),
                item(1, "2026-01-02T10:00:00Z", 3, "Banana", 1));

        // Massa aparece em 2 compras; os restantes empatam e seguem o nome e depois o id
        assertThat(artigosDaSemana(1, itens)).containsExactly(9, 4, 3, 5, 8);
    }

    @Test
    void listaComprasSemHistoricoNaSemanaDevolveListaVazia() {
        when(itemCompraRepository.findItensAdquiridosDeComprasFechadas())
                .thenReturn(List.of(item(1, "2026-01-02T10:00:00Z", 1, "Arroz", 1)));

        assertThat(servico().listaCompras(3)).isEmpty();
    }

    @Test
    void listaComprasComSemanaInvalidaOuEmFaltaDaPedidoInvalido() {
        assertThatThrownBy(() -> servico().listaCompras(null))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("A semana é obrigatória.");
        assertThatThrownBy(() -> servico().listaCompras(0))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("A semana tem de estar entre 1 e 4.");
        assertThatThrownBy(() -> servico().listaCompras(5))
                .isInstanceOf(PedidoInvalidoException.class);
        verify(itemCompraRepository, never()).findItensAdquiridosDeComprasFechadas();
    }
}
