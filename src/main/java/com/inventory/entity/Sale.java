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
@Table(name = "sale", indexes = {
    @Index(name = "idx_sale_invoice_number", columnList = "invoice_number"),
    @Index(name = "idx_sale_sale_date", columnList = "sale_date"),
    @Index(name = "idx_sale_purchase_id", columnList = "purchase_id"),
    @Index(name = "idx_sale_customer_id", columnList = "customer_id"),
    @Index(name = "idx_sale_transport_id", columnList = "transport_id"),
    @Index(name = "idx_sale_transport_item_id", columnList = "transport_item_id"),
    @Index(name = "idx_sale_client_id", columnList = "client_id")
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_sale_customer_id_customer_id"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id",
            foreignKey = @ForeignKey(name = "fk_sale_quotation_id_quotation_id"))
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_item_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_sale_quotation_item_id_quotation_item_id"))
    private QuotationItem quotationItem;

    @Column(name = "quotation_discount_percentage", precision = 5, scale = 2, columnDefinition = "NUMERIC(5, 2) DEFAULT 0.00")
    private BigDecimal quotationDiscountPercentage = BigDecimal.ZERO;

    @Column(name = "quotation_discount_amount", precision = 19, scale = 2, columnDefinition = "NUMERIC(19, 2) DEFAULT 0.00")
    private BigDecimal quotationDiscountAmount = BigDecimal.ZERO;

    @Column(name = "quotation_discount_price", precision = 10, scale = 2)
    private BigDecimal quotationDiscountPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_sale_transport_id_transport_id"))
    private Transport transport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_item_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_sale_transport_item_id_transport_item_id"))
    private TransportItem transportItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_sale_client_id_client_id"))
    private Client client;
}