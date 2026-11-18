package pt.istec.ishopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.ItemCompra;
import pt.istec.ishopping.model.ItemCompraPrevisto;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.security.UtilizadorAutenticado;

@Service
public class ExportacaoCsvService {

    public static final String CABECALHO = "NomeCompra;DataCriacao;DataFechada;NomeArtigo;ArtigoPrevisto;"
            + "ArtigoNaoPrevisto;QuantidadePrevista;QuantidadeAdquirida;PrecoUnitario";

    private static final String SEPARADOR = ";";
    private static final String FIM_DE_LINHA = "\r\n";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Lisbon"));

    private final ItemCompraRepository itemCompraRepository;
    private final UtilizadorAutenticado utilizadorAutenticado;

    public ExportacaoCsvService(ItemCompraRepository itemCompraRepository,
            UtilizadorAutenticado utilizadorAutenticado) {
        this.itemCompraRepository = itemCompraRepository;
        this.utilizadorAutenticado = utilizadorAutenticado;
    }

    // Uma linha por item das compras fechadas criadas pelo utilizador autenticado
    @Transactional(readOnly = true)
    public String comprasFechadas() {
        Integer idUtilizador = utilizadorAutenticado.obter().getId();
        List<ItemCompra> itens = itemCompraRepository.findItensDeComprasFechadasDoUtilizador(idUtilizador);

        StringBuilder csv = new StringBuilder(CABECALHO).append(FIM_DE_LINHA);
        for (ItemCompra item : itens) {
            Compra compra = item.getCompra();
            boolean previsto = item instanceof ItemCompraPrevisto;
            csv.append(String.join(SEPARADOR,
                    texto(compra.getNome()),
                    data(compra.getDataCriacao()),
                    data(compra.getDataFechada()),
                    texto(item.getArtigo().getNome()),
                    previsto ? "Sim" : "Não",
                    previsto ? "Não" : "Sim",
                    previsto ? numero(((ItemCompraPrevisto) item).getQuantidadePrevista()) : "",
                    numero(item.getQuantidadeAdquirida()),
                    preco(item.getPrecoUnitario())))
                    .append(FIM_DE_LINHA);
        }
        return csv.toString();
    }

    // Campos de texto: neutraliza formulas e aplica as regras de escape do CSV
    static String texto(String valor) {
        if (valor == null) {
            return "";
        }
        String seguro = valor;
        if (!seguro.isEmpty() && "=+-@\t\r".indexOf(seguro.charAt(0)) >= 0) {
            seguro = "'" + seguro;
        }
        if (seguro.contains(SEPARADOR) || seguro.contains("\"") || seguro.contains("\n") || seguro.contains("\r")) {
            seguro = "\"" + seguro.replace("\"", "\"\"") + "\"";
        }
        return seguro;
    }

    private static String data(Instant instante) {
        return instante == null ? "" : FORMATO_DATA.format(instante);
    }

    private static String numero(Integer valor) {
        return valor == null ? "" : valor.toString();
    }

    private static String preco(BigDecimal valor) {
        return valor == null ? "" : valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
