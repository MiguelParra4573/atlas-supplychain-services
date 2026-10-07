package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de solo lectura (Spec 1.2): qué módulos del menú ve un rol y con cuántas pantallas cada uno.
 * Nunca es body de request -- los módulos no se asignan a un rol, se deducen de sus permisos -- por
 * eso no lleva grupos {@code Crear}/{@code Actualizar}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuSummaryDTO {

    private Long idRole;

    private List<RoleMenuModuleDTO> modules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleMenuModuleDTO {

        private Long idModule;

        private String name;

        /** Pantallas distintas, no operaciones: {@code EMPRESAS:CREAR} + {@code EMPRESAS:LISTAR} es 1. */
        private int screenCount;
    }
}
