package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransportDto {
    private Long id;
    private Long customerId;
    private List<BagDto> bags;
    private String customerName;
    private OffsetDateTime createdAt;
    
    // Search parameters
    private String search;
    private Integer currentPage = 0;
    private Integer perPageRecord = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime startDate;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime endDate;
    
    @Data
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BagDto {
        private Long id;
        private BigDecimal weight;
        private List<BagItemDto> items;
    }
    
    @Data
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BagItemDto {
        private Long productId;
        private Integer quantity;
        private String remarks;
    }
} 