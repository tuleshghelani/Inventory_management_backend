package com.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dao.TransportDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.TransportDto;
import com.inventory.entity.*;
import com.inventory.exception.ValidationException;
import com.inventory.repository.*;
import com.inventory.service.UtilityService;
import com.inventory.util.DiscountCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;

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
    private final PurchaseRepository purchaseRepository;
    private final SaleRepository saleRepository;
    private final DailyProfitRepository dailyProfitRepository;
    private final TransportItemRepository transportItemRepository;

    @Transactional(rollbackFor = Exception.class)
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

                
                // Get all sales for this transport and delete their daily profits
                List<Sale> sales = saleRepository.findByTransportId(transport.getId());
                for (Sale sale : sales) {
                    dailyProfitRepository.deleteBySaleId(sale.getId());
                }
                saleRepository.deleteByTransportId(transport.getId());
                purchaseRepository.deleteByTransportId(transport.getId());
                transportBagRepository.deleteByTransportId(transport.getId());
                
            } else {
                transport = new Transport();
                transport.setCustomer(customerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new ValidationException("Customer not found")));
                transport.setCreatedBy(utilityService.getCurrentLoggedInUser());
            }

            BigDecimal totalWeight = dto.getBags().stream()
                .map(TransportDto.BagDto::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            transport.setTotalWeight(totalWeight);
            transport.setTotalBags(dto.getBags().size());
            
            transport = transportRepository.save(transport);

            saveBagsAndCreateEntries(dto.getBags(), transport);
            
            String message = dto.getId() != null ? "Transport updated successfully" : "Transport created successfully";
            return ApiResponse.success(message);
        } catch (Exception e) {
            e.getMessage();
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
                
                // Validate purchase fields
                if (item.getPurchaseUnitPrice() == null || item.getPurchaseUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidationException("Valid purchase unit price is required");
                }
                if (item.getPurchaseDiscount() != null && 
                    item.getPurchaseDiscount().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new ValidationException("Purchase discount cannot be greater than 100%");
                }
                
                // Validate sale fields
                if (item.getSaleUnitPrice() == null || item.getSaleUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidationException("Valid sale unit price is required");
                }
                if (item.getSaleDiscount() != null && 
                    item.getSaleDiscount().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new ValidationException("Sale discount cannot be greater than 100%");
                }
            }
        }
    }

//    @Transactional
//    public ApiResponse<?> update(Long id, TransportDto dto) {
//        try {
//            validateTransport(dto);
//
//            Transport transport = transportRepository.findById(id)
//                .orElseThrow(() -> new ValidationException("Transport not found"));
//
//            transport.setCustomer(customerRepository.findById(dto.getCustomerId())
//                .orElseThrow(() -> new ValidationException("Customer not found")));
//
//            // Delete existing bags
//            transportBagRepository.deleteByTransportId(id);
//
//            // Create new bags
//            saveBags(dto.getBags(), transport);
//
//            return ApiResponse.success("Transport updated successfully");
//        } catch (Exception e) {
//            throw new ValidationException("Failed to update transport: " + e.getMessage());
//        }
//    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            Transport transport = transportRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Transport not found"));
            
            // Get all sales for this transport and delete their daily profits
            List<Sale> sales = saleRepository.findByTransportId(id);
            for (Sale sale : sales) {
                dailyProfitRepository.deleteBySaleId(sale.getId());
            }
            
            // Delete associated records
            transportBagRepository.deleteByTransportId(id);
            saleRepository.deleteByTransportId(id);
            purchaseRepository.deleteByTransportId(id);
            
            // Delete transport
            transportRepository.delete(transport);
            
            return ApiResponse.success("Transport deleted successfully");
        } catch (Exception e) {
            e.getMessage();
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

    private void saveBagsAndCreateEntries(List<TransportDto.BagDto> bagDtos, Transport transport) {
        int batchSize = 50;
        int count = 0;
        
        for (TransportDto.BagDto bagDto : bagDtos) {
            // Save bag
            TransportBag bag = new TransportBag();
            bag.setTransport(transport);
            bag.setWeight(bagDto.getWeight());
            bag = transportBagRepository.save(bag);
            
            // Save items
            for (TransportDto.BagItemDto itemDto : bagDto.getItems()) {
                TransportItem item = new TransportItem();
                item.setTransport(transport);
                item.setTransportBag(bag);
                item.setProduct(productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ValidationException("Product not found")));
                item.setQuantity(itemDto.getQuantity());
                item.setRemarks(itemDto.getRemarks());
                item = transportItemRepository.save(item);
                
                // Create purchase and sale entries
                createPurchaseAndSaleEntries(itemDto, transport, item);
                
                if (++count % batchSize == 0) {
                    transportItemRepository.flush();
                }
            }
        }
    }

    private void createPurchaseAndSaleEntries(TransportDto.BagItemDto item, Transport transport, TransportItem transportItem) {
        // Create purchase
        Purchase purchase = new Purchase();
        Optional<Product> product = productRepository.findById(item.getProductId());
        if(product.isEmpty()) {
            throw new ValidationException("Product not found");
        }
        purchase.setProduct(product.get());
        purchase.setQuantity(item.getQuantity());
        purchase.setUnitPrice(item.getPurchaseUnitPrice());
        
        // Calculate purchase amounts with discount
        calculatePurchaseAmounts(purchase, item);
        
        purchase.setPurchaseDate(OffsetDateTime.now());
        purchase.setTransport(transport);
        purchase.setRemainingQuantity(0); // Since we're creating sale immediately
        purchase.setCreatedBy(transport.getCreatedBy());
        purchase.setOtherExpenses(BigDecimal.ZERO);
        purchase.setCategory(product.get().getCategory());
        purchase.setTransportItem(transportItem);
        purchase = purchaseRepository.save(purchase);
        
        // Create sale
        Sale sale = new Sale();
        sale.setPurchase(purchase);
        sale.setQuantity(item.getQuantity());
        sale.setUnitPrice(item.getSaleUnitPrice());
        
        // Calculate sale amounts with discount
        calculateSaleAmounts(sale, item);
        
        sale.setSaleDate(OffsetDateTime.now());
        sale.setTransport(transport);
        sale.setCreatedBy(transport.getCreatedBy());
        sale.setOtherExpenses(BigDecimal.ZERO);
        sale.setTransportItem(transportItem);
        sale = saleRepository.save(sale);
        
        // Create daily profit
        createDailyProfit(purchase, sale);
    }

    private void calculatePurchaseAmounts(Purchase purchase, TransportDto.BagItemDto item) {
        // Calculate base amount
        BigDecimal baseAmount = item.getPurchaseUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        
        // Calculate discount amount
        BigDecimal discountAmount = item.getPurchaseDiscountAmount();
        
        // Calculate discounted price
        BigDecimal discountPrice = DiscountCalculator.calculateDiscountedPrice(baseAmount, discountAmount);
        
        // Set all calculated values
        purchase.setDiscount(item.getPurchaseDiscount());
        purchase.setDiscountAmount(discountAmount);
        purchase.setDiscountPrice(discountPrice);
        purchase.setTotalAmount(discountPrice); // Since we don't have other expenses in transport
    }

    private void calculateSaleAmounts(Sale sale, TransportDto.BagItemDto item) {
        // Calculate base amount
        BigDecimal baseAmount = item.getSaleUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        
        // Calculate discount amount
        BigDecimal discountAmount = item.getSaleDiscountAmount();
        
        // Calculate discounted price
        BigDecimal discountPrice = DiscountCalculator.calculateDiscountedPrice(baseAmount, discountAmount);
        
        // Set all calculated values
        sale.setDiscount(item.getSaleDiscount());
        sale.setDiscountAmount(discountAmount);
        sale.setDiscountPrice(discountPrice);
        sale.setTotalAmount(discountPrice); // Since we don't have other expenses in transport
    }

    private void createDailyProfit(Purchase purchase, Sale sale) {
        DailyProfit dailyProfit = new DailyProfit();
        dailyProfit.setSale(sale);
        
        BigDecimal purchaseAmount = purchase.getDiscountPrice();
        BigDecimal saleAmount = sale.getDiscountPrice();
        
        dailyProfit.setPurchaseAmount(purchaseAmount);
        dailyProfit.setSaleAmount(saleAmount);
        dailyProfit.setGrossProfit(saleAmount.subtract(purchaseAmount));
        dailyProfit.setOtherExpenses(sale.getOtherExpenses());
        dailyProfit.setNetProfit(dailyProfit.getGrossProfit().subtract(
            sale.getOtherExpenses() != null ? sale.getOtherExpenses() : BigDecimal.ZERO));
        dailyProfit.setProfitDate(sale.getSaleDate());
        
        dailyProfitRepository.save(dailyProfit);
    }

    public ApiResponse<?> getTransportDetail(TransportDto dto) {
        try {
            if (dto.getId() == null) {
                throw new ValidationException("Transport ID is required");
            }
            
            Map<String, Object> result = transportDao.getTransportDetail(dto.getId());
            return ApiResponse.success("Transport detail retrieved successfully", result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to get transport detail: " + e.getMessage());
        }
    }
} 