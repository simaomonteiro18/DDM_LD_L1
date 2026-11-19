package pt.istec.ishopping.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.dto.ArtigoAtualizarPedido;
import pt.istec.ishopping.dto.ArtigoCriarPedido;
import pt.istec.ishopping.dto.ArtigoResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Artigo;
import pt.istec.ishopping.model.TipoArtigo;
import pt.istec.ishopping.repository.ArtigoRepository;
import pt.istec.ishopping.repository.TipoArtigoRepository;

@Service
public class ArtigoService {

    private static final String NOME_EM_USO = "Já existe um artigo com este nome neste tipo de artigo.";

    private final ArtigoRepository artigoRepository;
    private final TipoArtigoRepository tipoArtigoRepository;

    public ArtigoService(ArtigoRepository artigoRepository, TipoArtigoRepository tipoArtigoRepository) {
        this.artigoRepository = artigoRepository;
        this.tipoArtigoRepository = tipoArtigoRepository;
    }

    @Transactional(readOnly = true)
    public List<ArtigoResposta> listar(Integer idTipoArtigo) {
        List<Artigo> artigos = idTipoArtigo == null
                ? artigoRepository.findAllComTipo()
                : artigoRepository.findAllComTipoPorIdTipoArtigo(idTipoArtigo);
        return artigos.stream()
                .map(ArtigoResposta::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArtigoResposta obter(Integer id) {
        return ArtigoResposta.de(procurar(id));
    }

    @Transactional
    public ArtigoResposta criar(ArtigoCriarPedido pedido) {
        TipoArtigo tipoArtigo = procurarTipo(pedido.idTipoArtigo());
        artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(pedido.idTipoArtigo(), pedido.nome())
                .ifPresent(existente -> {
                    throw new ConflitoException(NOME_EM_USO);
                });
        Artigo artigo = new Artigo();
        artigo.setNome(pedido.nome());
        artigo.setTipoArtigo(tipoArtigo);
        return ArtigoResposta.de(guardar(artigo));
    }

    @Transactional
    public ArtigoResposta atualizar(Integer id, ArtigoAtualizarPedido pedido) {
        Artigo artigo = procurar(id);
        TipoArtigo tipoArtigo = procurarTipo(pedido.idTipoArtigo());
        artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(pedido.idTipoArtigo(), pedido.nome())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new ConflitoException(NOME_EM_USO);
                });
        artigo.setNome(pedido.nome());
        artigo.setTipoArtigo(tipoArtigo);
        return ArtigoResposta.de(guardar(artigo));
    }

    @Transactional
    public void apagar(Integer id) {
        Artigo artigo = procurar(id);
        try {
            artigoRepository.delete(artigo);
            artigoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("Não é possível apagar um artigo com itens de compra associados.");
        }
    }

    private Artigo procurar(Integer id) {
        return artigoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("O artigo não existe."));
    }

    private TipoArtigo procurarTipo(Integer idTipoArtigo) {
        return tipoArtigoRepository.findById(idTipoArtigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("O tipo de artigo não existe."));
    }

    private Artigo guardar(Artigo artigo) {
        try {
            return artigoRepository.saveAndFlush(artigo);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException(NOME_EM_USO);
        }
    }
}
