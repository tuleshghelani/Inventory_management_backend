package com.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "daily_profit")
public class DailyProfit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_daily_profit_sale_id_sale_id"))
    private Sale sale;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal purchaseAmount;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal saleAmount;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal grossProfit;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal otherExpenses;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netProfit;
    
    @Column(nullable = false)
    private OffsetDateTime profitDate;
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}