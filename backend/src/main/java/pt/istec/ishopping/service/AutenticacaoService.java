package pt.istec.ishopping.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import pt.istec.ishopping.dto.LoginPedido;
import pt.istec.ishopping.dto.LoginResposta;
import pt.istec.ishopping.dto.UtilizadorResposta;
import pt.istec.ishopping.exception.NaoAutenticadoException;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@Service
public class AutenticacaoService {

    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String hashFicticio;

    public AutenticacaoService(UtilizadorRepository utilizadorRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.utilizadorRepository = utilizadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public LoginResposta login(LoginPedido pedido) {
        Optional<Utilizador> utilizador = utilizadorRepository.findByUsernameIgnoreCase(pedido.username());
        // Compara sempre um hash para o tempo de resposta nao revelar se o username existe
        String hash = utilizador.map(Utilizador::getPasswordHash).orElse(hashFicticio);
        boolean passwordCorreta = passwordEncoder.matches(pedido.password(), hash);
        if (utilizador.isEmpty() || !passwordCorreta) {
            throw new NaoAutenticadoException("Credenciais inválidas.");
        }
        JwtService.TokenGerado token = jwtService.gerar(utilizador.get());
        return new LoginResposta(token.valor(), "Bearer", token.expiraEm(), UtilizadorResposta.de(utilizador.get()));
    }
}
