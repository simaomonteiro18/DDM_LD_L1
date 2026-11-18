package pt.istec.ishopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.dto.OrcamentoAtualizarPedido;
import pt.istec.ishopping.dto.OrcamentoCriarPedido;
import pt.istec.ishopping.dto.OrcamentoDisponivelResposta;
import pt.istec.ishopping.dto.OrcamentoResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.PedidoInvalidoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.repository.OrcamentoRepository;
import pt.istec.ishopping.security.UtilizadorAutenticado;

@Service
public class OrcamentoService {

    private static final String PERIODO_EM_USO = "Já existe um orçamento para este mês e ano.";
    private static final ZoneId ZONA = ZoneId.of("Europe/Lisbon");
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final OrcamentoRepository orcamentoRepository;
    private final ItemCompraRepository itemCompraRepository;
    private final UtilizadorAutenticado utilizadorAutenticado;
    private final Clock relogio;

    public OrcamentoService(OrcamentoRepository orcamentoRepository, ItemCompraRepository itemCompraRepository,
            UtilizadorAutenticado utilizadorAutenticado, Clock relogio) {
        this.orcamentoRepository = orcamentoRepository;
        this.itemCompraRepository = itemCompraRepository;
        this.utilizadorAutenticado = utilizadorAutenticado;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResposta> listar(Integer ano) {
        List<Orcamento> orcamentos = ano == null
                ? orcamentoRepository.findAllComUtilizadores()
                : orcamentoRepository.findAllComUtilizadoresPorAno(ano);
        return orcamentos.stream()
                .map(OrcamentoResposta::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrcamentoResposta obter(Integer id) {
        return OrcamentoResposta.de(procurar(id));
    }

    @Transactional(readOnly = true)
    public OrcamentoDisponivelResposta disponivel(Integer ano, Integer mes) {
        LocalDate hoje = LocalDate.ofInstant(relogio.instant(), ZONA);
        int anoPedido = ano == null ? hoje.getYear() : ano;
        int mesPedido = mes == null ? hoje.getMonthValue() : mes;
        if (anoPedido < 2000 || anoPedido > 2100) {
            throw new PedidoInvalidoException("O ano tem de estar entre 2000 e 2100.");
        }
        if (mesPedido < 1 || mesPedido > 12) {
            throw new PedidoInvalidoException("O mês tem de estar entre 1 e 12.");
        }
        Orcamento orcamento = orcamentoRepository.findByAnoAndMes(anoPedido, mesPedido)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Não existe orçamento para este mês."));

        // O mes conta pela data de criacao da compra, no fuso de Lisboa
        YearMonth periodo = YearMonth.of(anoPedido, mesPedido);
        Instant inicio = periodo.atDay(1).atStartOfDay(ZONA).toInstant();
        Instant fim = periodo.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant();
        BigDecimal somado = itemCompraRepository.somarGastoEntre(inicio, fim);
        BigDecimal gasto = (somado == null ? BigDecimal.ZERO : somado).setScale(2, RoundingMode.HALF_UP);

        BigDecimal valor = orcamento.getValor().setScale(2, RoundingMode.HALF_UP);
        BigDecimal disponivel = valor.subtract(gasto);
        BigDecimal percentagem = valor.signum() == 0
                ? null
                : disponivel.multiply(CEM).divide(valor, 0, RoundingMode.HALF_UP);
        return new OrcamentoDisponivelResposta(anoPedido, mesPedido, valor, gasto, disponivel, percentagem,
                disponivel.signum() < 0);
    }

    @Transactional
    public OrcamentoResposta criar(OrcamentoCriarPedido pedido) {
        verificarPeriodoLivre(null, pedido.ano(), pedido.mes());
        Orcamento orcamento = new Orcamento();
        orcamento.setAno(pedido.ano());
        orcamento.setMes(pedido.mes());
        orcamento.setValor(normalizar(pedido.valor()));
        orcamento.setCriadoPor(utilizadorAutenticado.obter());
        return OrcamentoResposta.de(guardar(orcamento));
    }

    @Transactional
    public OrcamentoResposta atualizar(Integer id, OrcamentoAtualizarPedido pedido) {
        Orcamento orcamento = procurar(id);
        verificarPeriodoLivre(id, pedido.ano(), pedido.mes());
        orcamento.setAno(pedido.ano());
        orcamento.setMes(pedido.mes());
        orcamento.setValor(normalizar(pedido.valor()));
        orcamento.registarAlteracao(utilizadorAutenticado.obter());
        return OrcamentoResposta.de(guardar(orcamento));
    }

    @Transactional
    public void apagar(Integer id) {
        Orcamento orcamento = procurar(id);
        try {
            orcamentoRepository.delete(orcamento);
            orcamentoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("Não é possível apagar um orçamento com registos associados.");
        }
    }

    private Orcamento procurar(Integer id) {
        return orcamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("O orçamento não existe."));
    }

    private void verificarPeriodoLivre(Integer id, Integer ano, Integer mes) {
        orcamentoRepository.findByAnoAndMes(ano, mes)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new ConflitoException(PERIODO_EM_USO);
                });
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private Orcamento guardar(Orcamento orcamento) {
        try {
            return orcamentoRepository.saveAndFlush(orcamento);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException(PERIODO_EM_USO);
        }
    }
}
