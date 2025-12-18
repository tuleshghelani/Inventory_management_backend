package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PowderCoatingProcessDto;
import com.inventory.dto.PowderCoatingProcessItemDto;
import com.inventory.entity.PowderCoatingProcess;
import com.inventory.entity.PowderCoatingProcessItem;
import com.inventory.entity.PowderCoatingReturn;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.PowderCoatingProcessRepository;
import com.inventory.repository.PowderCoatingProcessItemRepository;
import com.inventory.repository.PowderCoatingReturnRepository;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.dao.PowderCoatingProcessDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PowderCoatingProcessService {
    private final PowderCoatingProcessRepository processRepository;
    private final PowderCoatingProcessItemRepository processItemRepository;
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
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            process.setClient(currentUser.getClient());
            process.setCreatedBy(currentUser);
            process.setStatus(dto.getStatus() != null ? dto.getStatus() : "A");
            
            // Save process first to get the ID
            processRepository.save(process);
            
            // Create items
            List<PowderCoatingProcessItem> items = new ArrayList<>();
            for (PowderCoatingProcessItemDto itemDto : dto.getItems()) {
                PowderCoatingProcessItem item = new PowderCoatingProcessItem();
                item.setPowderCoatingProcess(process);
                item.setProduct(productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ValidationException("Product not found: " + itemDto.getProductId())));
                item.setQuantity(itemDto.getQuantity());
                item.setRemainingQuantity(itemDto.getQuantity());
                item.setTotalBags(itemDto.getTotalBags());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setTotalAmount(itemDto.getUnitPrice() != null && itemDto.getQuantity() != null 
                    ? itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())) 
                    : BigDecimal.ZERO);
                item.setRemarks(itemDto.getRemarks());
                item.setClient(currentUser.getClient());
                items.add(item);
            }
            
            processItemRepository.saveAll(items);
            
            return ApiResponse.success("Process created successfully");
        } catch (ValidationException e) {
            throw e;
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
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(!process.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to update this powder coating process");
            }
            
            process.setCustomer(customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found")));
            process.setUpdatedAt(OffsetDateTime.now());
            
            // Get existing items
            List<PowderCoatingProcessItem> existingItems = processItemRepository.findByPowderCoatingProcessId(id);
            
            List<Long> updatedItemIds = dto.getItems().stream()
                .filter(item -> item.getId() != null)
                .map(PowderCoatingProcessItemDto::getId)
                .collect(Collectors.toList());
            
            // Delete items that are not in the update request
            List<PowderCoatingProcessItem> itemsToDelete = existingItems.stream()
                .filter(item -> !updatedItemIds.contains(item.getId()))
                .collect(Collectors.toList());
            
            for (PowderCoatingProcessItem item : itemsToDelete) {
                // Check if item has returns
                List<PowderCoatingReturn> returns = returnRepository.findByProcessItemId(item.getId());
                if (!returns.isEmpty()) {
                    throw new ValidationException("Cannot delete item with existing returns. Product: " + item.getProduct().getName());
                }
            }
            processItemRepository.deleteAll(itemsToDelete);
            
            // Update or create items
            for (PowderCoatingProcessItemDto itemDto : dto.getItems()) {
                PowderCoatingProcessItem item;
                
                if (itemDto.getId() != null) {
                    // Update existing item
                    item = existingItems.stream()
                        .filter(i -> i.getId().equals(itemDto.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ValidationException("Item not found: " + itemDto.getId()));
                    
                    // Calculate total returned quantity for this item
                    Integer totalReturnedQuantity = returnRepository.findByProcessItemId(item.getId())
                        .stream()
                        .map(PowderCoatingReturn::getReturnQuantity)
                        .reduce(0, Integer::sum);
                    
                    item.setQuantity(itemDto.getQuantity());
                    item.setRemainingQuantity(itemDto.getQuantity() - totalReturnedQuantity);
                } else {
                    // Create new item
                    item = new PowderCoatingProcessItem();
                    item.setPowderCoatingProcess(process);
                    item.setQuantity(itemDto.getQuantity());
                    item.setRemainingQuantity(itemDto.getQuantity());
                    item.setClient(currentUser.getClient());
                }
                
                item.setProduct(productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ValidationException("Product not found: " + itemDto.getProductId())));
                item.setTotalBags(itemDto.getTotalBags());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setTotalAmount(itemDto.getUnitPrice() != null && itemDto.getQuantity() != null 
                    ? itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())) 
                    : BigDecimal.ZERO);
                item.setRemarks(itemDto.getRemarks());
                
                processItemRepository.save(item);
            }
            
            // Update process status based on all items' remaining quantities
            List<PowderCoatingProcessItem> allItems = processItemRepository.findByPowderCoatingProcessId(id);
            boolean allCompleted = allItems.stream()
                .allMatch(item -> item.getRemainingQuantity() <= 0);
            
            if (allCompleted && !allItems.isEmpty()) {
                process.setStatus("C");
            } else {
                process.setStatus(dto.getStatus() != null ? dto.getStatus() : process.getStatus());
            }
            
            processRepository.save(process);
            return ApiResponse.success("Process updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update process: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            PowderCoatingProcess process = processRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Process not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(!process.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this powder coating process");
            }
            
            // Check if any items have returns
            List<PowderCoatingProcessItem> items = processItemRepository.findByPowderCoatingProcessId(id);
            for (PowderCoatingProcessItem item : items) {
                List<PowderCoatingReturn> returns = returnRepository.findByProcessItemId(item.getId());
                if (!returns.isEmpty()) {
                    throw new ValidationException("Cannot delete process with existing returns");
                }
            }
            
            processRepository.delete(process);
            return ApiResponse.success("Process deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete process: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, Object>> searchProcesses(PowderCoatingProcessDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = processDao.searchProcesses(dto);
            return ApiResponse.success("Processes retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search processes: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> returnQuantity(Long processItemId, Integer returnQuantity, OffsetDateTime returnDate) {
        try {
            if (returnQuantity == null || returnQuantity <= 0) {
                throw new ValidationException("Return quantity must be greater than 0");
            }

            PowderCoatingProcessItem processItem = processItemRepository.findById(processItemId)
                .orElseThrow(() -> new ValidationException("Process item not found"));
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(!processItem.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to return quantity for this process item");
            }

            if (processItem.getRemainingQuantity() < returnQuantity) {
                throw new ValidationException("Return quantity cannot be greater than remaining quantity");
            }

            processItem.setRemainingQuantity(processItem.getRemainingQuantity() - returnQuantity);
            processItemRepository.save(processItem);

            // Create return record with custom date if provided
            PowderCoatingReturn returnRecord = new PowderCoatingReturn();
            returnRecord.setProcess(processItem.getPowderCoatingProcess());
            returnRecord.setProcessItem(processItem);
            returnRecord.setReturnQuantity(returnQuantity);
            returnRecord.setCreatedBy(currentUser);
            returnRecord.setClient(currentUser.getClient());
            if (returnDate != null) {
                returnRecord.setCreatedAt(returnDate);
            }
            returnRepository.save(returnRecord);
            
            // Update process status if all items are completed
            PowderCoatingProcess process = processItem.getPowderCoatingProcess();
            List<PowderCoatingProcessItem> allItems = processItemRepository.findByPowderCoatingProcessId(process.getId());
            boolean allCompleted = allItems.stream()
                .allMatch(item -> item.getRemainingQuantity() <= 0);
            
            if (allCompleted) {
                process.setStatus("C");
                process.setUpdatedAt(OffsetDateTime.now());
                processRepository.save(process);
            }

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
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new ValidationException("At least one item is required");
        }
        
        for (PowderCoatingProcessItemDto item : dto.getItems()) {
            if (item.getProductId() == null) {
                throw new ValidationException("Product is required for all items");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ValidationException("Valid quantity is required for all items");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Valid unit price is required for all items");
            }
        }
    }


    public ApiResponse<?> getProcess(Long id) {
        try {
            if (id == null) {
                throw new ValidationException("Process ID is required");
            }
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Map<String, Object> processDetails = processDao.getProcess(id, currentUser.getClient().getId());
            
            return ApiResponse.success("Process retrieved successfully", processDetails);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve process: " + e.getMessage());
        }
    }

} 