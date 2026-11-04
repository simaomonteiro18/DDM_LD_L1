package pt.istec.ishopping.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_compra_previsto")
@PrimaryKeyJoinColumn(name = "id")
public class ItemCompraPrevisto extends ItemCompra {

    @Column(name = "quantidade_prevista", nullable = false)
    private Integer quantidadePrevista;

    public Integer getQuantidadePrevista() {
        return quantidadePrevista;
    }

    public void setQuantidadePrevista(Integer quantidadePrevista) {
        this.quantidadePrevista = quantidadePrevista;
    }
}
