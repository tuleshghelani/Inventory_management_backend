
package com.inventory.service;

import com.inventory.dto.PurchaseDto;
import com.inventory.entity.Purchase;
import com.inventory.repository.PurchaseRepository;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;

    @Transactional
    public PurchaseDto create(PurchaseDto dto) {
        Purchase purchase = new Purchase();
        purchase.setProduct(productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found")));
        purchase.setQuantity(dto.getQuantity());
        purchase.setUnitPrice(dto.getUnitPrice());
        purchase.setTotalAmount(dto.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        purchase.setPurchaseDate(dto.getPurchaseDate());
        purchase.setInvoiceNumber(dto.getInvoiceNumber());
        purchase.setOtherExpenses(dto.getOtherExpenses());
        purchase.setRemainingQuantity(dto.getQuantity());
        
        purchase = purchaseRepository.save(purchase);
        return mapToDto(purchase);
    }

    public List<PurchaseDto> findAll() {
        return purchaseRepository.findAll().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
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
