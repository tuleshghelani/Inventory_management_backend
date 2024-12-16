package com.inventory.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class SaleDto {
    private Long id;
    private Long purchaseId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private OffsetDateTime saleDate;
    private String invoiceNumber;
    private BigDecimal otherExpenses;
}