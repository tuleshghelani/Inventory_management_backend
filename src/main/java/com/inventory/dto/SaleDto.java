package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaleDto {
    private Long id;
    private Long purchaseId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private OffsetDateTime saleDate;
    private String invoiceNumber;
    private BigDecimal otherExpenses;
}