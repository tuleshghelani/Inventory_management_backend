package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PurchaseDto;
import com.inventory.entity.Purchase;
import com.inventory.exception.ValidationException;
import com.inventory.repository.PurchaseRepository;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ApiResponse<?> create(PurchaseDto dto) {
        try {
            validatePurchase(dto);
            
            // Check if invoice number is unique
            Optional<Purchase> existingPurchase = purchaseRepository.findByInvoiceNumber(dto.getInvoiceNumber().trim());
            if (existingPurchase.isPresent()) {
                throw new ValidationException("Invoice number already exists");
            }

            Purchase purchase = new Purchase();
            purchase.setProduct(productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found")));
            
            purchase.setQuantity(dto.getQuantity());
            purchase.setUnitPrice(dto.getUnitPrice());
            purchase.setTotalAmount(calculateTotalAmount(dto.getUnitPrice(), dto.getQuantity(), dto.getOtherExpenses()));
            purchase.setPurchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : OffsetDateTime.now());
            purchase.setInvoiceNumber(dto.getInvoiceNumber().trim());
            purchase.setOtherExpenses(dto.getOtherExpenses());
            purchase.setRemainingQuantity(dto.getQuantity());
            
            purchase = purchaseRepository.save(purchase);
            return ApiResponse.success("Purchase created successfully", mapToDto(purchase));
        } catch (ValidationException e) {
            e.printStackTrace(); 
            throw e;
        } catch (Exception e) {
            e.printStackTrace(); 
            throw new ValidationException("Failed to create purchase: " + e.getMessage());
        }
    }

    public ApiResponse<List<PurchaseDto>> findAll() {
        try {
            List<PurchaseDto> purchases = purchaseRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
            return ApiResponse.success("Purchases retrieved successfully", purchases);
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve purchases");
        }
    }

    private void validatePurchase(PurchaseDto dto) {
        if (dto.getProductId() == null) {
            throw new ValidationException("Product is required");
        }
        
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new ValidationException("Valid quantity is required");
        }
        
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Valid unit price is required");
        }
        
        if (!StringUtils.hasText(dto.getInvoiceNumber())) {
            throw new ValidationException("Invoice number is required");
        }
        
        if (dto.getOtherExpenses() != null && dto.getOtherExpenses().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Other expenses cannot be negative");
        }
    }

    private BigDecimal calculateTotalAmount(BigDecimal unitPrice, Integer quantity, BigDecimal otherExpenses) {
        BigDecimal baseAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return otherExpenses != null ? baseAmount.add(otherExpenses) : baseAmount;
    }

    private PurchaseDto mapToDto(Purchase purchase) {
        PurchaseDto dto = new PurchaseDto();
        dto.setId(purchase.getId());
        dto.setProductId(purchase.getProduct().getId());
        dto.setQuantity(purchase.getQuantity());
        dto.setUnitPrice(purchase.getUnitPrice());
        dto.setTotalAmount(purchase.getTotalAmount());
        dto.setPurchaseDate(purchase.getPurchaseDate());
        dto.setInvoiceNumber(purchase.getInvoiceNumber());
        dto.setOtherExpenses(purchase.getOtherExpenses());
        return dto;
    }
}
