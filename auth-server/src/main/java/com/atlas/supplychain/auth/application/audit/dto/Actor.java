package com.atlas.supplychain.auth.application.audit.dto;

/**
 * Quien ejecuta la operacion de negocio en curso (Spec 2.4) -- el actor de un evento de
 * auditlog, no la entidad afectada. {@code idUser} y {@code username} coinciden en este
 * dominio: no hay un nombre de usuario separado del identificador de login.
 */
public record Actor(String idUser, String idCompany, String username, String rolPrincipal) {
}
