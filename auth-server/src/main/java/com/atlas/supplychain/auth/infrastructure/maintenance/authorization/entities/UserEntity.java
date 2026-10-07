package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "users", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user", nullable = false)
    private Long idUser;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 100, nullable = false)
    private String lastName;

    @Column(length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "lock_until")
    private Instant lockUntil;

    @Column(name = "password_expires_at")
    private Instant passwordExpiresAt;

    @Column(nullable = false)
    private boolean state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", nullable = false)
    private RoleEntity roleEntity;

}