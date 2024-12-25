package com.inventory.repository;

import com.inventory.entity.PowderCoatingProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PowderCoatingProcessRepository extends JpaRepository<PowderCoatingProcess, Long> {
    List<PowderCoatingProcess> findByCustomerId(Long customerId);
    List<PowderCoatingProcess> findByProductId(Long productId);
} 