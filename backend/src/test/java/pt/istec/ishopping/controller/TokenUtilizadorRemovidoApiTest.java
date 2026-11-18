package pt.istec.ishopping.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import pt.istec.ishopping.config.InicializadorUtilizador;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenUtilizadorRemovidoApiTest {

    private static final String UTILIZADORES = "/api/utilizadores";
    private static final String MENSAGEM_401 = "Autenticação necessária. O token está em falta, é inválido ou expirou.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JwtEncoder jwtEncoder;
    @MockitoBean
    private UtilizadorRepository utilizadorRepository;
    @MockitoBean
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private InicializadorUtilizador inicializadorUtilizador;

    private final Set<Integer> existentes = new HashSet<>();
    private Utilizador admin;
    private Utilizador rui;

    @BeforeEach
    void preparar() {
        admin = criarUtilizador(1, "admin");
        rui = criarUtilizador(2, "rui");
        existentes.addAll(List.of(1, 2));
        when(utilizadorRepository.existsById(anyInt()))
                .thenAnswer(pedido -> existentes.contains(pedido.<Integer>getArgument(0)));
        when(utilizadorRepository.findById(1)).thenReturn(Optional.of(admin));
        when(utilizadorRepository.findById(2)).thenReturn(Optional.of(rui));
        when(utilizadorRepository.findAll(Sort.by("nome", "id"))).thenReturn(List.of(admin, rui));
        doAnswer(pedido -> existentes.remove(pedido.<Utilizador>getArgument(0).getId()))
                .when(utilizadorRepository).delete(any(Utilizador.class));
    }

    private Utilizador criarUtilizador(Integer id, String username) {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", id);
        utilizador.setNome(username);
        utilizador.setUsername(username);
        return utilizador;
    }

    private String autorizacao(Utilizador utilizador) {
        return "Bearer " + jwtService.gerar(utilizador).valor();
    }

    @Test
    void tokenDeUtilizadorExistenteFunciona() throws Exception {
        mockMvc.perform(get(UTILIZADORES).header("Authorization", autorizacao(rui)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].username").value("rui"));
    }

    @Test
    void tokenDeUtilizadorApagadoDevolve401NoFormatoHabitual() throws Exception {
        existentes.remove(2);

        mockMvc.perform(get(UTILIZADORES).header("Authorization", autorizacao(rui)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.erro").value(MENSAGEM_401))
                .andExpect(jsonPath("$.caminho").value(UTILIZADORES));
    }

    @Test
    void utilizadorApagadoAMeioDaSessaoPerdeOAcesso() throws Exception {
        String tokenRui = autorizacao(rui);
        String tokenAdmin = autorizacao(admin);

        mockMvc.perform(get(UTILIZADORES).header("Authorization", tokenRui))
                .andExpect(status().isOk());

        mockMvc.perform(delete(UTILIZADORES + "/2").header("Authorization", tokenAdmin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(UTILIZADORES).header("Authorization", tokenRui))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.erro").value(MENSAGEM_401));
        mockMvc.perform(get(UTILIZADORES).header("Authorization", tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void tokenComSubjectNaoNumericoDevolve401() throws Exception {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet dados = JwtClaimsSet.builder()
                .subject("abc")
                .issuedAt(agora)
                .expiresAt(agora.plus(1, ChronoUnit.HOURS))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), dados))
                .getTokenValue();

        mockMvc.perform(get(UTILIZADORES).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(MENSAGEM_401));
    }
}
