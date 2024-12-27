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
            SELECT COUNT(pcp.id)
            FROM powder_coating_process pcp
            LEFT JOIN customer c ON pcp.customer_id = c.id
            LEFT JOIN product p ON pcp.product_id = p.id
            WHERE 1=1
        """);

        appendSearchConditions(countSql, params, dto);
        
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        // Then get paginated data with all required fields
        StringBuilder sql = new StringBuilder();
        
        sql.append("""
            SELECT 
                pcp.id,
                pcp.quantity,
                pcp.remaining_quantity,
                pcp.total_bags,
                pcp.remarks,
                pcp.created_at,
                pcp.status,
                c.name as customer_name,
                p.name as product_name,
                c.id as customer_id,
                p.id as product_id
            FROM powder_coating_process pcp
            LEFT JOIN customer c ON pcp.customer_id = c.id
            LEFT JOIN product p ON pcp.product_id = p.id
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
                    AND (LOWER(c.name) LIKE LOWER(:search)
                    OR LOWER(p.name) LIKE LOWER(:search))
                """);
                params.put("search", "%" + dto.getSearch().trim() + "%");
            }

            if (dto.getCustomerId() != null) {
                sql.append(" AND pcp.customer_id = :customerId");
                params.put("customerId", dto.getCustomerId());
            }

            if (dto.getProductId() != null) {
                sql.append(" AND pcp.product_id = :productId");
                params.put("productId", dto.getProductId());
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
            Map<String, Object> process = new HashMap<>();
            process.put("id", row[0]);
            process.put("quantity", row[1]);
            process.put("remainingQuantity", row[2]);
            process.put("totalBags", row[3]);
            process.put("remarks", row[4]);
            process.put("createdAt", row[5]);
            process.put("status", row[6]);
            process.put("customerId", row[7]);
            process.put("customerName", row[8]);
            process.put("productId", row[9]);
            process.put("productName", row[10]);
            processes.add(process);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", processes);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getPerPageRecord()));

        return response;
    }
} 