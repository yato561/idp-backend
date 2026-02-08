package com.idp.backend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false,unique = true)
    private String token;

    @ManyToOne(optional = false)
    private UserEntity user;

    private Instant expiryDate;

    private boolean revoked = false;
}
