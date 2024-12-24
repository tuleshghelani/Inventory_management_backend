package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PowderCoatingReturnDto;
import com.inventory.dao.PowderCoatingReturnDao;
import com.inventory.exception.ValidationException;
import com.inventory.repository.PowderCoatingReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PowderCoatingReturnService {
    private final PowderCoatingReturnDao returnDao;
    private final PowderCoatingReturnRepository returnRepository;

    public ApiResponse<Map<String, Object>> searchReturns(PowderCoatingReturnDto dto) {
        try {
            Map<String, Object> result = returnDao.searchReturns(dto);
            return ApiResponse.success("Return history retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search return history: " + e.getMessage());
        }
    }
} 