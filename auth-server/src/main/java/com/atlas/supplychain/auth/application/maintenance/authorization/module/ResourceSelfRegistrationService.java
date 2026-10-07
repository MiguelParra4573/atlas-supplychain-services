package com.atlas.supplychain.auth.application.maintenance.authorization.module;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;

import java.util.List;

/**
 * Auto-registro RBAC de un micro de negocio nuevo. Distinto de {@link OperationService} porque el
 * actor y la forma de la operación son distintos: acá no hay un {@code idCompany} de principal
 * humano -- las operaciones se replican en todas las empresas activas de {@code auth.company} --
 * y "crear" es en realidad "crear o actualizar" (upsert idempotente), no las dos operaciones
 * separadas del CRUD.
 *
 * <p>Desde la Spec 1.2 **no toca {@code Module}**. El servicio que se registra declara qué
 * pantallas protege; bajo qué etiqueta del menú se agrupan es configuración de cada empresa, y
 * nadie más que un administrador de esa empresa la decide.</p>
 */
public interface ResourceSelfRegistrationService {

    /**
     * @param resource   código estable de la pantalla, primera mitad de la autoridad
     *                   {@code RECURSO:OPERACION}.
     * @param operations operaciones tal como las declaró el llamante de servicio, sin
     *                   {@code idCompany} ni {@code idOperation} -- es la plantilla que se replica
     *                   por empresa.
     */
    void registrarOperacionesEnTodasLasEmpresas(String resource, List<Operation> operations);
}
