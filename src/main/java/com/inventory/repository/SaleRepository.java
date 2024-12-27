package com.inventory.repository;

import com.inventory.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findBySaleDateBetween(OffsetDateTime startDate, OffsetDateTime endDate);
    List<Sale> findByPurchaseId(Long purchaseId);
    Optional<Sale> findByInvoiceNumber(String invoiceNumber);
    void deleteByTransportId(Long transportId);
    List<Sale> findByTransportId(Long transportId);
}