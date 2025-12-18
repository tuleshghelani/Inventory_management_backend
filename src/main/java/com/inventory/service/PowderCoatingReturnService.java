package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PowderCoatingReturnDto;
import com.inventory.dao.PowderCoatingReturnDao;
import com.inventory.entity.PowderCoatingProcess;
import com.inventory.entity.PowderCoatingProcessItem;
import com.inventory.entity.PowderCoatingReturn;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.PowderCoatingReturnRepository;
import com.inventory.repository.PowderCoatingProcessRepository;
import com.inventory.repository.PowderCoatingProcessItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PowderCoatingReturnService {
    private final PowderCoatingReturnDao returnDao;
    private final PowderCoatingReturnRepository returnRepository;
    private final PowderCoatingProcessRepository processRepository;
    private final PowderCoatingProcessItemRepository processItemRepository;
    private final UtilityService utilityService;

    public ApiResponse<Map<String, Object>> searchReturns(PowderCoatingReturnDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            validateSearchRequest(dto);
            Map<String, Object> result = returnDao.searchReturns(dto);
            return ApiResponse.success("Return history retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search return history: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            if (id == null) {
                throw new ValidationException("ID is required for deletion");
            }

            PowderCoatingReturn returnRecord = returnRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Return record not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(!returnRecord.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this return record");
            }
            
            // Restore the remaining quantity to the specific item
            PowderCoatingProcessItem processItem = returnRecord.getProcessItem();
            processItem.setRemainingQuantity(processItem.getRemainingQuantity() + returnRecord.getReturnQuantity());
            processItemRepository.save(processItem);
            
            // Update process status if needed
            PowderCoatingProcess process = processItem.getPowderCoatingProcess();
            List<PowderCoatingProcessItem> allItems = processItemRepository.findByPowderCoatingProcessId(process.getId());
            boolean anyIncomplete = allItems.stream()
                .anyMatch(item -> item.getRemainingQuantity() > 0);
            
            if (anyIncomplete && "C".equals(process.getStatus())) {
                process.setStatus("A");
                processRepository.save(process);
            }
            
            returnRepository.delete(returnRecord);
            return ApiResponse.success("Return record deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete return record: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, PowderCoatingReturnDto dto) {
        try {
            validateUpdateRequest(id, dto);
            
            PowderCoatingReturn returnRecord = returnRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Return record not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(!returnRecord.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to update this return record");
            }

            PowderCoatingProcessItem processItem = returnRecord.getProcessItem();

            // Restore old quantity
            processItem.setRemainingQuantity(processItem.getRemainingQuantity() + returnRecord.getReturnQuantity());

            // Validate new quantity
            if (processItem.getRemainingQuantity() < dto.getReturnQuantity()) {
                throw new ValidationException("Return quantity cannot be greater than remaining quantity");
            }
            
            // Apply new quantity
            processItem.setRemainingQuantity(processItem.getRemainingQuantity() - dto.getReturnQuantity());
            processItemRepository.save(processItem);
            
            // Update process status
            PowderCoatingProcess process = processItem.getPowderCoatingProcess();
            List<PowderCoatingProcessItem> allItems = processItemRepository.findByPowderCoatingProcessId(process.getId());
            boolean allCompleted = allItems.stream()
                .allMatch(item -> item.getRemainingQuantity() <= 0);
            
            if (allCompleted) {
                process.setStatus("C");
            } else {
                process.setStatus("A");
            }
            processRepository.save(process);
            
            returnRecord.setReturnQuantity(dto.getReturnQuantity());
            returnRepository.save(returnRecord);
            
            return ApiResponse.success("Return record updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update return record: " + e.getMessage());
        }
    }

    private void validateSearchRequest(PowderCoatingReturnDto dto) {
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

    private void validateUpdateRequest(Long id, PowderCoatingReturnDto dto) {
        if (dto == null) {
            throw new ValidationException("Request cannot be null");
        }
        if (id == null) {
            throw new ValidationException("ID is required for update");
        }
        if (dto.getReturnQuantity() == null || dto.getReturnQuantity() <= 0) {
            throw new ValidationException("Valid return quantity is required");
        }
    }

    public ApiResponse<?> getByProcessId(Long processId) {
        try {
            if (processId == null) {
                throw new ValidationException("Process ID is required");
            }
            PowderCoatingProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new ValidationException("Process not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(!process.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to view this return record");
            }

            List<Map<String, Object>> returns = returnRepository.findByProcessId(processId)
                .stream()
                .map(returnRecord -> {
                    Map<String, Object> returnMap = new HashMap<>();
                    returnMap.put("id", returnRecord.getId());
                    returnMap.put("returnQuantity", returnRecord.getReturnQuantity());
                    returnMap.put("createdAt", returnRecord.getCreatedAt());
                    returnMap.put("processId", returnRecord.getProcess() != null ? returnRecord.getProcess().getId() : null);
                    returnMap.put("processItemId", returnRecord.getProcessItem().getId());
                    returnMap.put("productName", returnRecord.getProcessItem().getProduct().getName());
                    return returnMap;
                })
                .collect(Collectors.toList());

            return ApiResponse.success("Return records retrieved successfully", returns);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve return records: " + e.getMessage());
        }
    }
    
    public ApiResponse<?> getByProcessItemId(Long processItemId) {
        try {
            if (processItemId == null) {
                throw new ValidationException("Process Item ID is required");
            }
            PowderCoatingProcessItem processItem = processItemRepository.findById(processItemId)
                .orElseThrow(() -> new ValidationException("Process item not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(!processItem.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to view this return record");
            }

            List<Map<String, Object>> returns = returnRepository.findByProcessItemId(processItemId)
                .stream()
                .map(returnRecord -> {
                    Map<String, Object> returnMap = new HashMap<>();
                    returnMap.put("id", returnRecord.getId());
                    returnMap.put("returnQuantity", returnRecord.getReturnQuantity());
                    returnMap.put("createdAt", returnRecord.getCreatedAt());
                    returnMap.put("processId", returnRecord.getProcess() != null ? returnRecord.getProcess().getId() : null);
                    returnMap.put("processItemId", returnRecord.getProcessItem().getId());
                    returnMap.put("productName", returnRecord.getProcessItem().getProduct().getName());
                    return returnMap;
                })
                .collect(Collectors.toList());

            return ApiResponse.success("Return records retrieved successfully", returns);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve return records: " + e.getMessage());
        }
    }
} 