package pt.istec.ishopping.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import pt.istec.ishopping.repository.UtilizadorRepository;

// Faz uma consulta a base de dados em cada pedido autenticado para rejeitar tokens de utilizadores apagados
@Component
public class ConversorTokenUtilizador implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter conversorPadrao = new JwtAuthenticationConverter();
    private final UtilizadorRepository utilizadorRepository;

    public ConversorTokenUtilizador(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer id = lerId(jwt.getSubject());
        if (id == null || !utilizadorRepository.existsById(id)) {
            throw new InvalidBearerTokenException("O utilizador do token já não existe.");
        }
        return conversorPadrao.convert(jwt);
    }

    private Integer lerId(String subject) {
        try {
            return subject == null ? null : Integer.valueOf(subject);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
