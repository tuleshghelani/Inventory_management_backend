package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.CombinedPurchaseSaleDto;
import com.inventory.entity.*;
import com.inventory.exception.ValidationException;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CombinedPurchaseSaleService {
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleRepository saleRepository;
    private final DailyProfitRepository dailyProfitRepository;
    private final QuantityTrackingService quantityTrackingService;
    private final UtilityService utilityService;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> createPurchaseAndSale(CombinedPurchaseSaleDto dto) {
        try {
            // Validate input
            if (dto.getProductId() == null) {
                throw new ValidationException("Product is required");
            }
            if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
                throw new ValidationException("Valid quantity is required");
            }
            if (dto.getPurchaseUnitPrice() == null || dto.getPurchaseUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid purchase unit price is required");
            }
            if (dto.getSaleUnitPrice() == null || dto.getSaleUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid sale unit price is required");
            }

            // Get the product
            Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found"));

            // Create purchase
            Purchase purchase = new Purchase();
            purchase.setProduct(product);
            purchase.setCategory(product.getCategory());
            purchase.setQuantity(dto.getQuantity());
            purchase.setUnitPrice(dto.getPurchaseUnitPrice());
            purchase.setTotalAmount(calculateTotalAmount(dto.getPurchaseUnitPrice(), dto.getQuantity(), dto.getPurchaseOtherExpenses()));
            purchase.setPurchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : OffsetDateTime.now());
            purchase.setInvoiceNumber(dto.getPurchaseInvoiceNumber());
            purchase.setOtherExpenses(dto.getPurchaseOtherExpenses());
            purchase.setRemainingQuantity(0);
            purchase.setCreatedBy(utilityService.getCurrentLoggedInUser());
            
            purchase = purchaseRepository.save(purchase);
            quantityTrackingService.updateQuantitiesAfterPurchase(purchase);

            // Create sale
            Sale sale = new Sale();
            sale.setPurchase(purchase);
            sale.setQuantity(dto.getQuantity());
            sale.setUnitPrice(dto.getSaleUnitPrice());
            sale.setTotalAmount(calculateTotalAmount(dto.getSaleUnitPrice(), dto.getQuantity(), dto.getSaleOtherExpenses()));
            sale.setSaleDate(dto.getSaleDate() != null ? dto.getSaleDate() : OffsetDateTime.now());
            sale.setInvoiceNumber(dto.getSaleInvoiceNumber());
            sale.setOtherExpenses(dto.getSaleOtherExpenses());
            sale.setCreatedBy(utilityService.getCurrentLoggedInUser());

            sale = saleRepository.save(sale);
            quantityTrackingService.updateQuantitiesAfterSale(sale);

            // Create daily profit
            DailyProfit dailyProfit = new DailyProfit();
            dailyProfit.setSale(sale);
            
            // Calculate purchase amount based on quantity
            BigDecimal purchaseAmount = purchase.getUnitPrice()
                .multiply(BigDecimal.valueOf(sale.getQuantity()));
            BigDecimal saleAmount = sale.getTotalAmount();
            
            dailyProfit.setPurchaseAmount(purchaseAmount);
            dailyProfit.setSaleAmount(saleAmount);
            dailyProfit.setGrossProfit(saleAmount.subtract(purchaseAmount));
            dailyProfit.setOtherExpenses(sale.getOtherExpenses());
            
            // Calculate net profit
            dailyProfit.setNetProfit(dailyProfit.getGrossProfit().subtract(
                sale.getOtherExpenses() != null ? sale.getOtherExpenses() : BigDecimal.ZERO));
            dailyProfit.setProfitDate(sale.getSaleDate());
            
            dailyProfitRepository.save(dailyProfit);

            return ApiResponse.success("Purchase and sale created successfully");
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new ValidationException("Failed to create purchase and sale: " + e.getMessage());
        }
    }

    private BigDecimal calculateTotalAmount(BigDecimal unitPrice, Integer quantity, BigDecimal otherExpenses) {
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (otherExpenses != null) {
            total = total.add(otherExpenses);
        }
        return total;
    }
} 