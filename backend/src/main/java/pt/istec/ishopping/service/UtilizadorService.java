package pt.istec.ishopping.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.dto.UtilizadorAtualizarPedido;
import pt.istec.ishopping.dto.UtilizadorCriarPedido;
import pt.istec.ishopping.dto.UtilizadorResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.UtilizadorAutenticado;

@Service
public class UtilizadorService {

    private static final String USERNAME_EM_USO = "Já existe um utilizador com este username.";

    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final UtilizadorAutenticado utilizadorAutenticado;

    public UtilizadorService(UtilizadorRepository utilizadorRepository, PasswordEncoder passwordEncoder,
            UtilizadorAutenticado utilizadorAutenticado) {
        this.utilizadorRepository = utilizadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.utilizadorAutenticado = utilizadorAutenticado;
    }

    @Transactional(readOnly = true)
    public List<UtilizadorResposta> listar() {
        return utilizadorRepository.findAll(Sort.by("nome", "id")).stream()
                .map(UtilizadorResposta::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public UtilizadorResposta obter(Integer id) {
        return UtilizadorResposta.de(procurar(id));
    }

    @Transactional
    public UtilizadorResposta criar(UtilizadorCriarPedido pedido) {
        utilizadorRepository.findByUsernameIgnoreCase(pedido.username()).ifPresent(existente -> {
            throw new ConflitoException(USERNAME_EM_USO);
        });
        Utilizador utilizador = new Utilizador();
        utilizador.setNome(pedido.nome());
        utilizador.setUsername(pedido.username());
        utilizador.setPasswordHash(passwordEncoder.encode(pedido.password()));
        return UtilizadorResposta.de(guardar(utilizador));
    }

    @Transactional
    public UtilizadorResposta atualizar(Integer id, UtilizadorAtualizarPedido pedido) {
        Utilizador utilizador = procurar(id);
        utilizadorRepository.findByUsernameIgnoreCase(pedido.username())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new ConflitoException(USERNAME_EM_USO);
                });
        utilizador.setNome(pedido.nome());
        utilizador.setUsername(pedido.username());
        if (pedido.password() != null) {
            utilizador.setPasswordHash(passwordEncoder.encode(pedido.password()));
        }
        return UtilizadorResposta.de(guardar(utilizador));
    }

    @Transactional
    public void apagar(Integer id) {
        Utilizador utilizador = procurar(id);
        if (id.equals(utilizadorAutenticado.obter().getId())) {
            throw new ConflitoException("Não é possível apagar o próprio utilizador autenticado.");
        }
        try {
            utilizadorRepository.delete(utilizador);
            utilizadorRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("Não é possível apagar um utilizador com registos associados.");
        }
    }

    private Utilizador procurar(Integer id) {
        return utilizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("O utilizador não existe."));
    }

    private Utilizador guardar(Utilizador utilizador) {
        try {
            return utilizadorRepository.saveAndFlush(utilizador);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException(USERNAME_EM_USO);
        }
    }
}
