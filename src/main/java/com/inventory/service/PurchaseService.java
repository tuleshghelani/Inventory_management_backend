package com.inventory.service;

import com.inventory.dao.PurchaseDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.PurchaseDto;
import com.inventory.entity.*;
import com.inventory.exception.ValidationException;
import com.inventory.repository.PurchaseRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final UtilityService utilityService;
    private final PurchaseDao purchaseDao;
    private final QuantityTrackingService quantityTrackingService;
    private final SaleRepository saleRepository;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(PurchaseDto dto) throws ValidationException {
        try {
            validatePurchase(dto);
            
            // Check if invoice number is unique
            Optional<Purchase> existingPurchase = purchaseRepository.findByInvoiceNumber(dto.getInvoiceNumber().trim());
            if (existingPurchase.isPresent()) {
                throw new ValidationException("Invoice number already exists", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Get the product first
            Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found", HttpStatus.UNPROCESSABLE_ENTITY));

            Purchase purchase = new Purchase();
            purchase.setProduct(product);
            // Set the category from the product
            purchase.setCategory(product.getCategory());
            
            purchase.setQuantity(dto.getQuantity());
            purchase.setUnitPrice(dto.getUnitPrice());
            purchase.setTotalAmount(calculateTotalAmount(dto.getUnitPrice(), dto.getQuantity(), dto.getOtherExpenses()));
            
            try {
                purchase.setPurchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : OffsetDateTime.now());
            } catch (Exception dateEx) {
                throw new ValidationException("Invalid date format. Expected format: dd-MM-yyyy HH:mm:ss. Error: " + dateEx.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
            }
            
            purchase.setInvoiceNumber(dto.getInvoiceNumber().trim());
            purchase.setOtherExpenses(dto.getOtherExpenses());
            purchase.setRemainingQuantity(dto.getQuantity());
            UserMaster currentLoggedInUser = utilityService.getCurrentLoggedInUser();
            purchase.setCreatedBy(currentLoggedInUser);
                
            purchase = purchaseRepository.save(purchase);
            quantityTrackingService.updateQuantitiesAfterPurchase(purchase);
            return ApiResponse.success("Purchase created successfully", mapToDto(purchase));
        } catch (ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Failed to create purchase. Error: " + e.getMessage());
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

    public Page<Map<String, Object>> searchPurchases(PurchaseDto dto) {
        try {
            return purchaseDao.searchPurchases(dto);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to search purchases");
        }
    }

    @Transactional(rollbackFor = Exception.class)
        public ApiResponse<?> delete(Long id) {
            try {
                Purchase purchase = purchaseRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Purchase not found"));

                // Check if there are any associated sales
                List<Sale> existingSales = saleRepository.findByPurchaseId(purchase.getId());
                if (!existingSales.isEmpty()) {
                    throw new ValidationException("Cannot delete purchase. Please remove associated sales first.");
                }

                // Get the product before deleting the purchase
                Product product = purchase.getProduct();
                Category category = product.getCategory();

                // Delete the purchase
                purchaseRepository.delete(purchase);

                // Update quantities
                quantityTrackingService.updateQuantitiesAfterPurchase(purchase);

                return ApiResponse.success("Purchase deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to delete purchase: " + e.getMessage());
        }

    }
}
