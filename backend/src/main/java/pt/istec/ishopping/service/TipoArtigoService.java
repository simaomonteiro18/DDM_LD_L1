package pt.istec.ishopping.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.dto.TipoArtigoAtualizarPedido;
import pt.istec.ishopping.dto.TipoArtigoCriarPedido;
import pt.istec.ishopping.dto.TipoArtigoResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.TipoArtigo;
import pt.istec.ishopping.repository.TipoArtigoRepository;

@Service
public class TipoArtigoService {

    private static final String NOME_EM_USO = "Já existe um tipo de artigo com este nome.";

    private final TipoArtigoRepository tipoArtigoRepository;

    public TipoArtigoService(TipoArtigoRepository tipoArtigoRepository) {
        this.tipoArtigoRepository = tipoArtigoRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoArtigoResposta> listar() {
        return tipoArtigoRepository.findAll(Sort.by("nome", "id")).stream()
                .map(TipoArtigoResposta::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoArtigoResposta obter(Integer id) {
        return TipoArtigoResposta.de(procurar(id));
    }

    @Transactional
    public TipoArtigoResposta criar(TipoArtigoCriarPedido pedido) {
        tipoArtigoRepository.findByNomeIgnoreCase(pedido.nome()).ifPresent(existente -> {
            throw new ConflitoException(NOME_EM_USO);
        });
        TipoArtigo tipoArtigo = new TipoArtigo();
        tipoArtigo.setNome(pedido.nome());
        return TipoArtigoResposta.de(guardar(tipoArtigo));
    }

    @Transactional
    public TipoArtigoResposta atualizar(Integer id, TipoArtigoAtualizarPedido pedido) {
        TipoArtigo tipoArtigo = procurar(id);
        tipoArtigoRepository.findByNomeIgnoreCase(pedido.nome())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new ConflitoException(NOME_EM_USO);
                });
        tipoArtigo.setNome(pedido.nome());
        return TipoArtigoResposta.de(guardar(tipoArtigo));
    }

    @Transactional
    public void apagar(Integer id) {
        TipoArtigo tipoArtigo = procurar(id);
        try {
            tipoArtigoRepository.delete(tipoArtigo);
            tipoArtigoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("Não é possível apagar um tipo de artigo com artigos associados.");
        }
    }

    private TipoArtigo procurar(Integer id) {
        return tipoArtigoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("O tipo de artigo não existe."));
    }

    private TipoArtigo guardar(TipoArtigo tipoArtigo) {
        try {
            return tipoArtigoRepository.saveAndFlush(tipoArtigo);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException(NOME_EM_USO);
        }
    }
}
