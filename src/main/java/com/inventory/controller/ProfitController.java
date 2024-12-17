
package com.inventory.controller;

import com.inventory.dao.ProfitDao;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/profits")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ProfitController {
    private final ProfitDao profitDao;

    @GetMapping("/daily")
    public ResponseEntity<?> getDailyProfits(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(profitDao.getDailyProfitSummary(
            startDate.atStartOfDay().atOffset(ZoneOffset.UTC),
            endDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
        ));
    }

    @GetMapping("/products")
    public ResponseEntity<?> getProductWiseProfits(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(profitDao.getProductWiseProfitSummary(
            startDate.atStartOfDay().atOffset(ZoneOffset.UTC),
            endDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
        ));
    }
}
