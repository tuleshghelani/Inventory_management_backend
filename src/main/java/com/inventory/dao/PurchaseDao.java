package com.inventory.dao;

import com.inventory.dto.PurchaseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;

@Repository
public class PurchaseDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Page<Map<String, Object>> searchPurchases(PurchaseDto dto) {
        try {
            StringBuilder countQuery = new StringBuilder();
            StringBuilder actualQuery = new StringBuilder();
            StringBuilder nativeQuery = new StringBuilder();
            Map<String, Object> params = new HashMap<>();

            actualQuery.append("""
                SELECT 
                    p.id, p.quantity, p.unit_price, p.total_amount, 
                    p.purchase_date, p.invoice_number, p.other_expenses,
                    p.remaining_quantity, pr.name as product_name,
                    pr.id as product_id, c.name as category_name,
                    c.id as category_id
                """);

            countQuery.append("SELECT COUNT(*) ");

            nativeQuery.append("""
                FROM purchase p 
                INNER JOIN product pr ON p.product_id = pr.id
                INNER JOIN category c ON pr.category_id = c.id 
                WHERE 1=1
                """);

            appendSearchConditions(nativeQuery, params, dto);

            countQuery.append(nativeQuery);
            nativeQuery.append(" ORDER BY p.id DESC LIMIT :perPageRecord OFFSET :offset");
            actualQuery.append(nativeQuery);

            Pageable pageable = PageRequest.of(dto.getCurrentPage(), dto.getPerPageRecord());
            Query countQueryObj = entityManager.createNativeQuery(countQuery.toString());
            Query query = entityManager.createNativeQuery(actualQuery.toString());

            setQueryParameters(query, countQueryObj, params, dto);

            Long totalCount = ((Number) countQueryObj.getSingleResult()).longValue();
            List<Object[]> results = query.getResultList();
            List<Map<String, Object>> purchases = transformResults(results);

            return new PageImpl<>(purchases, pageable, totalCount);
        } catch (Exception e) {
            e.printStackTrace();
            return new PageImpl<>(new ArrayList<>(), 
                PageRequest.of(dto.getCurrentPage(), dto.getPerPageRecord()), 0L);
        }
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, PurchaseDto dto) {
        if (!Objects.isNull(dto.getSearch()) && dto.getSearch().trim().length() > 0) {
            sql.append("""
                AND (LOWER(pr.name) LIKE :search 
                OR LOWER(p.invoice_number) LIKE :search)
                """);
            params.put("search", "%" + dto.getSearch().toLowerCase() + "%");
        }
        
        if (!Objects.isNull(dto.getProductId()) && dto.getProductId() > 0) {
            sql.append("AND product_id = :productId");
            params.put("productId", dto.getProductId());
        }
    }

    private void setQueryParameters(Query query, Query countQuery, Map<String, Object> params, PurchaseDto dto) {
        params.forEach((key, value) -> {
            query.setParameter(key, value);
            countQuery.setParameter(key, value);
        });

        query.setParameter("perPageRecord", dto.getPerPageRecord());
        query.setParameter("offset", (long) dto.getCurrentPage() * dto.getPerPageRecord());
    }

    private List<Map<String, Object>> transformResults(List<Object[]> results) {
        List<Map<String, Object>> purchases = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> purchase = new HashMap<>();
            purchase.put("id", row[0]);
            purchase.put("quantity", row[1]);
            purchase.put("unitPrice", row[2]);
            purchase.put("totalAmount", row[3]);
            purchase.put("purchaseDate", row[4]);
            purchase.put("invoiceNumber", row[5]);
            purchase.put("otherExpenses", row[6]);
            purchase.put("remainingQuantity", row[7]);
            purchase.put("productName", row[8]);
            purchase.put("productId", row[9]);
            purchase.put("categoryName", row[10]);
            purchase.put("categoryId", row[11]);
            purchases.add(purchase);
        }
        return purchases;
    }
}