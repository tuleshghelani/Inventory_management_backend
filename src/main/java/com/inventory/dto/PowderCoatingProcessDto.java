package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PowderCoatingProcessDto {
    private Long id;
    private Long customerId;
    private Long productId;
    private Integer quantity;
    private Integer remainingQuantity;
    private String status;
    private String customerName;
    private String productName;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime createdAt;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime returnDate;
    
    // Search and pagination parameters
    private String search;
    private Integer currentPage = 0;
    private Integer perPageRecord = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
    private Integer returnQuantity;
} 