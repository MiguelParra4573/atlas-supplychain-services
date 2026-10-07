package com.atlas.supplychain.auth.application.audit.contracts;

import com.atlas.supplychain.auth.application.audit.dto.Actor;

/**
 * Puerto de auditlog (Spec 2.4): resuelve el actor autenticado de la request en curso, sin
 * que la capa {@code application} conozca Spring Security. {@code null} si no hay autenticacion
 * en el contexto actual.
 */
public interface CurrentActor {

    Actor actorActual();
}
