package com.inventory.controller;

import com.inventory.dto.CombinedPurchaseSaleDto;
import com.inventory.service.CombinedPurchaseSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/combined-purchase-sale")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class CombinedPurchaseSaleController {
    private final CombinedPurchaseSaleService combinedPurchaseSaleService;

    @PostMapping("/create")
    public ResponseEntity<?> createPurchaseAndSale(@RequestBody CombinedPurchaseSaleDto request) {
        return ResponseEntity.ok(combinedPurchaseSaleService.createPurchaseAndSale(request));
    }
} 