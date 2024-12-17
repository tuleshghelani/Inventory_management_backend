package com.inventory.controller;

import com.inventory.dto.SaleDto;
import com.inventory.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class SaleController {
    private final SaleService saleService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody SaleDto request) {
        return ResponseEntity.ok(saleService.create(request));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchSales(@RequestBody SaleDto request) {
        return ResponseEntity.ok(saleService.searchSales(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SaleDto request) {
        return ResponseEntity.ok(saleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.delete(id));
    }
}
