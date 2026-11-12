package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pt.istec.ishopping.dto.CompraAtualizarPedido;
import pt.istec.ishopping.dto.CompraCriarPedido;
import pt.istec.ishopping.dto.CompraResposta;
import pt.istec.ishopping.dto.UtilizadorResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.ItemCompraPrevisto;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.CompraRepository;
import pt.istec.ishopping.security.UtilizadorAutenticado;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;
    @Mock
    private UtilizadorAutenticado utilizadorAutenticado;
    @InjectMocks
    private CompraService compraService;

    private Utilizador criarUtilizador(Integer id, String username) {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", id);
        utilizador.setNome(username);
        utilizador.setUsername(username);
        return utilizador;
    }

    private Compra criarCompra(Integer id, String nome) {
        Compra compra = new Compra();
        ReflectionTestUtils.setField(compra, "id", id);
        compra.setNome(nome);
        compra.setCriadoPor(criarUtilizador(1, "ana"));
        return compra;
    }

    private Compra criarCompraFechada(Integer id) {
        Compra compra = criarCompra(id, "Semana 1");
        compra.fechar(criarUtilizador(1, "ana"));
        return compra;
    }

    @Test
    void listarSemFiltroDevolveTodas() {
        when(compraRepository.findAllByOrderByDataCriacaoDescIdDesc())
                .thenReturn(List.of(criarCompra(2, "Semana 2"), criarCompraFechada(1)));

        assertThat(compraService.listar(null))
                .extracting(CompraResposta::id)
                .containsExactly(2, 1);
    }

    @Test
    void listarComFiltroUsaOEstadoIndicado() {
        when(compraRepository.findByFechadaOrderByDataCriacaoDescIdDesc(true)).thenReturn(List.of(criarCompraFechada(1)));

        assertThat(compraService.listar(true))
                .extracting(CompraResposta::fechada)
                .containsExactly(true);
        verify(compraRepository, never()).findAllByOrderByDataCriacaoDescIdDesc();
    }

    @Test
    void obterInexistenteDaNaoEncontrado() {
        when(compraRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.obter(9))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("A compra não existe.");
    }

    @Test
    void criarGuardaONomeEOCriadorComACompraAberta() {
        when(utilizadorAutenticado.obter()).thenReturn(criarUtilizador(1, "ana"));
        when(compraRepository.saveAndFlush(any(Compra.class))).thenAnswer(pedido -> {
            Compra guardada = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardada, "id", 5);
            return guardada;
        });

        CompraResposta resposta = compraService.criar(new CompraCriarPedido("  Semana 1 "));

        assertThat(resposta.id()).isEqualTo(5);
        assertThat(resposta.nome()).isEqualTo("Semana 1");
        assertThat(resposta.fechada()).isFalse();
        assertThat(resposta.criadoPor()).isEqualTo(new UtilizadorResposta(1, "ana", "ana"));
        assertThat(resposta.alteradoPor()).isNull();
        assertThat(resposta.fechadaPor()).isNull();
        assertThat(resposta.dataFechada()).isNull();
    }

    @Test
    void atualizarAlteraONomeERegistaQuemAlterou() {
        Compra compra = criarCompra(1, "Semana 1");
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(utilizadorAutenticado.obter()).thenReturn(criarUtilizador(2, "rui"));
        when(compraRepository.saveAndFlush(compra)).thenReturn(compra);

        CompraResposta resposta = compraService.atualizar(1, new CompraAtualizarPedido("Semana 2"));

        assertThat(resposta.nome()).isEqualTo("Semana 2");
        assertThat(resposta.criadoPor().username()).isEqualTo("ana");
        assertThat(resposta.alteradoPor().username()).isEqualTo("rui");
        assertThat(resposta.dataAlterado()).isNotNull();
    }

    @Test
    void atualizarCompraFechadaDaConflito() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(criarCompraFechada(1)));

        assertThatThrownBy(() -> compraService.atualizar(1, new CompraAtualizarPedido("Outro")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível alterar uma compra fechada.");
        verify(compraRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarInexistenteDaNaoEncontrado() {
        when(compraRepository.findByIdParaAlterar(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.atualizar(9, new CompraAtualizarPedido("Outro")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("A compra não existe.");
    }

    @Test
    void fecharPreencheQuemFechouEAData() {
        Compra compra = criarCompra(1, "Semana 1");
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(utilizadorAutenticado.obter()).thenReturn(criarUtilizador(2, "rui"));
        when(compraRepository.saveAndFlush(compra)).thenReturn(compra);

        CompraResposta resposta = compraService.fechar(1);

        assertThat(resposta.fechada()).isTrue();
        assertThat(resposta.fechadaPor().username()).isEqualTo("rui");
        assertThat(resposta.dataFechada()).isNotNull();
    }

    @Test
    void fecharCompraJaFechadaDaConflito() {
        Compra compra = criarCompraFechada(1);
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> compraService.fechar(1))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("A compra já está fechada.");
        assertThat(compra.getFechadaPor().getUsername()).isEqualTo("ana");
        verify(compraRepository, never()).saveAndFlush(any());
    }

    @Test
    void fecharInexistenteDaNaoEncontrado() {
        when(compraRepository.findByIdParaAlterar(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.fechar(9))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("A compra não existe.");
    }

    @Test
    void apagarCompraAbertaComItensApagaSoACompra() {
        // item_compra tem ON DELETE CASCADE para compra e a entidade tem cascade ALL nos itens,
        // por isso basta apagar a compra para os itens irem com ela
        Compra compra = criarCompra(1, "Semana 1");
        compra.adicionarItem(new ItemCompraPrevisto());
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));

        compraService.apagar(1);

        verify(compraRepository).delete(compra);
        verify(compraRepository).flush();
    }

    @Test
    void apagarCompraFechadaDaConflito() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(criarCompraFechada(1)));

        assertThatThrownBy(() -> compraService.apagar(1))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível apagar uma compra fechada.");
        verify(compraRepository, never()).delete(any());
    }

    @Test
    void apagarInexistenteDaNaoEncontrado() {
        when(compraRepository.findByIdParaAlterar(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.apagar(9))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("A compra não existe.");
    }
}
