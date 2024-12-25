package com.inventory.controller;

import com.inventory.dto.TransportDto;
import com.inventory.service.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transport")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class TransportController {
    private final TransportService transportService;
    
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody TransportDto request) {
        return ResponseEntity.ok(transportService.create(request));
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody TransportDto request) {
        return ResponseEntity.ok(transportService.create(request));
    }
    
    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody TransportDto request) {
        return ResponseEntity.ok(transportService.searchTransports(request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(transportService.delete(id));
    }
    
    @PostMapping("/detail")
    public ResponseEntity<?> getTransportDetail(@RequestBody TransportDto request) {
        return ResponseEntity.ok(transportService.getTransportDetail(request));
    }
} 