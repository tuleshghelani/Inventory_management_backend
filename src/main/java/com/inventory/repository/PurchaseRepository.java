package com.inventory.repository;

import com.inventory.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByProductId(Long productId);
    List<Purchase> findByRemainingQuantityGreaterThan(Integer quantity);
    Optional<Purchase> findByInvoiceNumber(String invoiceNumber);
    void deleteByTransportId(Long transportId);
    List<Purchase> findByClientId(Long clientId);
}