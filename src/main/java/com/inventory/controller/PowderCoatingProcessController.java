package com.inventory.controller;

import com.inventory.dto.PowderCoatingProcessDto;
import com.inventory.service.PowderCoatingProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/powder-coating")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class PowderCoatingProcessController {
    private final PowderCoatingProcessService processService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PowderCoatingProcessDto request) {
        return ResponseEntity.ok(processService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PowderCoatingProcessDto request) {
        return ResponseEntity.ok(processService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(processService.delete(id));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchProcesses(@RequestBody PowderCoatingProcessDto request) {
        return ResponseEntity.ok(processService.searchProcesses(request));
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnQuantity(@RequestBody PowderCoatingProcessDto request) {
        return ResponseEntity.ok(processService.returnQuantity(request.getId(), request.getReturnQuantity()));
    }
} 