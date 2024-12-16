
package com.inventory.controller;

import com.inventory.dto.SaleDto;
import com.inventory.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {
    private final SaleService saleService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SaleDto request) {
        return ResponseEntity.ok(saleService.create(request));
    }
}
