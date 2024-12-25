package com.inventory.service;

import com.inventory.repository.*;
import com.inventory.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuantityTrackingService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final QuantityTrackingRepository quantityTrackingRepository;

    @Transactional
    public void updateQuantitiesAfterPurchase(Purchase purchase) {
        // Update product quantity
        Product product = purchase.getProduct();
        Integer totalProductQuantity = quantityTrackingRepository.getProductTotalQuantity(product.getId());
        product.setRemainingQuantity(totalProductQuantity);
        productRepository.save(product);
        
        // Update category quantity
        Category category = product.getCategory();
        Integer totalCategoryQuantity = quantityTrackingRepository.getCategoryTotalQuantity(category.getId());
        category.setRemainingQuantity(totalCategoryQuantity);
        categoryRepository.save(category);
    }

    @Transactional
    public void updateQuantitiesAfterSale(Sale sale) {
        Product product = sale.getPurchase().getProduct();
        Category category = product.getCategory();
        
        // Update product quantity
        Integer totalProductQuantity = quantityTrackingRepository.getProductTotalQuantity(product.getId());
        product.setRemainingQuantity(totalProductQuantity);
        productRepository.save(product);
        
        // Update category quantity
        Integer totalCategoryQuantity = quantityTrackingRepository.getCategoryTotalQuantity(category.getId());
        category.setRemainingQuantity(totalCategoryQuantity);
        categoryRepository.save(category);
    }
} 