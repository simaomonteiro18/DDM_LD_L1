package pt.istec.ishopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.dto.OrcamentoAtualizarPedido;
import pt.istec.ishopping.dto.OrcamentoCriarPedido;
import pt.istec.ishopping.dto.OrcamentoResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.repository.OrcamentoRepository;
import pt.istec.ishopping.security.UtilizadorAutenticado;

@Service
public class OrcamentoService {

    private static final String PERIODO_EM_USO = "Já existe um orçamento para este mês e ano.";

    private final OrcamentoRepository orcamentoRepository;
    private final UtilizadorAutenticado utilizadorAutenticado;

    public OrcamentoService(OrcamentoRepository orcamentoRepository, UtilizadorAutenticado utilizadorAutenticado) {
        this.orcamentoRepository = orcamentoRepository;
        this.utilizadorAutenticado = utilizadorAutenticado;
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
