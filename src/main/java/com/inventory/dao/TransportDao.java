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
            LEFT JOIN user_master u ON t.created_by = u.id
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
                b.items
            FROM transport_bag b
            WHERE b.transport_id = :transportId
            order by b.id 
        """;
        
        Query bagNativeQuery = entityManager.createNativeQuery(bagQuery);
        bagNativeQuery.setParameter("transportId", transportId);
        
        List<Object[]> bagResults = bagNativeQuery.getResultList();
        List<Map<String, Object>> bags = new ArrayList<>();
        
        for (Object[] bagRow : bagResults) {
            Map<String, Object> bag = new HashMap<>();
            bag.put("id", bagRow[0]);
            bag.put("weight", bagRow[1]);
            bag.put("items", bagRow[2]); // JSONB column will be automatically converted
            bags.add(bag);
        }
        
        transport.put("bags", bags);
        return transport;
    }
} 