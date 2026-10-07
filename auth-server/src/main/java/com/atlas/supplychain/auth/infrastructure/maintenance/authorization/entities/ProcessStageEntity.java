package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Catálogo maestro de etapas de proceso (Spec 1.3). Global: sin {@code id_company}. */
@Entity
@Table(name = "process_stage", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStageEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_process_stage")
    private Long idProcessStage;

    @Column(name = "id_business_process", nullable = false)
    private Long idBusinessProcess;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 150, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private boolean state;
}
