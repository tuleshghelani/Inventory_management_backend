package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.CategoryDto;
import com.inventory.entity.Category;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public ApiResponse<?> create(CategoryDto dto) {
        validateCategory(dto);
        
        try {
            Category category = new Category();
            category.setName(dto.getName().trim());
            category.setStatus(dto.getStatus().trim());
            
            categoryRepository.save(category);
            return ApiResponse.success("Category created successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to create category");
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, CategoryDto dto) {
        validateCategory(dto);
        
        try {
            Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Category not found"));
                
            category.setName(dto.getName().trim());
            category.setStatus(dto.getStatus().trim());
            
            categoryRepository.save(category);
            return ApiResponse.success("Category updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update category");
        }
    }

    public ApiResponse<List<CategoryDto>> findAll() {
        try {
            List<CategoryDto> categories = categoryRepository.findByStatus("A").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
            return ApiResponse.success("Categories retrieved successfully", categories);
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve categories");
        }
    }

    private void validateCategory(CategoryDto dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new ValidationException("Category name is required");
        }
        if (!StringUtils.hasText(dto.getStatus())) {
            throw new ValidationException("Category status is required");
        }
        if (dto.getStatus().trim().length() != 1 || !dto.getStatus().trim().matches("[AI]")) {
            throw new ValidationException("Category status must be either 'A' (Active) or 'I' (Inactive)");
        }
    }

    private CategoryDto mapToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setStatus(category.getStatus());
        return dto;
    }
}
