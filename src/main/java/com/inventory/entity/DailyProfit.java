package com.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "daily_profit",
    indexes = {
        @Index(name = "idx_daily_sale_id", columnList = "sale_id"),
    }
)
public class DailyProfit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_daily_profit_sale_id_sale_id"))
    private Sale sale;
    
    @Column(name = "purchase_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchaseAmount;
    
    @Column(name = "sale_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal saleAmount;
    
    @Column(name = "gross_profit", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossProfit;
    
    @Column(name = "other_expenses", precision = 10, scale = 2)
    private BigDecimal otherExpenses;
    
    @Column(name = "net_profit", nullable = false, precision = 10, scale = 2)
    private BigDecimal netProfit;
    
    @Column(name = "profit_date", nullable = false)
    private OffsetDateTime profitDate;
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}