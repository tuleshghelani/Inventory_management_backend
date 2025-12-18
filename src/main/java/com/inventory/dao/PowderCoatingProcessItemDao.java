package com.inventory.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class PowderCoatingProcessItemDao {
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<Map<String, Object>> getItemsByProcessId(Long processId, Long clientId) {
        String sql = """
            SELECT 
                pcpi.id,
                pcpi.quantity,
                pcpi.remaining_quantity,
                pcpi.total_bags,
                pcpi.unit_price,
                pcpi.total_amount,
                pcpi.remarks,
                p.id as product_id,
                p.name as product_name
            FROM (SELECT * FROM powder_coating_process_items WHERE client_id = :clientId) pcpi
            LEFT JOIN (SELECT * FROM product WHERE client_id = :clientId) p ON pcpi.product_id = p.id
            WHERE pcpi.powder_coating_process_id = :processId
            ORDER BY pcpi.id
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("processId", processId);
        query.setParameter("clientId", clientId);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> items = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("quantity", row[1]);
            item.put("remainingQuantity", row[2]);
            item.put("totalBags", row[3]);
            item.put("unitPrice", row[4]);
            item.put("totalAmount", row[5]);
            item.put("remarks", row[6]);
            item.put("productId", row[7]);
            item.put("productName", row[8]);
            items.add(item);
        }
        
        return items;
    }
}

