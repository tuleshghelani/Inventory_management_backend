package com.inventory.repository;

import com.inventory.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByProductId(Long productId);
    List<Purchase> findByRemainingQuantityGreaterThan(Integer quantity);
}