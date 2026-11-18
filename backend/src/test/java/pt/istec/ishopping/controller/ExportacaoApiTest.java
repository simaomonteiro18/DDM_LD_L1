package pt.istec.ishopping.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import pt.istec.ishopping.config.InicializadorUtilizador;
import pt.istec.ishopping.model.Artigo;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.ItemCompraNaoPrevisto;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExportacaoApiTest {

    private static final String EXPORTAR = "/api/compras/exportar";
    private static final String CABECALHO = "NomeCompra;DataCriacao;DataFechada;NomeArtigo;ArtigoPrevisto;"
            + "ArtigoNaoPrevisto;QuantidadePrevista;QuantidadeAdquirida;PrecoUnitario\r\n";
    private static final byte[] BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
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
        Utilizador ana = new Utilizador();
        ReflectionTestUtils.setField(ana, "id", 1);
        when(utilizadorRepository.existsById(1)).thenReturn(true);
        when(utilizadorRepository.findById(1)).thenReturn(Optional.of(ana));
        autorizacao = "Bearer " + jwtService.gerar(ana).valor();
    }

    private byte[] exportar() throws Exception {
        return mockMvc.perform(get(EXPORTAR).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    @Test
    void semTokenDa401() throws Exception {
        mockMvc.perform(get(EXPORTAR))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void devolveUmFicheiroCsvComBomECabecalhoMesmoSemCompras() throws Exception {
        when(itemCompraRepository.findItensDeComprasFechadasDoUtilizador(1)).thenReturn(List.of());

        byte[] corpo = mockMvc.perform(get(EXPORTAR).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("filename=\"compras-fechadas.csv\"")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(Arrays.copyOf(corpo, 3)).isEqualTo(BOM);
        assertThat(new String(corpo, 3, corpo.length - 3, StandardCharsets.UTF_8)).isEqualTo(CABECALHO);
    }

    @Test
    void exportarNaoELidoComoIdDeCompra() throws Exception {
        Compra compra = new Compra();
        ReflectionTestUtils.setField(compra, "id", 1);
        ReflectionTestUtils.setField(compra, "dataCriacao", Instant.parse("2026-01-10T10:00:00Z"));
        compra.setNome("Semana 1");
        compra.fechar(new Utilizador());
        ReflectionTestUtils.setField(compra, "dataFechada", Instant.parse("2026-01-10T11:00:00Z"));
        Artigo leite = new Artigo();
        leite.setNome("Leite");
        ItemCompraNaoPrevisto item = new ItemCompraNaoPrevisto();
        item.setCompra(compra);
        item.setArtigo(leite);
        item.setQuantidadeAdquirida(2);
        item.setPrecoUnitario(new BigDecimal("0.89"));
        when(itemCompraRepository.findItensDeComprasFechadasDoUtilizador(1)).thenReturn(List.of(item));

        String csv = new String(exportar(), StandardCharsets.UTF_8);

        assertThat(csv).endsWith("Semana 1;10/01/2026 10:00;10/01/2026 11:00;Leite;Não;Sim;;2;0,89\r\n");
    }
}
