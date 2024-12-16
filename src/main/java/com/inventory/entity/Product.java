package com.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "product", uniqueConstraints={
        @UniqueConstraint( name = "uk_product_category_name",  columnNames ={"category_id","name"})
})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 256)
    private String name;
    
    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_product_category_id_category_id"))
    private Category category;
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_product_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(nullable = false, length = 2)
    private String status = "A";
    
    private String description;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal minimumStock;
}