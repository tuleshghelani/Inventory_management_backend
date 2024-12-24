package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PowderCoatingProcessDto;
import com.inventory.entity.PowderCoatingProcess;
import com.inventory.entity.PowderCoatingReturn;
import com.inventory.exception.ValidationException;
import com.inventory.repository.PowderCoatingProcessRepository;
import com.inventory.repository.PowderCoatingReturnRepository;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.dao.PowderCoatingProcessDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class PowderCoatingProcessService {
    private final PowderCoatingProcessRepository processRepository;
    private final PowderCoatingReturnRepository returnRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PowderCoatingProcessDao processDao;
    private final UtilityService utilityService;

    @Transactional
    public ApiResponse<?> create(PowderCoatingProcessDto dto) {
        try {
            validateProcess(dto);
            
            PowderCoatingProcess process = new PowderCoatingProcess();
            process.setCustomer(customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found")));
            process.setProduct(productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found")));
            
            process.setQuantity(dto.getQuantity());
            process.setRemainingQuantity(dto.getQuantity());
            process.setCreatedBy(utilityService.getCurrentLoggedInUser());
            process.setStatus(dto.getStatus() != null ? dto.getStatus() : "A");
            
            process = processRepository.save(process);
            return ApiResponse.success("Process created successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to create process: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, PowderCoatingProcessDto dto) {
        try {
            validateProcess(dto);
            
            PowderCoatingProcess process = processRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Process not found"));
            
            process.setCustomer(customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found")));
            process.setProduct(productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found")));
            
            process.setQuantity(dto.getQuantity());
            process.setRemainingQuantity(dto.getQuantity());
            process.setUpdatedAt(OffsetDateTime.now());
            process.setStatus(dto.getStatus());
            
            processRepository.save(process);
            return ApiResponse.success("Process updated successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to update process: " + e.getMessage());
        }
    }

    public ApiResponse<?> delete(Long id) {
        try {
            PowderCoatingProcess process = processRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Process not found"));
            
            processRepository.delete(process);
            return ApiResponse.success("Process deleted successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to delete process: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, Object>> searchProcesses(PowderCoatingProcessDto dto) {
        try {
            Map<String, Object> result = processDao.searchProcesses(dto);
            return ApiResponse.success("Processes retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search processes: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> returnQuantity(Long processId, Integer returnQuantity, OffsetDateTime returnDate) {
        try {
            if (returnQuantity <= 0) {
                throw new ValidationException("Return quantity must be greater than 0");
            }

            PowderCoatingProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new ValidationException("Process not found"));

            if (process.getRemainingQuantity() < returnQuantity) {
                throw new ValidationException("Return quantity cannot be greater than remaining quantity");
            }

            process.setRemainingQuantity(process.getRemainingQuantity() - returnQuantity);

            if (process.getRemainingQuantity() == 0) {
                process.setStatus("C");
            }
            
            process.setUpdatedAt(OffsetDateTime.now());
            processRepository.save(process);

            // Create return record with custom date if provided
            PowderCoatingReturn returnRecord = new PowderCoatingReturn();
            returnRecord.setProcess(process);
            returnRecord.setReturnQuantity(returnQuantity);
            returnRecord.setCreatedBy(utilityService.getCurrentLoggedInUser());
            if (returnDate != null) {
                returnRecord.setCreatedAt(returnDate);
            }
            returnRepository.save(returnRecord);

            return ApiResponse.success("Quantity returned successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to return quantity: " + e.getMessage());
        }
    }

    private void validateProcess(PowderCoatingProcessDto dto) {
        if (dto.getCustomerId() == null) {
            throw new ValidationException("Customer is required");
        }
        if (dto.getProductId() == null) {
            throw new ValidationException("Product is required");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new ValidationException("Valid quantity is required");
        }
    }

    private PowderCoatingProcessDto mapToDto(PowderCoatingProcess process) {
        PowderCoatingProcessDto dto = new PowderCoatingProcessDto();
        dto.setId(process.getId());
        dto.setCustomerId(process.getCustomer().getId());
        dto.setProductId(process.getProduct().getId());
        dto.setQuantity(process.getQuantity());
        dto.setRemainingQuantity(process.getRemainingQuantity());
        dto.setStatus(process.getStatus());
        dto.setCustomerName(process.getCustomer().getName());
        dto.setProductName(process.getProduct().getName());
        dto.setCreatedAt(process.getCreatedAt());
        return dto;
    }
} 