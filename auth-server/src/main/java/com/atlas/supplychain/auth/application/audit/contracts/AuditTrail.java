package com.atlas.supplychain.auth.application.audit.contracts;

import com.atlas.supplychain.auth.application.audit.dto.AuditEventCommand;

/**
 * Puerto de auditlog. Los servicios de {@code application/} llaman a {@link #registrar(AuditEventCommand)}
 * como ultima sentencia de la operacion de negocio, sin conocer HTTP ni el transporte usado para
 * llegar al broker (Spec 2.4).
 */
public interface AuditTrail {

    void registrar(AuditEventCommand cmd);
}
