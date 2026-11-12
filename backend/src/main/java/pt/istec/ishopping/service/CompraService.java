package pt.istec.ishopping.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.dto.CompraAtualizarPedido;
import pt.istec.ishopping.dto.CompraCriarPedido;
import pt.istec.ishopping.dto.CompraResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.repository.CompraRepository;
import pt.istec.ishopping.security.UtilizadorAutenticado;

@Service
public class CompraService {

    private static final String NAO_EXISTE = "A compra não existe.";

    private final CompraRepository compraRepository;
    private final UtilizadorAutenticado utilizadorAutenticado;

    public CompraService(CompraRepository compraRepository, UtilizadorAutenticado utilizadorAutenticado) {
        this.compraRepository = compraRepository;
        this.utilizadorAutenticado = utilizadorAutenticado;
    }

    @Transactional(readOnly = true)
    public List<CompraResposta> listar(Boolean fechada) {
        List<Compra> compras = fechada == null
                ? compraRepository.findAllByOrderByDataCriacaoDescIdDesc()
                : compraRepository.findByFechadaOrderByDataCriacaoDescIdDesc(fechada);
        return compras.stream()
                .map(CompraResposta::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompraResposta obter(Integer id) {
        return CompraResposta.de(compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_EXISTE)));
    }

    @Transactional
    public CompraResposta criar(CompraCriarPedido pedido) {
        Compra compra = new Compra();
        compra.setNome(pedido.nome());
        compra.setCriadoPor(utilizadorAutenticado.obter());
        return CompraResposta.de(compraRepository.saveAndFlush(compra));
    }

    @Transactional
    public CompraResposta atualizar(Integer id, CompraAtualizarPedido pedido) {
        Compra compra = procurarAberta(id, "Não é possível alterar uma compra fechada.");
        compra.setNome(pedido.nome());
        compra.registarAlteracao(utilizadorAutenticado.obter());
        return CompraResposta.de(compraRepository.saveAndFlush(compra));
    }

    @Transactional
    public CompraResposta fechar(Integer id) {
        Compra compra = procurarAberta(id, "A compra já está fechada.");
        compra.fechar(utilizadorAutenticado.obter());
        return CompraResposta.de(compraRepository.saveAndFlush(compra));
    }

    @Transactional
    public void apagar(Integer id) {
        Compra compra = procurarAberta(id, "Não é possível apagar uma compra fechada.");
        compraRepository.delete(compra);
        compraRepository.flush();
    }

    private Compra procurarAberta(Integer id, String mensagemSeFechada) {
        Compra compra = compraRepository.findByIdParaAlterar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_EXISTE));
        if (compra.isFechada()) {
            throw new ConflitoException(mensagemSeFechada);
        }
        return compra;
    }
}
