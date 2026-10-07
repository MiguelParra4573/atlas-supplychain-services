package com.atlas.supplychain.auth.application.messaging.contracts;

import com.atlas.supplychain.auth.application.messaging.dto.OutboxEventCommand;

/**
 * Puerto del patrón Outbox (Spec 3.4). Los servicios de {@code application/} llaman a
 * {@link #append(OutboxEventCommand)} en la misma operación de negocio que cambia el dato que el
 * evento anuncia -- el adaptador de infraestructura garantiza que ambas escrituras (la del
 * agregado y la de este evento) caen en la misma transacción.
 */
public interface OutboxRepository {

    void append(OutboxEventCommand command);
}
