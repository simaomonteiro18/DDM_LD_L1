package pt.istec.ishopping.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_compra_nao_previsto")
@PrimaryKeyJoinColumn(name = "id")
public class ItemCompraNaoPrevisto extends ItemCompra {

    @Column(columnDefinition = "text")
    private String observacoes;

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
