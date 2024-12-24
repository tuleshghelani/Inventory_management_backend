package com.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "powder_coating_return", indexes = {
    @Index(name = "idx_pcr_process_id", columnList = "process_id"),
    @Index(name = "idx_pcr_created_at", columnList = "created_at")
})
public class PowderCoatingReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_pcr_process_id"))
    private PowderCoatingProcess process;
    
    @Column(name = "return_quantity", nullable = false)
    private Integer returnQuantity;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private UserMaster createdBy;
} 