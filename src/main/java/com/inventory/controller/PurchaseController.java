package com.inventory.controller;

import com.inventory.dto.PurchaseDto;
import com.inventory.exception.ValidationException;
import com.inventory.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody PurchaseDto request) throws ValidationException {
        return ResponseEntity.ok(purchaseService.create(request));
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(purchaseService.findAll());
    }

    @PostMapping("/searchPurchase")
    public ResponseEntity<?> searchPurchases(@RequestBody PurchaseDto request) {
        return ResponseEntity.ok(purchaseService.searchPurchases(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.delete(id));
    }
}
