package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.BusinessProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.CompanyProcess;

import java.util.List;

/**
 * Catálogo maestro de procesos y su activación por empresa (Spec 1.0).
 *
 * <p>Las operaciones del catálogo no llevan {@code idCompany}: es global, lo administra el
 * proveedor del ERP. Las de activación sí, porque son por empresa.</p>
 */
public interface BusinessProcessRepository {

    void crear(BusinessProcess process);

    void actualizar(BusinessProcess process) throws EntityNotFoundException;

    BusinessProcess obtenerPorId(Long idBusinessProcess) throws EntityNotFoundException;

    void eliminar(Long idBusinessProcess) throws EntityNotFoundException;

    List<BusinessProcess> listar();

    boolean existsByCode(String code);

    /** Activaciones de una empresa, incluidas las que están en {@code active = false}. */
    List<CompanyProcess> listarPorEmpresa(String idCompany);

    /**
     * Alta o cambio de estado de la activación. Hay una sola fila por empresa y proceso. Valida que
     * el proceso exista dentro de la misma transacción que el upsert.
     */
    void activar(String idCompany, Long idBusinessProcess, boolean active) throws EntityNotFoundException;
}
