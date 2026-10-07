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

/** Botón del rail de navegación, dueño la empresa (Spec 1.4). */
@Entity
@Table(name = "menu_category", schema = "auth")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategoryEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_menu_category")
    private Long idMenuCategory;

    @Column(name = "id_company", length = 36, nullable = false)
    private String idCompany;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 150, nullable = false)
    private String name;

    @Column(length = 100)
    private String icon;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean state;
}
