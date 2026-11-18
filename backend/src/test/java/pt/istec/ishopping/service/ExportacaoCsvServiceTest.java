package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pt.istec.ishopping.model.Artigo;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.ItemCompra;
import pt.istec.ishopping.model.ItemCompraNaoPrevisto;
import pt.istec.ishopping.model.ItemCompraPrevisto;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.security.UtilizadorAutenticado;

@ExtendWith(MockitoExtension.class)
class ExportacaoCsvServiceTest {

    private static final String CABECALHO = "NomeCompra;DataCriacao;DataFechada;NomeArtigo;ArtigoPrevisto;"
            + "ArtigoNaoPrevisto;QuantidadePrevista;QuantidadeAdquirida;PrecoUnitario\r\n";

    @Mock
    private ItemCompraRepository itemCompraRepository;
    @Mock
    private UtilizadorAutenticado utilizadorAutenticado;
    @InjectMocks
    private ExportacaoCsvService exportacaoCsvService;

    @BeforeEach
    void preparar() {
        Utilizador ana = new Utilizador();
        ReflectionTestUtils.setField(ana, "id", 3);
        when(utilizadorAutenticado.obter()).thenReturn(ana);
    }

    // Criada a 15 de julho (hora de verao, UTC+1) e fechada a 2 de dezembro (hora de inverno, UTC+0)
    private Compra criarCompra(String nome) {
        Compra compra = new Compra();
        ReflectionTestUtils.setField(compra, "id", 1);
        ReflectionTestUtils.setField(compra, "dataCriacao", Instant.parse("2026-07-15T08:05:00Z"));
        compra.setNome(nome);
        compra.fechar(new Utilizador());
        ReflectionTestUtils.setField(compra, "dataFechada", Instant.parse("2026-12-02T18:30:00Z"));
        return compra;
    }

    private Artigo criarArtigo(String nome) {
        Artigo artigo = new Artigo();
        artigo.setNome(nome);
        return artigo;
    }

    private ItemCompraPrevisto criarPrevisto(Compra compra, String artigo, Integer quantidadePrevista,
            Integer quantidadeAdquirida, String preco) {
        ItemCompraPrevisto item = new ItemCompraPrevisto();
        item.setCompra(compra);
        item.setArtigo(criarArtigo(artigo));
        item.setQuantidadePrevista(quantidadePrevista);
        item.setQuantidadeAdquirida(quantidadeAdquirida);
        item.setPrecoUnitario(preco == null ? null : new BigDecimal(preco));
        return item;
    }

    private ItemCompraNaoPrevisto criarNaoPrevisto(Compra compra, String artigo, Integer quantidade, String preco) {
        ItemCompraNaoPrevisto item = new ItemCompraNaoPrevisto();
        item.setCompra(compra);
        item.setArtigo(criarArtigo(artigo));
        item.setQuantidadeAdquirida(quantidade);
        item.setPrecoUnitario(new BigDecimal(preco));
        return item;
    }

    private String exportar(List<ItemCompra> itens) {
        when(itemCompraRepository.findItensDeComprasFechadasDoUtilizador(3)).thenReturn(itens);
        return exportacaoCsvService.comprasFechadas();
    }

    @Test
    void semComprasFechadasDevolveSoOCabecalho() {
        assertThat(exportar(List.of())).isEqualTo(CABECALHO);
    }

    @Test
    void procuraSoAsComprasDoUtilizadorAutenticado() {
        exportar(List.of());

        verify(itemCompraRepository).findItensDeComprasFechadasDoUtilizador(3);
    }

    @Test
    void escreveUmaLinhaPorItemComDatasDeLisboaEVirgulaDecimal() {
        Compra compra = criarCompra("Semana 1");

        String csv = exportar(List.of(
                criarPrevisto(compra, "Arroz", 2, 2, "1.5"),
                criarPrevisto(compra, "Massa", 1, null, null),
                criarNaoPrevisto(compra, "Azeite", 1, "1234.5")));

        assertThat(csv).isEqualTo(CABECALHO
                + "Semana 1;15/07/2026 09:05;02/12/2026 18:30;Arroz;Sim;Não;2;2;1,50\r\n"
                + "Semana 1;15/07/2026 09:05;02/12/2026 18:30;Massa;Sim;Não;1;;\r\n"
                + "Semana 1;15/07/2026 09:05;02/12/2026 18:30;Azeite;Não;Sim;;1;1234,50\r\n");
    }

    @Test
    void escapaPontoEVirgulaAspasEQuebrasDeLinha() {
        Compra compra = criarCompra("Festa; jantar");

        String csv = exportar(List.of(
                criarNaoPrevisto(compra, "Vinho \"tinto\"", 1, "5"),
                criarNaoPrevisto(compra, "Queijo\nda serra", 1, "8")));

        assertThat(csv).contains("\"Festa; jantar\";15/07/2026 09:05;02/12/2026 18:30;\"Vinho \"\"tinto\"\"\";");
        assertThat(csv).contains(";\"Queijo\nda serra\";");
    }

    @Test
    void neutralizaFormulasNosCamposDeTexto() {
        Compra compra = criarCompra("=SOMA(A1:A9)");

        String csv = exportar(List.of(criarNaoPrevisto(compra, "+Leite", 1, "1")));

        assertThat(csv).contains("'=SOMA(A1:A9);").contains(";'+Leite;");
        assertThat(ExportacaoCsvService.texto("-1")).isEqualTo("'-1");
        assertThat(ExportacaoCsvService.texto("@cmd")).isEqualTo("'@cmd");
        assertThat(ExportacaoCsvService.texto("\tTab")).isEqualTo("'\tTab");
        assertThat(ExportacaoCsvService.texto("\rRetorno")).isEqualTo("\"'\rRetorno\"");
        assertThat(ExportacaoCsvService.texto("Arroz")).isEqualTo("Arroz");
    }
}
