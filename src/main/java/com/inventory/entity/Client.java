package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 256)
    private String name;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "phone", length = 32)
    private String phone;
    
    @Column(name = "email", length = 256)
    private String email;
    
    @Column(name = "password", length = 256)
    private String password;

    @Column(name = "fail_password_count")
    private Integer failPasswordCount = 0;

    @Column(name = "lock_time")
    private OffsetDateTime lockTime;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";

    @Column(name = "last_quote_number", columnDefinition = "BIGINT DEFAULT 0")
    private Long lastQuoteNumber = 0L;
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}