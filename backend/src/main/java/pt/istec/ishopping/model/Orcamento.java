package pt.istec.ishopping.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orcamento")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_criado_por", nullable = false)
    private Utilizador criadoPor;

    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alterado_por")
    private Utilizador alteradoPor;

    @Column(name = "data_alterado")
    private Instant dataAlterado;

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

    public Integer getId() {
        return id;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Utilizador getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(Utilizador criadoPor) {
        this.criadoPor = criadoPor;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Utilizador getAlteradoPor() {
        return alteradoPor;
    }

    public Instant getDataAlterado() {
        return dataAlterado;
    }
}
