package com.atlas.supplychain.auth.application.audit.dto;

/**
 * Campos que decide el emisor de un evento de auditlog (Spec 2.4, tabla de mapeo evento-&gt;columnas).
 * No incluye {@code ip}/{@code userAgent}/{@code creadoEn} (detalles de transporte/servidor que
 * completa el adaptador de infraestructura) ni {@code dataBefore}/{@code dataAfter} (fuera de alcance
 * de Spec 2.2).
 *
 * @param accionTipo    tipo de accion, ej. {@code LOGIN}, {@code UPDATE}, {@code CREATE}, {@code DELETE}
 * @param accionNombre  nombre puntual de la accion, ej. {@code LOGIN_EXITOSO}, {@code CAMBIO_PASSWORD}
 * @param entidadTipo   tipo de entidad afectada, ej. {@code USUARIO}, {@code ROL}, {@code PERMISO}
 * @param entidadId     identificador (como texto) de la entidad afectada
 * @param modulo        modulo funcional, ej. {@code SEGURIDAD}, {@code ADMINISTRACION}
 * @param severity      {@code LOW}, {@code MEDIUM} o {@code HIGH}
 * @param companyId     empresa del actor (o de la entidad afectada, segun la fila de la tabla de mapeo)
 * @param userId        identificador del actor que ejecuta la accion (no el afectado)
 * @param username      nombre de usuario del actor
 * @param rolPrincipal  rol principal del actor si esta disponible, null en caso contrario
 * @param resumen       frase corta en espanol describiendo el evento
 */
public record AuditEventCommand(
        String accionTipo,
        String accionNombre,
        String entidadTipo,
        String entidadId,
        String modulo,
        String severity,
        String companyId,
        String userId,
        String username,
        String rolPrincipal,
        String resumen
) {
}
