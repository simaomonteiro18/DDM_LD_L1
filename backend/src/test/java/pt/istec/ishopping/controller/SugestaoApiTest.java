package pt.istec.ishopping.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;

import pt.istec.ishopping.config.InicializadorUtilizador;
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.ItemAdquirido;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.repository.OrcamentoRepository;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SugestaoApiTest {

    private static final String ORCAMENTO = "/api/sugestoes/orcamento";
    private static final String LISTA = "/api/sugestoes/lista-compras";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private OrcamentoRepository orcamentoRepository;
    @MockitoBean
    private ItemCompraRepository itemCompraRepository;
    @MockitoBean
    private UtilizadorRepository utilizadorRepository;
    @MockitoBean
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private InicializadorUtilizador inicializadorUtilizador;

    private String autorizacao;

    @BeforeEach
    void preparar() {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", 1);
        when(utilizadorRepository.existsById(1)).thenReturn(true);
        autorizacao = "Bearer " + jwtService.gerar(utilizador).valor();
    }

    private ResultActions pedir(String caminho, String... parametros) throws Exception {
        var pedido = get(caminho).header("Authorization", autorizacao);
        for (int i = 0; i < parametros.length; i += 2) {
            pedido.param(parametros[i], parametros[i + 1]);
        }
        return mockMvc.perform(pedido);
    }

    @Test
    void pedidosSemTokenDao401() throws Exception {
        mockMvc.perform(get(ORCAMENTO)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(LISTA).param("semana", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void orcamentoDevolveOValorSugerido() throws Exception {
        Orcamento outubro = new Orcamento();
        outubro.setValor(new BigDecimal("200"));
        Orcamento novembro = new Orcamento();
        novembro.setValor(new BigDecimal("150.5"));
        when(orcamentoRepository.findAnterioresA(anyInt(), anyInt(), any())).thenReturn(List.of(novembro, outubro));

        pedir(ORCAMENTO, "ano", "2026", "mes", "12")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.mes").value(12))
                .andExpect(jsonPath("$.valorSugerido").value(175.25))
                .andExpect(jsonPath("$.baseadoEm").value(2));
    }

    @Test
    void orcamentoSemAnterioresDa404() throws Exception {
        when(orcamentoRepository.findAnterioresA(anyInt(), anyInt(), any())).thenReturn(List.of());

        pedir(ORCAMENTO, "ano", "2026", "mes", "12")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.erro").value("Não existem orçamentos anteriores para sugerir um valor."))
                .andExpect(jsonPath("$.caminho").value(ORCAMENTO));
    }

    @Test
    void orcamentoComParametrosInvalidosDa400() throws Exception {
        pedir(ORCAMENTO, "mes", "13")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O mês tem de estar entre 1 e 12."));
        pedir(ORCAMENTO, "ano", "1999")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O ano tem de estar entre 2000 e 2100."));
        pedir(ORCAMENTO, "mes", "abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void listaComprasDevolveOsArtigosDaSemana() throws Exception {
        when(itemCompraRepository.findItensAdquiridosDeComprasFechadas()).thenReturn(List.of(
                new ItemAdquirido(1, Instant.parse("2026-01-09T10:00:00Z"), 4, "Leite", 2, "Laticínios", 2),
                new ItemAdquirido(2, Instant.parse("2026-02-10T10:00:00Z"), 4, "Leite", 2, "Laticínios", 4)));

        pedir(LISTA, "semana", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idArtigo").value(4))
                .andExpect(jsonPath("$[0].nomeArtigo").value("Leite"))
                .andExpect(jsonPath("$[0].tipoArtigo.id").value(2))
                .andExpect(jsonPath("$[0].tipoArtigo.nome").value("Laticínios"))
                .andExpect(jsonPath("$[0].vezes").value(2))
                .andExpect(jsonPath("$[0].quantidadeSugerida").value(3));
    }

    @Test
    void listaComprasSemHistoricoDevolveListaVazia() throws Exception {
        when(itemCompraRepository.findItensAdquiridosDeComprasFechadas()).thenReturn(List.of());

        pedir(LISTA, "semana", "4")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listaComprasComSemanaInvalidaOuEmFaltaDa400() throws Exception {
        pedir(LISTA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erro").value("A semana é obrigatória."))
                .andExpect(jsonPath("$.caminho").value(LISTA));
        pedir(LISTA, "semana", "5")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A semana tem de estar entre 1 e 4."));
        pedir(LISTA, "semana", "abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
        pedir(LISTA, "semana", "1.5")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }
}
