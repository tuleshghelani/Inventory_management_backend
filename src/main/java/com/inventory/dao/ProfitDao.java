package com.inventory.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class ProfitDao {
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<Map<String, Object>> getDailyProfitSummary(OffsetDateTime startDate, OffsetDateTime endDate) {
        String sql = """
            SELECT 
                DATE(dp.profit_date) as date,
                SUM(dp.gross_profit) as gross_profit,
                SUM(dp.other_expenses) as total_expenses,
                SUM(dp.net_profit) as net_profit
            FROM daily_profit dp
            WHERE dp.profit_date BETWEEN :startDate AND :endDate
            GROUP BY DATE(dp.profit_date)
            ORDER BY DATE(dp.profit_date)
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        
        return query.getResultList();
    }
    
    public List<Map<String, Object>> getProductWiseProfitSummary(OffsetDateTime startDate, OffsetDateTime endDate) {
        String sql = """
            SELECT 
                p.name as product_name,
                SUM(dp.gross_profit) as gross_profit,
                SUM(dp.other_expenses) as total_expenses,
                SUM(dp.net_profit) as net_profit
            FROM daily_profit dp
            JOIN sale s ON dp.sale_id = s.id
            JOIN purchase pu ON s.purchase_id = pu.id
            JOIN product p ON pu.product_id = p.id
            WHERE dp.profit_date BETWEEN :startDate AND :endDate
            GROUP BY p.id, p.name
            ORDER BY SUM(dp.net_profit) DESC
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        
        return query.getResultList();
    }
}