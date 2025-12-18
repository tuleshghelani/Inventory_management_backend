package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "powder_coating_process_items", indexes = {
    @Index(name = "idx_pcpi_process_id", columnList = "powder_coating_process_id"),
    @Index(name = "idx_pcpi_product_id", columnList = "product_id"),
    @Index(name = "idx_pcpi_client_id", columnList = "client_id")
})
public class PowderCoatingProcessItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "powder_coating_process_id", referencedColumnName = "id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_pcpi_process_id_powder_coating_process_id"))
    private PowderCoatingProcess powderCoatingProcess;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_powder_coating_process_items_product_id_product_id"))
    private Product product;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;
    
    @Column(name = "total_bags")
    private Integer totalBags;
    
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "remarks", columnDefinition = "varchar")
    private String remarks;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_powder_coating_process_items_client_id_client_id"))
    private Client client;
}

