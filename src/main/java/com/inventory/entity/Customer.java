package com.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 256)
    private String name;
    
    @Column(length = 15)
    private String gst;
    
    @Column(length = 512)
    private String address;
    
    @Column(length = 15)
    private String mobile;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal remainingPaymentAmount;
    
    private OffsetDateTime nextActionDate;
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_customer_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(nullable = false, length = 2)
    private String status = "A";
    
    @Column(length = 256)
    private String email;
    
    @Column(length = 1000)
    private String remarks;
} 