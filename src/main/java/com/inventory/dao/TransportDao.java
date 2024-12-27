package com.inventory.dao;

import com.inventory.dto.SaleDto;
import com.inventory.dto.TransportDto;
import com.inventory.exception.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TransportDao {
    private final EntityManager entityManager;
    
    public Map<String, Object> searchTransports(TransportDto dto) {
        StringBuilder countQuery = new StringBuilder("SELECT COUNT(t) FROM Transport t JOIN t.customer c WHERE 1=1");
        StringBuilder dataQuery = new StringBuilder("""
            SELECT 
                t.id, 
                t.createdAt, 
                c.name as customerName, 
                c.id as customerId
            FROM Transport t
            JOIN t.customer c
            WHERE 1=1
        """);
        
        Map<String, Object> params = new HashMap<>();
        buildWhereClause(countQuery, dataQuery, params, dto);
        
        // Add sorting
        dataQuery.append(" ORDER BY t.").append(dto.getSortBy()).append(" ").append(dto.getSortDir());
        
        // Execute count query
        Query query = entityManager.createQuery(countQuery.toString());
        setParameters(query, params);
        long totalRecords = (long) query.getSingleResult();
        
        // Execute data query with pagination
        query = entityManager.createQuery(dataQuery.toString());
        setParameters(query, params);
        query.setFirstResult(dto.getCurrentPage() * dto.getPerPageRecord());
        query.setMaxResults(dto.getPerPageRecord());
        
        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto);
    }
    
    private void buildWhereClause(StringBuilder countQuery, StringBuilder dataQuery, 
            Map<String, Object> params, TransportDto dto) {
        if (StringUtils.hasText(dto.getSearch())) {
            String search = "%" + dto.getSearch().toLowerCase() + "%";
            countQuery.append(" AND LOWER(c.name) LIKE :search");
            dataQuery.append(" AND LOWER(c.name) LIKE :search");
            params.put("search", search);
        }
        
        if (dto.getStartDate() != null) {
            countQuery.append(" AND t.createdAt >= :startDate");
            dataQuery.append(" AND t.createdAt >= :startDate");
            params.put("startDate", dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            countQuery.append(" AND t.createdAt <= :endDate");
            dataQuery.append(" AND t.createdAt <= :endDate");
            params.put("endDate", dto.getEndDate());
        }
    }

    private void setParameters(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, TransportDto dto) {
        List<Map<String, Object>> transports = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> transport = new HashMap<>();
            transport.put("id", row[0]);
            transport.put("createdAt", row[1]);
            transport.put("customerName", row[2]);
            transport.put("customerId", row[3]);
            transports.add(transport);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", transports);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getPerPageRecord()));
        
        return response;
    }

    public Map<String, Object> getTransportDetail(Long transportId) {
        String query = """
            SELECT 
                t.id,
                t.created_at as createdAt,
                c.id as customerId,
                c.name as customerName
            FROM transport t
            JOIN customer c ON t.customer_id = c.id
            WHERE t.id = :transportId
        """;
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        nativeQuery.setParameter("transportId", transportId);
        
        Object[] result = (Object[]) nativeQuery.getSingleResult();
        
        if (result == null) {
            throw new ValidationException("Transport not found");
        }
        
        Map<String, Object> transport = new HashMap<>();
        transport.put("id", result[0]);
        transport.put("createdAt", result[1]);
        transport.put("customerId", result[2]);
        transport.put("customerName", result[3]);
        
        // Get bags for this transport
        String bagQuery = """
            SELECT 
                b.id,
                b.weight,
                ti.id as item_id,
                ti.product_id,
                p.name as product_name,
                ti.quantity,
                ti.remarks
            FROM transport_bag b
            LEFT JOIN transport_items ti ON ti.transport_bag_id = b.id
            LEFT JOIN product p ON p.id = ti.product_id
            WHERE b.transport_id = :transportId
            ORDER BY b.id, ti.id
        """;
        
        Query bagNativeQuery = entityManager.createNativeQuery(bagQuery);
        bagNativeQuery.setParameter("transportId", transportId);
        
        List<Object[]> bagResults = bagNativeQuery.getResultList();
        Map<Long, Map<String, Object>> bagsMap = new HashMap<>();
        
        for (Object[] bagRow : bagResults) {
            Long bagId = ((Number) bagRow[0]).longValue();
            
            Map<String, Object> bag = bagsMap.computeIfAbsent(bagId, k -> {
                Map<String, Object> newBag = new HashMap<>();
                newBag.put("id", bagRow[0]);
                newBag.put("weight", bagRow[1]);
                newBag.put("items", new ArrayList<>());
                return newBag;
            });
            
            if (bagRow[2] != null) {  // If there are items
                Map<String, Object> item = new HashMap<>();
                item.put("id", bagRow[2]);
                item.put("productId", bagRow[3]);
                item.put("productName", bagRow[4]);
                item.put("quantity", bagRow[5]);
                item.put("remarks", bagRow[6]);
                ((List<Map<String, Object>>) bag.get("items")).add(item);
            }
        }
        
        transport.put("bags", new ArrayList<>(bagsMap.values()));
        return transport;
    }
} 