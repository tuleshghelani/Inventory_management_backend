package com.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dao.TransportDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.TransportDto;
import com.inventory.entity.Transport;
import com.inventory.entity.TransportBag;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.TransportBagRepository;
import com.inventory.repository.TransportRepository;
import com.inventory.service.UtilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransportService {
    private final TransportRepository transportRepository;
    private final TransportBagRepository transportBagRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final TransportDao transportDao;
    private final UtilityService utilityService;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public ApiResponse<?> create(TransportDto dto) {
        try {
            validateTransport(dto);
            
            Transport transport;
            if (dto.getId() != null) {
                // Update existing transport
                transport = transportRepository.findById(dto.getId())
                    .orElseThrow(() -> new ValidationException("Transport not found"));
                    
                transport.setCustomer(customerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new ValidationException("Customer not found")));

                transportBagRepository.deleteByTransportId(transport.getId());
                
            } else {
                transport = new Transport();
                transport.setCustomer(customerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new ValidationException("Customer not found")));
                transport.setCreatedBy(utilityService.getCurrentLoggedInUser());
            }
            
            transport = transportRepository.save(transport);
            saveBags(dto.getBags(), transport);
            String message = dto.getId() != null ? "Transport updated successfully" : "Transport created successfully";
            return ApiResponse.success(message);
        } catch (Exception e) {
            throw new ValidationException("Failed to " + (dto.getId() != null ? "update" : "create") + 
                " transport: " + e.getMessage());
        }
    }
    
    private void validateTransport(TransportDto dto) {
        if (dto.getCustomerId() == null) {
            throw new ValidationException("Customer is required");
        }
        if (dto.getBags() == null || dto.getBags().isEmpty()) {
            throw new ValidationException("At least one bag is required");
        }
        
        for (TransportDto.BagDto bag : dto.getBags()) {
            if (bag.getWeight() == null || bag.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid weight is required for each bag");
            }
            if (bag.getItems() == null || bag.getItems().isEmpty()) {
                throw new ValidationException("At least one item is required in each bag");
            }
            
            for (TransportDto.BagItemDto item : bag.getItems()) {
                if (item.getProductId() == null) {
                    throw new ValidationException("Product is required for each item");
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new ValidationException("Valid quantity is required for each item");
                }
                // Verify product exists
                productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ValidationException("Product not found: " + item.getProductId()));
            }
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, TransportDto dto) {
        try {
            validateTransport(dto);
            
            Transport transport = transportRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Transport not found"));
            
            transport.setCustomer(customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found")));
            
            // Delete existing bags
            transportBagRepository.deleteByTransportId(id);
            
            // Create new bags
            saveBags(dto.getBags(), transport);
            
            return ApiResponse.success("Transport updated successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to update transport: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            Transport transport = transportRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Transport not found"));
            
            // Delete associated bags first
            transportBagRepository.deleteByTransportId(id);
            
            // Delete transport
            transportRepository.delete(transport);
            
            return ApiResponse.success("Transport deleted successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to delete transport: " + e.getMessage());
        }
    }

    public ApiResponse<?> searchTransports(TransportDto dto) {
        try {
            validateSearchRequest(dto);
            Map<String, Object> result = transportDao.searchTransports(dto);
            return ApiResponse.success("Transports retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search transports: " + e.getMessage());
        }
    }

    private void validateSearchRequest(TransportDto dto) {
        if (dto == null) {
            throw new ValidationException("Request cannot be null");
        }
        if (dto.getCurrentPage() == null) {
            dto.setCurrentPage(0);
        }
        if (dto.getPerPageRecord() == null) {
            dto.setPerPageRecord(10);
        }
    }

    private List<Map<String, Object>> convertItemsToJsonFormat(List<TransportDto.BagItemDto> items) {
        return items.stream()
            .map(item -> {
                Map<String, Object> itemMap = new HashMap<>(4); // Initialize with expected size
                itemMap.put("productId", String.valueOf(item.getProductId())); // Convert to String
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("remarks", item.getRemarks());
                return itemMap;
            })
            .collect(Collectors.toList());
    }

    private void saveBags(List<TransportDto.BagDto> bagDtos, Transport transport) {
        int batchSize = 50;
        int count = 0;
        
        for (TransportDto.BagDto bagDto : bagDtos) {
            TransportBag bag = new TransportBag();
            bag.setTransport(transport);
            bag.setWeight(bagDto.getWeight());
            bag.setItems(convertItemsToJsonFormat(bagDto.getItems()));
            transportBagRepository.save(bag);
            
            if (++count % batchSize == 0) {
                transportBagRepository.flush();
            }
        }
    }

    public ApiResponse<?> getTransportDetail(TransportDto dto) {
        try {
            if (dto.getId() == null) {
                throw new ValidationException("Transport ID is required");
            }
            
            Map<String, Object> result = transportDao.getTransportDetail(dto.getId());
            return ApiResponse.success("Transport detail retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to get transport detail: " + e.getMessage());
        }
    }
} 