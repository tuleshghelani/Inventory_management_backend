package com.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "transport", indexes = {
    @Index(name = "idx_transport_customer_id", columnList = "customer_id"),
    @Index(name = "idx_transport_created_at", columnList = "created_at")
})
public class Transport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_transport_customer_id_customer_id"))
    private Customer customer;
    
    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_transport_created_by_user_master_id"))
    private UserMaster createdBy;
} 