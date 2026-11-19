package pt.istec.ishopping.security;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import pt.istec.ishopping.config.JwtProperties;
import pt.istec.ishopping.model.Utilizador;

@Service
public class JwtService {

    public record TokenGerado(String valor, Instant expiraEm) {
    }

    private final JwtEncoder jwtEncoder;
    private final Duration validade;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties propriedades) {
        this.jwtEncoder = jwtEncoder;
        this.validade = Duration.ofHours(propriedades.expiracaoHoras());
    }

    public TokenGerado gerar(Utilizador utilizador) {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = agora.plus(validade);
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet dados = JwtClaimsSet.builder()
                .subject(String.valueOf(utilizador.getId()))
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .build();
        String valor = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, dados)).getTokenValue();
        return new TokenGerado(valor, expiraEm);
    }
}
