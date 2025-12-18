package com.inventory.repository;

import com.inventory.entity.PowderCoatingReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PowderCoatingReturnRepository extends JpaRepository<PowderCoatingReturn, Long> {
    @Query("SELECT r FROM PowderCoatingReturn r WHERE r.processItem.id = :processItemId")
    List<PowderCoatingReturn> findByProcessItemId(@Param("processItemId") Long processItemId);
    
    @Query("SELECT r FROM PowderCoatingReturn r WHERE r.processItem.powderCoatingProcess.id = :processId")
    List<PowderCoatingReturn> findByProcessId(@Param("processId") Long processId);
} 