package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.SaleDto;
import com.inventory.entity.Sale;
import com.inventory.entity.Purchase;
import com.inventory.entity.DailyProfit;
import com.inventory.exception.ValidationException;
import com.inventory.repository.SaleRepository;
import com.inventory.repository.PurchaseRepository;
import com.inventory.repository.DailyProfitRepository;
import com.inventory.dao.SaleDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final DailyProfitRepository dailyProfitRepository;
    private final SaleDao saleDao;

    @Transactional
    public ApiResponse<?> create(SaleDto dto) {
        try {
            validateSale(dto);
            
            Optional<Sale> existingSale = saleRepository.findByInvoiceNumber(dto.getInvoiceNumber().trim());
            if (existingSale.isPresent()) {
                throw new ValidationException("Invoice number already exists");
            }

            Purchase purchase = purchaseRepository.findById(dto.getPurchaseId())
                .orElseThrow(() -> new ValidationException("Purchase not found"));
                
            if (purchase.getRemainingQuantity() < dto.getQuantity()) {
                throw new ValidationException("Insufficient stock. Available: " + purchase.getRemainingQuantity());
            }

            Sale sale = new Sale();
            sale.setPurchase(purchase);
            sale.setQuantity(dto.getQuantity());
            sale.setUnitPrice(dto.getUnitPrice());
            sale.setTotalAmount(calculateTotalAmount(dto.getUnitPrice(), dto.getQuantity()));
            sale.setSaleDate(dto.getSaleDate());
            sale.setInvoiceNumber(dto.getInvoiceNumber().trim());
            sale.setOtherExpenses(dto.getOtherExpenses());
            
            sale = saleRepository.save(sale);
            
            // Update remaining quantity
            purchase.setRemainingQuantity(purchase.getRemainingQuantity() - dto.getQuantity());
            purchaseRepository.save(purchase);
            
            // Calculate and save profit
            calculateAndSaveProfit(sale);
            
            return ApiResponse.success("Sale created successfully", mapToDto(sale));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create sale: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, Object>> searchSales(SaleDto dto) {
        try {
            Map<String, Object> result = saleDao.searchSales(dto);
            return ApiResponse.success("Sales retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search sales: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(Long id, SaleDto dto) {
        try {
            validateSale(dto);
            
            Sale existingSale = saleRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Sale not found"));
                
            // Check if new invoice number is unique (if changed)
            if (!existingSale.getInvoiceNumber().equals(dto.getInvoiceNumber().trim())) {
                Optional<Sale> saleWithInvoice = saleRepository.findByInvoiceNumber(dto.getInvoiceNumber().trim());
                if (saleWithInvoice.isPresent()) {
                    throw new ValidationException("Invoice number already exists");
                }
            }

            // Restore old purchase quantity before updating
            Purchase oldPurchase = existingSale.getPurchase();
            oldPurchase.setRemainingQuantity(oldPurchase.getRemainingQuantity() + existingSale.getQuantity());
            purchaseRepository.save(oldPurchase);

            // Validate and update with new purchase
            Purchase newPurchase = purchaseRepository.findById(dto.getPurchaseId())
                .orElseThrow(() -> new ValidationException("Purchase not found"));
                
            if (newPurchase.getRemainingQuantity() < dto.getQuantity()) {
                throw new ValidationException("Insufficient stock. Available: " + newPurchase.getRemainingQuantity());
            }

            // Update sale entity
            existingSale.setPurchase(newPurchase);
            existingSale.setQuantity(dto.getQuantity());
            existingSale.setUnitPrice(dto.getUnitPrice());
            existingSale.setTotalAmount(calculateTotalAmount(dto.getUnitPrice(), dto.getQuantity()));
            existingSale.setSaleDate(dto.getSaleDate()!= null ? dto.getSaleDate() : OffsetDateTime.now());
            existingSale.setInvoiceNumber(dto.getInvoiceNumber().trim());
            existingSale.setOtherExpenses(dto.getOtherExpenses());
            existingSale.setUpdatedAt(OffsetDateTime.now());
            
            existingSale = saleRepository.save(existingSale);
            
            // Update new purchase remaining quantity
            newPurchase.setRemainingQuantity(newPurchase.getRemainingQuantity() - dto.getQuantity());
            purchaseRepository.save(newPurchase);
            
            // Update profit records
            updateProfit(existingSale);
            
            return ApiResponse.success("Sale updated successfully", mapToDto(existingSale));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update sale: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> delete(Long id) {
        try {
            Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Sale not found"));

            // First, delete associated profit record
            DailyProfit profit = dailyProfitRepository.findBySaleId(sale.getId())
                .orElseThrow(() -> new ValidationException("Profit record not found"));
            dailyProfitRepository.delete(profit);

            // Restore purchase quantity before deleting sale
            Purchase purchase = sale.getPurchase();
            purchase.setRemainingQuantity(purchase.getRemainingQuantity() + sale.getQuantity());
            purchaseRepository.save(purchase);

            // Finally, delete the sale
            saleRepository.delete(sale);

            return ApiResponse.success("Sale deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete sale: " + e.getMessage());
        }
    }

    private void updateProfit(Sale sale) {
        // Delete existing profit record
        DailyProfit existingProfit = dailyProfitRepository.findBySaleId(sale.getId())
            .orElseThrow(() -> new ValidationException("Profit record not found"));
        dailyProfitRepository.delete(existingProfit);
        
        // Calculate and save new profit
        calculateAndSaveProfit(sale);
    }

    private void validateSale(SaleDto dto) {
        if (dto.getPurchaseId() == null) {
            throw new ValidationException("Purchase is required");
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
        
//        if (dto.getSaleDate() == null) {
//            throw new ValidationException("Sale date is required");
//        }
        
        if (dto.getOtherExpenses() != null && dto.getOtherExpenses().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Other expenses cannot be negative");
        }
    }

    private BigDecimal calculateTotalAmount(BigDecimal unitPrice, Integer quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private void calculateAndSaveProfit(Sale sale) {
        BigDecimal purchaseAmount = sale.getPurchase().getUnitPrice()
            .multiply(BigDecimal.valueOf(sale.getQuantity()));
        BigDecimal saleAmount = sale.getTotalAmount();
        BigDecimal grossProfit = saleAmount.subtract(purchaseAmount);
        BigDecimal netProfit = grossProfit.subtract(sale.getOtherExpenses() != null ? 
            sale.getOtherExpenses() : BigDecimal.ZERO);

        DailyProfit profit = new DailyProfit();
        profit.setSale(sale);
        profit.setPurchaseAmount(purchaseAmount);
        profit.setSaleAmount(saleAmount);
        profit.setGrossProfit(grossProfit);
        profit.setOtherExpenses(sale.getOtherExpenses());
        profit.setNetProfit(netProfit);
        profit.setProfitDate(sale.getSaleDate());
        
        dailyProfitRepository.save(profit);
    }

    private SaleDto mapToDto(Sale sale) {
        SaleDto dto = new SaleDto();
        dto.setId(sale.getId());
        dto.setPurchaseId(sale.getPurchase().getId());
        dto.setQuantity(sale.getQuantity());
        dto.setUnitPrice(sale.getUnitPrice());
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setSaleDate(sale.getSaleDate());
        dto.setInvoiceNumber(sale.getInvoiceNumber());
        dto.setOtherExpenses(sale.getOtherExpenses());
        return dto;
    }
}
