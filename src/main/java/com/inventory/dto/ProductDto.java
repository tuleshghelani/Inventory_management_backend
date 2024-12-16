package com.inventory.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private Long categoryId;
    private String description;
    private BigDecimal minimumStock;
    private String status;
}