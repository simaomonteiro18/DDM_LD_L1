package pt.istec.ishopping.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacaoApiTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String PROTEGIDO = "/api/protegido";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private UtilizadorRepository utilizadorRepository;

    private Utilizador criarUtilizador() {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", 1);
        utilizador.setNome("Ana Silva");
        utilizador.setUsername("ana");
        utilizador.setPasswordHash(passwordEncoder.encode("segredo"));
        return utilizador;
    }

    private String corpoLogin(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void loginValidoDevolveTokenNoFormatoCombinado() throws Exception {
        when(utilizadorRepository.findByUsernameIgnoreCase("ANA")).thenReturn(Optional.of(criarUtilizador()));

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(corpoLogin("ANA", "segredo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").value(containsString("T")))
                .andExpect(jsonPath("$.utilizador.id").value(1))
                .andExpect(jsonPath("$.utilizador.nome").value("Ana Silva"))
                .andExpect(jsonPath("$.utilizador.username").value("ana"))
                .andExpect(jsonPath("$.utilizador.passwordHash").doesNotExist());
    }

    @Test
    void passwordErradaDevolve401() throws Exception {
        when(utilizadorRepository.findByUsernameIgnoreCase("ana")).thenReturn(Optional.of(criarUtilizador()));

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(corpoLogin("ana", "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").value(containsString("T")))
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.erro").value("Credenciais inválidas."))
                .andExpect(jsonPath("$.caminho").value(LOGIN));
    }

    @Test
    void usernameInexistenteDevolve401ComAMesmaMensagem() throws Exception {
        when(utilizadorRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(corpoLogin("ninguem", "segredo")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Credenciais inválidas."));
    }

    @Test
    void loginSemCamposDevolve400ComMensagensEmPortugues() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erro").value("A palavra-passe é obrigatória. O username é obrigatório."))
                .andExpect(jsonPath("$.caminho").value(LOGIN));
    }

    @Test
    void jsonMalFormadoDevolve400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void erroInesperadoDevolve500SemDetalhes() throws Exception {
        when(utilizadorRepository.findByUsernameIgnoreCase("ana")).thenThrow(new IllegalStateException("detalhe interno"));

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(corpoLogin("ana", "segredo")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.estado").value(500))
                .andExpect(jsonPath("$.erro").value("Ocorreu um erro interno. Tente novamente mais tarde."))
                .andExpect(content().string(not(containsString("detalhe interno"))));
    }

    @Test
    void endpointProtegidoSemTokenDevolve401() throws Exception {
        mockMvc.perform(get(PROTEGIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.erro").value(containsString("Autenticação necessária")))
                .andExpect(jsonPath("$.caminho").value(PROTEGIDO));
    }

    @Test
    void endpointProtegidoComTokenInvalidoDevolve401() throws Exception {
        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void tokenValidoPassaASegurancaEEnderecoInexistenteDevolve404() throws Exception {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", 1);
        when(utilizadorRepository.existsById(1)).thenReturn(true);
        String token = jwtService.gerar(utilizador).valor();

        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.erro").value("O recurso pedido não existe."));
    }

    @Test
    void healthSemTokenDevolve200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
