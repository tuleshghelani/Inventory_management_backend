package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PowderCoatingProcessItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer remainingQuantity;
    private Integer totalBags;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String remarks;
}

