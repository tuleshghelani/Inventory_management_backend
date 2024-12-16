package com.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "\"user\"")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(nullable = false)
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
    @JoinColumn(name = "client_id")
    private Client client;
}