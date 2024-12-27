package com.inventory.dao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PriceDao {
    private final EntityManager entityManager;
    
    @Transactional
    public Map<String, Object> getLastPrices(Long productId, Long customerId) {
        String query = """
            SELECT 
                COALESCE(p.unit_price, 0) as last_purchase_price,
                COALESCE(s.unit_price, 0) as last_sale_price
            FROM (
                SELECT unit_price 
                FROM purchase 
                WHERE product_id = :productId 
                AND customer_id = :customerId
                ORDER BY id DESC 
                LIMIT 1
            ) p,
            (
                SELECT s.unit_price 
                FROM (select * from sale s where s.customer_id = :customerId) s
                JOIN (select * from purchase p where p.product_id = :productId and p.customer_id = :customerId) p ON s.purchase_id = p.id
                WHERE p.product_id = :productId 
                AND s.customer_id = :customerId
                ORDER BY s.id DESC 
                LIMIT 1
            ) s
            """;
            
        try {
            Object[] result = (Object[]) entityManager.createNativeQuery(query)
                .setParameter("productId", productId)
                .setParameter("customerId", customerId)
                .getSingleResult();
                
            Map<String, Object> prices = new HashMap<>();
            prices.put("lastPurchasePrice", result[0]);
            prices.put("lastSalePrice", result[1]);
            
            return prices;
        } catch (NoResultException e) {
            e.printStackTrace();
            Map<String, Object> emptyPrices = new HashMap<>();
            emptyPrices.put("lastPurchasePrice", BigDecimal.ZERO);
            emptyPrices.put("lastSalePrice", BigDecimal.ZERO);
            return emptyPrices;
        }
    }
} 