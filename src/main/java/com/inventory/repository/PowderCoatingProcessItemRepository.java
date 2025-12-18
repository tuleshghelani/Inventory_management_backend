package com.inventory.repository;

import com.inventory.entity.PowderCoatingProcessItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PowderCoatingProcessItemRepository extends JpaRepository<PowderCoatingProcessItem, Long> {
    @Query("SELECT i FROM PowderCoatingProcessItem i WHERE i.powderCoatingProcess.id = :processId")
    List<PowderCoatingProcessItem> findByPowderCoatingProcessId(@Param("processId") Long processId);
    
    List<PowderCoatingProcessItem> findByProductId(Long productId);
    
    @Query("SELECT i FROM PowderCoatingProcessItem i LEFT JOIN FETCH i.product WHERE i.powderCoatingProcess.id = :processId")
    List<PowderCoatingProcessItem> findByPowderCoatingProcessIdWithProduct(@Param("processId") Long processId);
}

