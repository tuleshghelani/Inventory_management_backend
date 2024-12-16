package com.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "\"user_master\"", uniqueConstraints={
        @UniqueConstraint( name = "uk_user_master_email",  columnNames ={"email"})
})
public class UserMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @Column(nullable = false, length = 256)
    private String password;
    
    @Column(length = 64)
    private String firstName;
    
    @Column(length = 64)
    private String lastName;
    
    @Column(length = 256)
    private String jwtToken;
    
    @Column(nullable = false, length = 2)
    private String status = "A";
    
    @Column(length = 64)
    private String email;
    
    @Column(nullable = false)
    private Integer failLoginCount = 0;
    
    private OffsetDateTime lockTime;
    
    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_user_master_client_id_client_id"))
    private Client client;
}