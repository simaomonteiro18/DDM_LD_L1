package pt.istec.ishopping.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;

import pt.istec.ishopping.config.InicializadorUtilizador;
import pt.istec.ishopping.model.Artigo;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.ItemCompra;
import pt.istec.ishopping.model.ItemCompraNaoPrevisto;
import pt.istec.ishopping.model.ItemCompraPrevisto;
import pt.istec.ishopping.model.TipoArtigo;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.ArtigoRepository;
import pt.istec.ishopping.repository.CompraRepository;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ItemCompraApiTest {

    private static final String BASE = "/api/compras/1/itens";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private ItemCompraRepository itemCompraRepository;
    @MockitoBean
    private CompraRepository compraRepository;
    @MockitoBean
    private ArtigoRepository artigoRepository;
    @MockitoBean
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private InicializadorUtilizador inicializadorUtilizador;

    private String autorizacao;
    private Compra compra;
    private Artigo arroz;

    @BeforeEach
    void preparar() {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", 1);
        autorizacao = "Bearer " + jwtService.gerar(utilizador).valor();
        compra = new Compra();
        ReflectionTestUtils.setField(compra, "id", 1);
        compra.setNome("Semana 1");
        TipoArtigo mercearia = new TipoArtigo();
        ReflectionTestUtils.setField(mercearia, "id", 2);
        mercearia.setNome("Mercearia");
        arroz = new Artigo();
        ReflectionTestUtils.setField(arroz, "id", 3);
        arroz.setNome("Arroz");
        arroz.setTipoArtigo(mercearia);
    }

    private ItemCompraPrevisto criarPrevisto(Integer id) {
        ItemCompraPrevisto item = new ItemCompraPrevisto();
        ReflectionTestUtils.setField(item, "id", id);
        item.setCompra(compra);
        item.setArtigo(arroz);
        item.setQuantidadePrevista(2);
        return item;
    }

    private ItemCompraNaoPrevisto criarNaoPrevisto(Integer id) {
        ItemCompraNaoPrevisto item = new ItemCompraNaoPrevisto();
        ReflectionTestUtils.setField(item, "id", id);
        item.setCompra(compra);
        item.setArtigo(arroz);
        item.setQuantidadeAdquirida(1);
        item.setPrecoUnitario(new BigDecimal("1.99"));
        item.setObservacoes("promoção");
        return item;
    }

    private void compraAberta() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(artigoRepository.findById(3)).thenReturn(Optional.of(arroz));
        when(itemCompraRepository.saveAndFlush(any(ItemCompra.class))).thenAnswer(pedido -> {
            ItemCompra guardado = pedido.getArgument(0);
            if (guardado.getId() == null) {
                ReflectionTestUtils.setField(guardado, "id", 7);
            }
            return guardado;
        });
    }

    private void compraFechada() {
        compra.fechar(new Utilizador());
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
    }

    private ResultActions enviar(String caminho, String json) throws Exception {
        return mockMvc.perform(post(BASE + caminho).header("Authorization", autorizacao)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions atualizar(Integer id, String json) throws Exception {
        return mockMvc.perform(put(BASE + "/" + id).header("Authorization", autorizacao)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void pedidosSemTokenDao401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/previstos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/nao-previstos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void listarIndicaOTipoDeCadaItem() throws Exception {
        when(compraRepository.existsById(1)).thenReturn(true);
        when(itemCompraRepository.findByCompraIdOrderByIdAsc(1)).thenReturn(List.of(criarPrevisto(1), criarNaoPrevisto(2)));

        mockMvc.perform(get(BASE).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("PREVISTO"))
                .andExpect(jsonPath("$[0].quantidadePrevista").value(2))
                .andExpect(jsonPath("$[0].artigo.nome").value("Arroz"))
                .andExpect(jsonPath("$[0].artigo.tipo.nome").value("Mercearia"))
                .andExpect(jsonPath("$[0].observacoes").isEmpty())
                .andExpect(jsonPath("$[1].tipo").value("NAO_PREVISTO"))
                .andExpect(jsonPath("$[1].quantidadePrevista").isEmpty())
                .andExpect(jsonPath("$[1].precoUnitario").value(1.99))
                .andExpect(jsonPath("$[1].observacoes").value("promoção"));
    }

    @Test
    void listarDeCompraInexistenteDa404() throws Exception {
        when(compraRepository.existsById(9)).thenReturn(false);

        mockMvc.perform(get("/api/compras/9/itens").header("Authorization", autorizacao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("A compra não existe."));
    }

    @Test
    void obterDevolve200() throws Exception {
        when(compraRepository.existsById(1)).thenReturn(true);
        when(itemCompraRepository.findByIdAndCompraId(2, 1)).thenReturn(Optional.of(criarNaoPrevisto(2)));

        mockMvc.perform(get(BASE + "/2").header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.tipo").value("NAO_PREVISTO"));
    }

    @Test
    void obterItemDeOutraCompraDa404() throws Exception {
        when(compraRepository.existsById(1)).thenReturn(true);
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/5").header("Authorization", autorizacao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.erro").value("O item de compra não existe."))
                .andExpect(jsonPath("$.caminho").value(BASE + "/5"));
    }

    @Test
    void idQueNaoEhNumeroDa400() throws Exception {
        mockMvc.perform(get("/api/compras/abc/itens").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/abc").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
    }

    @Test
    void criarPrevistoDevolve201ComLocation() throws Exception {
        compraAberta();

        enviar("/previstos", "{\"idArtigo\":3,\"quantidadePrevista\":2}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.tipo").value("PREVISTO"))
                .andExpect(jsonPath("$.quantidadePrevista").value(2))
                .andExpect(jsonPath("$.quantidadeAdquirida").isEmpty())
                .andExpect(jsonPath("$.precoUnitario").isEmpty());
    }

    @Test
    void criarNaoPrevistoDevolve201ComLocation() throws Exception {
        compraAberta();

        enviar("/nao-previstos", "{\"idArtigo\":3,\"quantidadeAdquirida\":1,\"precoUnitario\":1.5,\"observacoes\":\"oferta\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/7")))
                .andExpect(jsonPath("$.tipo").value("NAO_PREVISTO"))
                .andExpect(jsonPath("$.precoUnitario").value(1.50))
                .andExpect(jsonPath("$.observacoes").value("oferta"));
    }

    @Test
    void criarPrevistoComDadosEmFaltaDa400() throws Exception {
        enviar("/previstos", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("O artigo é obrigatório.")))
                .andExpect(jsonPath("$.erro").value(containsString("A quantidade prevista é obrigatória.")));
    }

    @Test
    void criarNaoPrevistoSemAquisicaoDa400() throws Exception {
        enviar("/nao-previstos", "{\"idArtigo\":3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("A quantidade adquirida é obrigatória.")))
                .andExpect(jsonPath("$.erro").value(containsString("O preço unitário é obrigatório.")));
    }

    @Test
    void restricoesDoEsquemaDao400() throws Exception {
        enviar("/previstos", "{\"idArtigo\":3,\"quantidadePrevista\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A quantidade prevista tem de ser maior que zero."));
        enviar("/nao-previstos", "{\"idArtigo\":3,\"quantidadeAdquirida\":-1,\"precoUnitario\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A quantidade adquirida tem de ser maior que zero."));
        enviar("/nao-previstos", "{\"idArtigo\":3,\"quantidadeAdquirida\":1,\"precoUnitario\":-0.01}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O preço unitário não pode ser negativo."));
        enviar("/nao-previstos", "{\"idArtigo\":3,\"quantidadeAdquirida\":1,\"precoUnitario\":1.999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("2 casas decimais")));
        enviar("/previstos", "{\"idArtigo\":3,\"quantidadePrevista\":2,\"quantidadeAdquirida\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("A quantidade adquirida e o preço unitário têm de ser indicados em conjunto."));
        enviar("/previstos", "{\"idArtigo\":3,\"quantidadePrevista\":2.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void jsonMalFormadoDa400() throws Exception {
        enviar("/previstos", "{")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void criarComArtigoInexistenteDa404() throws Exception {
        compraAberta();
        when(artigoRepository.findById(9)).thenReturn(Optional.empty());

        enviar("/previstos", "{\"idArtigo\":9,\"quantidadePrevista\":2}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("O artigo não existe."));
    }

    @Test
    void criarEmCompraInexistenteDa404() throws Exception {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.empty());

        enviar("/previstos", "{\"idArtigo\":3,\"quantidadePrevista\":2}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("A compra não existe."));
    }

    @Test
    void criarEmCompraFechadaDa409() throws Exception {
        compraFechada();

        enviar("/nao-previstos", "{\"idArtigo\":3,\"quantidadeAdquirida\":1,\"precoUnitario\":1}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.erro").value("Não é possível adicionar itens a uma compra fechada."));
    }

    @Test
    void atualizarPrevistoDevolve200() throws Exception {
        compraAberta();
        when(itemCompraRepository.findByIdAndCompraId(1, 1)).thenReturn(Optional.of(criarPrevisto(1)));

        atualizar(1, "{\"idArtigo\":3,\"quantidadePrevista\":3,\"quantidadeAdquirida\":3,\"precoUnitario\":0.99}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("PREVISTO"))
                .andExpect(jsonPath("$.quantidadePrevista").value(3))
                .andExpect(jsonPath("$.quantidadeAdquirida").value(3))
                .andExpect(jsonPath("$.precoUnitario").value(0.99));
    }

    @Test
    void atualizarNaoMudaOTipoDoItem() throws Exception {
        compraAberta();
        when(itemCompraRepository.findByIdAndCompraId(2, 1)).thenReturn(Optional.of(criarNaoPrevisto(2)));

        atualizar(2, "{\"idArtigo\":3,\"quantidadePrevista\":2,\"quantidadeAdquirida\":1,\"precoUnitario\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erro").value("Um item não previsto não tem quantidade prevista."));
    }

    @Test
    void atualizarItemDeOutraCompraDa404() throws Exception {
        compraAberta();
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.empty());

        atualizar(5, "{\"idArtigo\":3,\"quantidadePrevista\":2}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("O item de compra não existe."));
    }

    @Test
    void atualizarEmCompraFechadaDa409() throws Exception {
        compraFechada();

        atualizar(1, "{\"idArtigo\":3,\"quantidadePrevista\":2}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é possível alterar itens de uma compra fechada."));
    }

    @Test
    void apagarDevolve204() throws Exception {
        compraAberta();
        when(itemCompraRepository.findByIdAndCompraId(1, 1)).thenReturn(Optional.of(criarPrevisto(1)));

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isNoContent());
    }

    @Test
    void apagarItemDeOutraCompraDa404() throws Exception {
        compraAberta();
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.empty());

        mockMvc.perform(delete(BASE + "/5").header("Authorization", autorizacao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("O item de compra não existe."));
    }

    @Test
    void apagarEmCompraFechadaDa409() throws Exception {
        compraFechada();

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é possível apagar itens de uma compra fechada."));
    }
}
