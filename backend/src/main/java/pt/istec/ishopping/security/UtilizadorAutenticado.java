package pt.istec.ishopping.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import pt.istec.ishopping.exception.NaoAutenticadoException;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;

@Component
public class UtilizadorAutenticado {

    private static final String MENSAGEM = "Autenticação necessária.";

    private final UtilizadorRepository utilizadorRepository;

    public UtilizadorAutenticado(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    public Utilizador obter() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken token)) {
            throw new NaoAutenticadoException(MENSAGEM);
        }
        return utilizadorRepository.findById(lerId(token.getToken().getSubject()))
                .orElseThrow(() -> new NaoAutenticadoException(MENSAGEM));
    }

    private Integer lerId(String subject) {
        try {
            return Integer.valueOf(subject);
        } catch (NumberFormatException ex) {
            throw new NaoAutenticadoException(MENSAGEM);
        }
    }
}
