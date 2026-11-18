package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import pt.istec.ishopping.dto.OrcamentoAtualizarPedido;
import pt.istec.ishopping.dto.OrcamentoCriarPedido;
import pt.istec.ishopping.dto.OrcamentoDisponivelResposta;
import pt.istec.ishopping.dto.OrcamentoResposta;
import pt.istec.ishopping.dto.UtilizadorResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.PedidoInvalidoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.repository.OrcamentoRepository;
import pt.istec.ishopping.security.UtilizadorAutenticado;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    private static final String PERIODO_EM_USO = "Já existe um orçamento para este mês e ano.";

    // Em Lisboa ja e 1 de abril de 2026 (UTC+1 desde o fim de marco)
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-03-31T23:30:00Z"), ZoneOffset.UTC);

    @Mock
    private OrcamentoRepository orcamentoRepository;
    @Mock
    private ItemCompraRepository itemCompraRepository;
    @Mock
    private UtilizadorAutenticado utilizadorAutenticado;
    @InjectMocks
    private OrcamentoService orcamentoService;

    private Utilizador criarUtilizador(Integer id, String username) {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", id);
        utilizador.setNome(username);
        utilizador.setUsername(username);
        return utilizador;
    }

    private Orcamento criarOrcamento(Integer id, int ano, int mes, String valor) {
        Orcamento orcamento = new Orcamento();
        ReflectionTestUtils.setField(orcamento, "id", id);
        orcamento.setAno(ano);
        orcamento.setMes(mes);
        orcamento.setValor(new BigDecimal(valor));
        orcamento.setCriadoPor(criarUtilizador(1, "ana"));
        return orcamento;
    }

    @Test
    void listarSemFiltroDevolveTodos() {
        when(orcamentoRepository.findAllComUtilizadores())
                .thenReturn(List.of(criarOrcamento(2, 2026, 5, "300.00"), criarOrcamento(1, 2026, 4, "250.00")));

        assertThat(orcamentoService.listar(null))
                .extracting(OrcamentoResposta::id)
                .containsExactly(2, 1);
    }

    @Test
    void listarComFiltroUsaOAnoIndicado() {
        when(orcamentoRepository.findAllComUtilizadoresPorAno(2025)).thenReturn(List.of());

        assertThat(orcamentoService.listar(2025)).isEmpty();
        verify(orcamentoRepository, never()).findAllComUtilizadores();
    }

    @Test
    void criarGuardaOsDadosEOCriador() {
        when(utilizadorAutenticado.obter()).thenReturn(criarUtilizador(1, "ana"));
        when(orcamentoRepository.findByAnoAndMes(2026, 5)).thenReturn(Optional.empty());
        when(orcamentoRepository.saveAndFlush(any(Orcamento.class))).thenAnswer(pedido -> {
            Orcamento guardado = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", 7);
            return guardado;
        });

        OrcamentoResposta resposta = orcamentoService.criar(new OrcamentoCriarPedido(2026, 5, new BigDecimal("300.5")));

        assertThat(resposta.id()).isEqualTo(7);
        assertThat(resposta.ano()).isEqualTo(2026);
        assertThat(resposta.mes()).isEqualTo(5);
        assertThat(resposta.valor()).isEqualTo(new BigDecimal("300.50"));
        assertThat(resposta.criadoPor()).isEqualTo(new UtilizadorResposta(1, "ana", "ana"));
        assertThat(resposta.alteradoPor()).isNull();
        assertThat(resposta.dataAlterado()).isNull();
    }

    @Test
    void criarComPeriodoDuplicadoDaConflito() {
        when(orcamentoRepository.findByAnoAndMes(2026, 5)).thenReturn(Optional.of(criarOrcamento(1, 2026, 5, "1")));

        assertThatThrownBy(() -> orcamentoService.criar(new OrcamentoCriarPedido(2026, 5, BigDecimal.TEN)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(PERIODO_EM_USO);
        verify(orcamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarQueViolaARestricaoUnicaDaConflito() {
        when(utilizadorAutenticado.obter()).thenReturn(criarUtilizador(1, "ana"));
        when(orcamentoRepository.findByAnoAndMes(2026, 5)).thenReturn(Optional.empty());
        when(orcamentoRepository.saveAndFlush(any(Orcamento.class)))
                .thenThrow(new DataIntegrityViolationException("uq_orcamento_ano_mes"));

        assertThatThrownBy(() -> orcamentoService.criar(new OrcamentoCriarPedido(2026, 5, BigDecimal.TEN)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(PERIODO_EM_USO);
    }

    @Test
    void atualizarAlteraOsDadosERegistaQuemAlterou() {
        Orcamento orcamento = criarOrcamento(1, 2026, 5, "300.00");
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(orcamento));
        when(orcamentoRepository.findByAnoAndMes(2026, 6)).thenReturn(Optional.empty());
        when(utilizadorAutenticado.obter()).thenReturn(criarUtilizador(2, "rui"));
        when(orcamentoRepository.saveAndFlush(orcamento)).thenReturn(orcamento);

        OrcamentoResposta resposta = orcamentoService.atualizar(1,
                new OrcamentoAtualizarPedido(2026, 6, new BigDecimal("450")));

        assertThat(resposta.mes()).isEqualTo(6);
        assertThat(resposta.valor()).isEqualTo(new BigDecimal("450.00"));
        assertThat(resposta.criadoPor().username()).isEqualTo("ana");
        assertThat(resposta.alteradoPor().username()).isEqualTo("rui");
        assertThat(resposta.dataAlterado()).isNotNull();
    }

    @Test
    void atualizarIgnoraOProprioNaVerificacaoDoPeriodo() {
        Orcamento orcamento = criarOrcamento(1, 2026, 5, "300.00");
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(orcamento));
        when(orcamentoRepository.findByAnoAndMes(2026, 5)).thenReturn(Optional.of(orcamento));
        when(utilizadorAutenticado.obter()).thenReturn(criarUtilizador(1, "ana"));
        when(orcamentoRepository.saveAndFlush(orcamento)).thenReturn(orcamento);

        assertThat(orcamentoService.atualizar(1, new OrcamentoAtualizarPedido(2026, 5, BigDecimal.ONE)).valor())
                .isEqualTo(new BigDecimal("1.00"));
    }

    @Test
    void atualizarParaPeriodoDeOutroDaConflito() {
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(criarOrcamento(1, 2026, 5, "300.00")));
        when(orcamentoRepository.findByAnoAndMes(2026, 6)).thenReturn(Optional.of(criarOrcamento(2, 2026, 6, "1")));

        assertThatThrownBy(() -> orcamentoService.atualizar(1, new OrcamentoAtualizarPedido(2026, 6, BigDecimal.TEN)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(PERIODO_EM_USO);
        verify(orcamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarInexistenteDaNaoEncontrado() {
        when(orcamentoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orcamentoService.atualizar(9, new OrcamentoAtualizarPedido(2026, 5, BigDecimal.TEN)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O orçamento não existe.");
    }

    @Test
    void obterInexistenteDaNaoEncontrado() {
        when(orcamentoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orcamentoService.obter(9))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O orçamento não existe.");
    }

    @Test
    void apagarFazFlushDentroDoServico() {
        Orcamento orcamento = criarOrcamento(1, 2026, 5, "300.00");
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(orcamento));

        orcamentoService.apagar(1);

        verify(orcamentoRepository).delete(orcamento);
        verify(orcamentoRepository).flush();
    }

    @Test
    void apagarInexistenteDaNaoEncontrado() {
        when(orcamentoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orcamentoService.apagar(9))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O orçamento não existe.");
    }

    @Test
    void apagarComRegistosAssociadosDaConflito() {
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(criarOrcamento(1, 2026, 5, "300.00")));
        doThrow(new DataIntegrityViolationException("fk")).when(orcamentoRepository).flush();

        assertThatThrownBy(() -> orcamentoService.apagar(1))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível apagar um orçamento com registos associados.");
    }

    private OrcamentoService servicoComRelogio() {
        return new OrcamentoService(orcamentoRepository, itemCompraRepository, utilizadorAutenticado, RELOGIO);
    }

    @Test
    void disponivelSemParametrosUsaOMesAtualDeLisboaESemComprasNadaFoiGasto() {
        when(orcamentoRepository.findByAnoAndMes(2026, 4)).thenReturn(Optional.of(criarOrcamento(1, 2026, 4, "400")));
        when(itemCompraRepository.somarGastoEntre(Instant.parse("2026-03-31T23:00:00Z"),
                Instant.parse("2026-04-30T23:00:00Z"))).thenReturn(null);

        OrcamentoDisponivelResposta resposta = servicoComRelogio().disponivel(null, null);

        assertThat(resposta).isEqualTo(new OrcamentoDisponivelResposta(2026, 4, new BigDecimal("400.00"),
                new BigDecimal("0.00"), new BigDecimal("400.00"), new BigDecimal("100"), false));
    }

    @Test
    void disponivelDescontaOGastoDoMes() {
        when(orcamentoRepository.findByAnoAndMes(2026, 4)).thenReturn(Optional.of(criarOrcamento(1, 2026, 4, "400.00")));
        when(itemCompraRepository.somarGastoEntre(any(), any())).thenReturn(new BigDecimal("100.50"));

        OrcamentoDisponivelResposta resposta = servicoComRelogio().disponivel(2026, 4);

        assertThat(resposta.gasto()).isEqualTo(new BigDecimal("100.50"));
        assertThat(resposta.disponivel()).isEqualTo(new BigDecimal("299.50"));
        assertThat(resposta.percentagemRestante()).isEqualTo(new BigDecimal("75"));
        assertThat(resposta.ultrapassado()).isFalse();
    }

    @Test
    void disponivelUltrapassadoFicaNegativo() {
        when(orcamentoRepository.findByAnoAndMes(2026, 4)).thenReturn(Optional.of(criarOrcamento(1, 2026, 4, "100.00")));
        when(itemCompraRepository.somarGastoEntre(any(), any())).thenReturn(new BigDecimal("112.50"));

        OrcamentoDisponivelResposta resposta = servicoComRelogio().disponivel(2026, 4);

        assertThat(resposta.disponivel()).isEqualTo(new BigDecimal("-12.50"));
        assertThat(resposta.percentagemRestante()).isEqualTo(new BigDecimal("-13"));
        assertThat(resposta.ultrapassado()).isTrue();
    }

    @Test
    void disponivelLimitaAsComprasAoMesPedidoNoFusoDeLisboa() {
        when(orcamentoRepository.findByAnoAndMes(2026, 10)).thenReturn(Optional.of(criarOrcamento(1, 2026, 10, "50")));

        servicoComRelogio().disponivel(2026, 10);

        // Outubro comeca em hora de verao (UTC+1) e acaba em hora de inverno (UTC+0)
        verify(itemCompraRepository).somarGastoEntre(Instant.parse("2026-09-30T23:00:00Z"),
                Instant.parse("2026-11-01T00:00:00Z"));
    }

    @Test
    void disponivelComOrcamentoZeroNaoTemPercentagem() {
        when(orcamentoRepository.findByAnoAndMes(2026, 4)).thenReturn(Optional.of(criarOrcamento(1, 2026, 4, "0")));
        when(itemCompraRepository.somarGastoEntre(any(), any())).thenReturn(new BigDecimal("5.00"));

        OrcamentoDisponivelResposta resposta = servicoComRelogio().disponivel(2026, 4);

        assertThat(resposta.percentagemRestante()).isNull();
        assertThat(resposta.disponivel()).isEqualTo(new BigDecimal("-5.00"));
        assertThat(resposta.ultrapassado()).isTrue();
    }

    @Test
    void disponivelSemOrcamentoDaNaoEncontrado() {
        when(orcamentoRepository.findByAnoAndMes(2026, 4)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoComRelogio().disponivel(2026, 4))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Não existe orçamento para este mês.");
        verify(itemCompraRepository, never()).somarGastoEntre(any(), any());
    }

    @Test
    void disponivelComParametrosForaDoIntervaloDaPedidoInvalido() {
        assertThatThrownBy(() -> servicoComRelogio().disponivel(2026, 13))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("O mês tem de estar entre 1 e 12.");
        assertThatThrownBy(() -> servicoComRelogio().disponivel(1999, 4))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("O ano tem de estar entre 2000 e 2100.");
    }
}
