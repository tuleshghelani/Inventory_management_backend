package com.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "sale", indexes = {
    @Index(name = "idx_sale_invoice_number", columnList = "invoice_number"),
    @Index(name = "idx_sale_sale_date", columnList = "sale_date"),
    @Index(name = "idx_sale_purchase_id", columnList = "purchase_id"),
})
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_sale_purchase_id_purchase_id"))
    private Purchase purchase;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "sale_date", nullable = false)
    private OffsetDateTime saleDate;
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_sale_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(name = "other_expenses", precision = 10, scale = 2)
    private BigDecimal otherExpenses;
    
    @Column(name = "invoice_number")
    private String invoiceNumber;
    
    @Column(name = "discount", precision = 6, scale = 4)
    private BigDecimal discount;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;
}