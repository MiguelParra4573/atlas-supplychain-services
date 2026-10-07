package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_password_history",
        indexes = {
                @Index(name="idx_user_password_history_id_user_id_company_created_at", columnList = "id_user,id_company,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordHistoryEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="id_user", nullable=false)
    private Long idUser;

    @Column(name="id_company", nullable=false)
    private String idCompany;

    @Column(name="password_hash", nullable=false, length=255)
    private String passwordHash;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;
}

