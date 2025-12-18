package com.inventory.dao;

import com.inventory.dto.PowderCoatingProcessDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;

@Repository
public class PowderCoatingProcessDao {
    @PersistenceContext
    private EntityManager entityManager;
    
    public Map<String, Object> searchProcesses(PowderCoatingProcessDto dto) {
        // First get total count with a separate optimized query
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("""
            SELECT COUNT(DISTINCT pcp.id)
            FROM (SELECT * FROM powder_coating_process WHERE client_id = :clientId) pcp
            LEFT JOIN (SELECT * FROM customer WHERE client_id = :clientId) c ON pcp.customer_id = c.id
            WHERE 1=1
        """);
        params.put("clientId", dto.getClientId());

        appendSearchConditions(countSql, params, dto);
        
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        // Then get paginated data with all required fields
        StringBuilder sql = new StringBuilder();
        
        sql.append("""
            SELECT 
                pcp.id,
                pcp.created_at,
                pcp.status,
                c.id as customer_id,
                c.name as customer_name
            FROM (SELECT * FROM powder_coating_process WHERE client_id = :clientId) pcp
            LEFT JOIN (SELECT * FROM customer WHERE client_id = :clientId) c ON pcp.customer_id = c.id
            WHERE 1=1
        """);

        appendSearchConditions(sql, params, dto);

        sql.append("""
            ORDER BY pcp.%s %s
            LIMIT :pageSize OFFSET :offset
        """.formatted(dto.getSortBy(), dto.getSortDir().toUpperCase()));

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, dto);

        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto);
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, PowderCoatingProcessDto dto) {
        if (dto != null) {
            if (StringUtils.hasText(dto.getSearch())) {
                sql.append("""
                    AND (LOWER(c.name) LIKE LOWER(:search))
                """);
                params.put("search", "%" + dto.getSearch().trim() + "%");
            }

            if (dto.getCustomerId() != null) {
                sql.append(" AND pcp.customer_id = :customerId");
                params.put("customerId", dto.getCustomerId());
            }

            if (StringUtils.hasText(dto.getStatus())) {
                sql.append(" AND pcp.status = :status");
                params.put("status", dto.getStatus().trim());
            }
        }
    }

    private void setQueryParameters(Query query, Map<String, Object> params, PowderCoatingProcessDto dto) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getPerPageRecord());
        query.setParameter("offset", dto.getCurrentPage() * dto.getPerPageRecord());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, PowderCoatingProcessDto dto) {
        List<Map<String, Object>> processes = new ArrayList<>();

        for (Object[] row : results) {
            Long processId = ((Number) row[0]).longValue();
            Map<String, Object> process = new HashMap<>();
            process.put("id", processId);
            process.put("createdAt", row[1]);
            process.put("status", row[2]);
            process.put("customerId", row[3]);
            process.put("customerName", row[4]);
            
            // Fetch items for this process
            List<Map<String, Object>> items = getItemsForProcess(processId, dto.getClientId());
            process.put("items", items);
            
            processes.add(process);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", processes);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getPerPageRecord()));

        return response;
    }
    
    private List<Map<String, Object>> getItemsForProcess(Long processId, Long clientId) {
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
        
        List<Object[]> itemResults = query.getResultList();
        List<Map<String, Object>> items = new ArrayList<>();
        
        for (Object[] row : itemResults) {
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

    public Map<String, Object> getProcess(Long id, Long clientId) {
        String query = """
            SELECT 
                pcp.id,
                pcp.created_at,
                pcp.status,
                pcp.customer_id,
                c.name as customer_name
            FROM (SELECT * FROM powder_coating_process WHERE client_id = :clientId) pcp
            LEFT JOIN (SELECT * FROM customer WHERE client_id = :clientId) c ON c.id = pcp.customer_id
            WHERE pcp.id = :id
        """;

        Query nativeQuery = entityManager.createNativeQuery(query);
        nativeQuery.setParameter("id", id);
        nativeQuery.setParameter("clientId", clientId);
        Object[] result = (Object[]) nativeQuery.getSingleResult();
        
        Map<String, Object> process = new HashMap<>();
        process.put("id", result[0]);
        process.put("createdAt", result[1]);
        process.put("status", result[2]);
        process.put("customerId", result[3]);
        process.put("customerName", result[4]);
        
        // Fetch items for this process
        List<Map<String, Object>> items = getItemsForProcess(id, clientId);
        process.put("items", items);
        
        return process;
    }
} 