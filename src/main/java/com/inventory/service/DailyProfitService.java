package com.inventory.service;

import com.inventory.dao.DailyProfitDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.DailyProfitDto;
import com.inventory.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DailyProfitService {
    private final DailyProfitDao dailyProfitDao;

    public ApiResponse<Map<String, Object>> searchDailyProfits(DailyProfitDto dto) {
        try {
            Map<String, Object> result = dailyProfitDao.searchDailyProfits(dto);
            return ApiResponse.success("Daily profits retrieved successfully", result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to search daily profits: " + e.getMessage());
        }
    }
}
