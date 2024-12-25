package com.inventory.dao;

import com.inventory.dto.SaleDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SaleDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> searchSales(SaleDto saleDto) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("""
            SELECT COUNT(s.id)
            FROM sale s
            JOIN purchase p ON s.purchase_id = p.id
            JOIN product pr ON p.product_id = pr.id
            WHERE 1=1
        """);

        appendSearchConditions(countSql, params, saleDto);
        
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                s.id,
                s.quantity,
                s.unit_price,
                s.total_amount,
                s.sale_date,
                s.invoice_number,
                s.other_expenses,
                p.id as purchase_id,
                pr.id as product_id,
                pr.name as product_name,
                c.id as category_id,
                c.name as category_name
            FROM sale s
            JOIN purchase p ON s.purchase_id = p.id
            left JOIN product pr ON p.product_id = pr.id
            left JOIN category c ON p.category_id = c.id
            WHERE 1=1
        """);

        appendSearchConditions(sql, params, saleDto);
        sql.append(" ORDER BY s.id DESC LIMIT :pageSize OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, saleDto);

        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, saleDto.getPerPageRecord());
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, SaleDto saleDto) {
        if (StringUtils.hasText(saleDto.getSearch())) {
            sql.append("""
                AND (LOWER(pr.name) LIKE LOWER(:search)
                OR LOWER(s.invoice_number) LIKE LOWER(:search))
            """);
            params.put("search", "%" + saleDto.getSearch().trim() + "%");
        }

        if (saleDto.getStartDate() != null) {
            sql.append(" AND s.sale_date >= :startDate");
            params.put("startDate", saleDto.getStartDate());
        }

        if (saleDto.getEndDate() != null) {
            sql.append(" AND s.sale_date <= :endDate");
            params.put("endDate", saleDto.getEndDate());
        }

        if (saleDto.getProductId() != null) {
            sql.append(" AND pr.id = :productId");
            params.put("productId", saleDto.getProductId());
        }
    }

    private void setQueryParameters(Query query, Map<String, Object> params, SaleDto saleDto) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", saleDto.getPerPageRecord());
        query.setParameter("offset", (long) saleDto.getCurrentPage() * saleDto.getPerPageRecord());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, int pageSize) {
        List<Map<String, Object>> sales = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> sale = new HashMap<>();
            sale.put("id", row[0]);
            sale.put("quantity", row[1]);
            sale.put("unitPrice", row[2]);
            sale.put("totalAmount", row[3]);
            sale.put("saleDate", row[4]);
            sale.put("invoiceNumber", row[5]);
            sale.put("otherExpenses", row[6]);
            sale.put("purchaseId", row[7]);
            sale.put("productId", row[8]);
            sale.put("productName", row[9]);
            sale.put("categoryId", row[10]);
            sale.put("categoryName", row[11]);
            sales.add(sale);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", sales);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / pageSize));

        return response;
    }
} 