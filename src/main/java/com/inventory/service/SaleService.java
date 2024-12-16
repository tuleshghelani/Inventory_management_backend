
package com.inventory.service;

import com.inventory.dto.SaleDto;
import com.inventory.entity.Sale;
import com.inventory.entity.Purchase;
import com.inventory.entity.DailyProfit;
import com.inventory.repository.SaleRepository;
import com.inventory.repository.PurchaseRepository;
import com.inventory.repository.DailyProfitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final DailyProfitRepository dailyProfitRepository;

    @Transactional
    public SaleDto create(SaleDto dto) {
        Purchase purchase = purchaseRepository.findById(dto.getPurchaseId())
            .orElseThrow(() -> new RuntimeException("Purchase not found"));
            
        if (purchase.getRemainingQuantity() < dto.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        Sale sale = new Sale();
        sale.setPurchase(purchase);
        sale.setQuantity(dto.getQuantity());
        sale.setUnitPrice(dto.getUnitPrice());
        sale.setTotalAmount(dto.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        sale.setSaleDate(dto.getSaleDate());
        sale.setInvoiceNumber(dto.getInvoiceNumber());
        sale.setOtherExpenses(dto.getOtherExpenses());
        
        sale = saleRepository.save(sale);
        
        // Update remaining quantity
        purchase.setRemainingQuantity(purchase.getRemainingQuantity() - dto.getQuantity());
        purchaseRepository.save(purchase);
        
        // Calculate and save profit
        calculateAndSaveProfit(sale);
        
        return mapToDto(sale);
    }

    private void calculateAndSaveProfit(Sale sale) {
        BigDecimal purchaseAmount = sale.getPurchase().getUnitPrice()
            .multiply(new BigDecimal(sale.getQuantity()));
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
        dto.setSaleDate(sale.getSaleDate());
        dto.setInvoiceNumber(sale.getInvoiceNumber());
        dto.setOtherExpenses(sale.getOtherExpenses());
        return dto;
    }
}
