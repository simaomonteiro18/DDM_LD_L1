package pt.istec.ishopping.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_criado_por", nullable = false)
    private Utilizador criadoPor;

    @Column(name = "data_alterado")
    private Instant dataAlterado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alterado_por")
    private Utilizador alteradoPor;

    @Column(nullable = false)
    private boolean fechada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fechada_por")
    private Utilizador fechadaPor;

    @Column(name = "data_fechada")
    private Instant dataFechada;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCompra> itens = new ArrayList<>();

    @PrePersist
    void antesDeGuardar() {
        if (dataCriacao == null) {
            dataCriacao = Instant.now();
        }
    }

    public void registarAlteracao(Utilizador utilizador) {
        this.alteradoPor = utilizador;
        this.dataAlterado = Instant.now();
    }

    public void fechar(Utilizador utilizador) {
        this.fechada = true;
        this.fechadaPor = utilizador;
        this.dataFechada = Instant.now();
    }

    public void adicionarItem(ItemCompra item) {
        itens.add(item);
        item.setCompra(this);
    }

    public void removerItem(ItemCompra item) {
        itens.remove(item);
        item.setCompra(null);
    }

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Utilizador getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(Utilizador criadoPor) {
        this.criadoPor = criadoPor;
    }

    public Instant getDataAlterado() {
        return dataAlterado;
    }

    public Utilizador getAlteradoPor() {
        return alteradoPor;
    }

    public boolean isFechada() {
        return fechada;
    }

    public Utilizador getFechadaPor() {
        return fechadaPor;
    }

    public Instant getDataFechada() {
        return dataFechada;
    }

    public List<ItemCompra> getItens() {
        return itens;
    }
}
