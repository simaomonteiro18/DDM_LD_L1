package pt.istec.ishopping.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class RespostasSegurancaTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final RespostasSeguranca respostas = new RespostasSeguranca(objectMapper);

    @Test
    void acessoNegadoDevolve403NoFormatoDeErro() throws Exception {
        MockHttpServletRequest pedido = new MockHttpServletRequest("GET", "/api/compras");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        respostas.handle(pedido, resposta, new AccessDeniedException("negado"));

        JsonNode corpo = objectMapper.readTree(resposta.getContentAsString(StandardCharsets.UTF_8));
        assertThat(resposta.getStatus()).isEqualTo(403);
        assertThat(corpo.get("estado").asInt()).isEqualTo(403);
        assertThat(corpo.get("erro").asString()).isEqualTo("Não tem permissão para aceder a este recurso.");
        assertThat(corpo.get("caminho").asString()).isEqualTo("/api/compras");
        assertThat(corpo.get("timestamp").asString()).contains("T");
    }
}
