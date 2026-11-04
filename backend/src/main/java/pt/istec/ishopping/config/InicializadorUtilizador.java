package pt.istec.ishopping.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;

@Component
public class InicializadorUtilizador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorUtilizador.class);

    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties admin;

    public InicializadorUtilizador(UtilizadorRepository utilizadorRepository, PasswordEncoder passwordEncoder,
            AdminProperties admin) {
        this.utilizadorRepository = utilizadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.admin = admin;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (utilizadorRepository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(admin.username()) || !StringUtils.hasText(admin.nome())
                || !StringUtils.hasText(admin.password())) {
            log.warn("Não existem utilizadores e faltam as variáveis ADMIN_USERNAME, ADMIN_NOME ou ADMIN_PASSWORD. "
                    + "Não foi criado nenhum utilizador.");
            return;
        }
        Utilizador utilizador = new Utilizador();
        utilizador.setUsername(admin.username());
        utilizador.setNome(admin.nome());
        utilizador.setPasswordHash(passwordEncoder.encode(admin.password()));
        utilizadorRepository.save(utilizador);
        log.info("Primeiro utilizador criado com o username {}.", admin.username());
    }
}
