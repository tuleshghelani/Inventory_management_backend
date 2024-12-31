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
@Table(name = "\"user_master\"", indexes = {
    @Index(name = "idx_user_master_email", columnList = "email"),
    @Index(name = "idx_user_master_client_id", columnList = "client_id")
}, uniqueConstraints={
        @UniqueConstraint( name = "uk_user_master_email",  columnNames ={"email"})
})
public class UserMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @Column(name = "password", nullable = false, length = 256)
    private String password;
    
    @Column(name = "first_name", length = 64)
    private String firstName;
    
    @Column(name = "last_name", length = 64)
    private String lastName;
    
    @Column(name = "jwt_token", length = 256)
    private String jwtToken;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";
    
    @Column(name = "email", length = 64)
    private String email;
    
    @Column(name = "fail_login_count", nullable = false)
    private Integer failLoginCount = 0;
    
    @Column(name = "lock_time")
    private OffsetDateTime lockTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_user_master_client_id_client_id"))
    private Client client;
}