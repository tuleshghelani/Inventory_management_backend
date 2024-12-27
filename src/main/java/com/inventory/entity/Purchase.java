package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "purchase", indexes = {
    @Index(name = "idx_purchase_invoice_number", columnList = "invoice_number"),
    @Index(name = "idx_purchase_purchase_date", columnList = "purchase_date"),
    @Index(name = "idx_purchase_remaining_quantity", columnList = "remaining_quantity"),
    @Index(name = "idx_purchase_category_id", columnList = "category_id"),
    @Index(name = "idx_purchase_product_id", columnList = "product_id"),
})
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_purchase_product_id_product_id"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_purchase_category_id_category_id"))
    private Category category;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "purchase_date")
    private OffsetDateTime purchaseDate;
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(nullable = false, length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_purchase_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(name = "invoice_number")
    private String invoiceNumber;
    
    @Column(name = "other_expenses", precision = 10, scale = 2)
    private BigDecimal otherExpenses;
    
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(name = "discount", precision = 6, scale = 4)
    private BigDecimal discount;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_purchase_customer_id_customer_id"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_purchase_transport_id_transport_id"))
    private Transport transport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_item_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_purchase_transport_item_id_transport_item_id"))
    private TransportItem transportItem;
}