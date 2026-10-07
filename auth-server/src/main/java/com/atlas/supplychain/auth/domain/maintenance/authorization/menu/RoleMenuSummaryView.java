package com.atlas.supplychain.auth.domain.maintenance.authorization.menu;

import java.util.List;

/**
 * Qué módulos del menú termina viendo un rol, y con cuántas pantallas cada uno (Spec 1.2).
 *
 * <p>No es una relación almacenada: los módulos no se asignan a roles. Se deduce de los permisos
 * concedidos, aplicando el mismo criterio de vigencia que {@code User.esVigente} -- operación
 * activa y módulo activo. Existe para que la pantalla de roles pueda responder "¿qué va a ver este
 * rol?" sin pedir los permisos rol por rol, que era una llamada por fila.</p>
 *
 * <p>{@code screenCount} cuenta {@code resource} distintos, no operaciones: conceder
 * {@code EMPRESAS:CREAR} y {@code EMPRESAS:LISTAR} es una sola pantalla en el menú.</p>
 */
public record RoleMenuSummaryView(Long idRole, List<RoleMenuModuleView> modules) {

    public record RoleMenuModuleView(Long idModule, String name, int screenCount) {
    }
}
