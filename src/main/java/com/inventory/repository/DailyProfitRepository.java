package com.inventory.repository;

import com.inventory.entity.DailyProfit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

public interface DailyProfitRepository extends JpaRepository<DailyProfit, Long> {
    List<DailyProfit> findByProfitDateBetween(OffsetDateTime startDate, OffsetDateTime endDate);
}