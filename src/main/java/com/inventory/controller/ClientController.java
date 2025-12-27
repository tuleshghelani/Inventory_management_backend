package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.ClientPasswordRequest;
import com.inventory.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ClientController {

    private final ClientService clientService;

    @PostMapping("/password/set")
    public ResponseEntity<ApiResponse<?>> setPassword(@RequestBody ClientPasswordRequest request) {
        clientService.setClientPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password set successfully", null));
    }

    @PostMapping("/password/validate")
    public ResponseEntity<ApiResponse<?>> validatePassword(@RequestBody ClientPasswordRequest request) {
        boolean isValid = clientService.validateClientPassword(request);
        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success("Password is correct", true));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Incorrect password"));
        }
    }
}
