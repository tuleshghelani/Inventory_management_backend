package com.inventory.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Index(name = "idx_purchase_customer_id", columnList = "customer_id"),
    @Index(name = "idx_purchase_transport_id", columnList = "transport_id"),
    @Index(name = "idx_purchase_transport_item_id", columnList = "transport_item_id"),
    @Index(name = "idx_purchase_client_id", columnList = "client_id")
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
    @JoinColumn(name = "quotation_id",
            foreignKey = @ForeignKey(name = "fk_purchase_quotation_id_quotation_id"))
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_item_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_purchase_quotation_item_id_quotation_item_id"))
    private QuotationItem quotationItem;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_purchase_client_id_client_id"))
    private Client client;
}