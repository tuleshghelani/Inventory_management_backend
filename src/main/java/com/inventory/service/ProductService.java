
package com.inventory.service;

import com.inventory.dto.ProductDto;
import com.inventory.entity.Product;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductDto create(ProductDto dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setCategory(categoryRepository.findById(dto.getCategoryId())
            .orElseThrow(() -> new RuntimeException("Category not found")));
        product.setDescription(dto.getDescription());
        product.setMinimumStock(dto.getMinimumStock());
        product.setStatus(dto.getStatus());
        
        product = productRepository.save(product);
        return mapToDto(product);
    }

    @Transactional
    public ProductDto update(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
            
        product.setName(dto.getName());
        product.setCategory(categoryRepository.findById(dto.getCategoryId())
            .orElseThrow(() -> new RuntimeException("Category not found")));
        product.setDescription(dto.getDescription());
        product.setMinimumStock(dto.getMinimumStock());
        product.setStatus(dto.getStatus());
        
        product = productRepository.save(product);
        return mapToDto(product);
    }

    public List<ProductDto> findAll() {
        return productRepository.findByStatus("A").stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private ProductDto mapToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCategoryId(product.getCategory().getId());
        dto.setDescription(product.getDescription());
        dto.setMinimumStock(product.getMinimumStock());
        dto.setStatus(product.getStatus());
        return dto;
    }
}
