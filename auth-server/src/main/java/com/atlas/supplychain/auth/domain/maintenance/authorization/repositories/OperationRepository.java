package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;


import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.screen.ScreenSummary;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;
import java.util.Optional;

public interface OperationRepository {

    void crearOperacion(Operation operation);

    void actualizarOperacion(Operation operation) throws EntityNotFoundException;

    Operation obtenerOperacionPorId(String idCompany, Long idOperation) throws EntityNotFoundException;

    void eliminarOperacion(String idCompany, Long idOperation) throws EntityNotFoundException;

    List<Operation> listarOperacionesPorModulo(String idCompany, Long idModule);

    /** Las operaciones activas de un {@code resource} (Spec 1.3): siempre las 5 CRUD de la misma
     * pantalla, para moverlas juntas de módulo en una sola llamada. */
    List<Operation> listarPorRecurso(String idCompany, String resource);

    /**
     * Buscador de pantallas (Spec 1.3): página de {@code resource} distintos de la empresa, con
     * sus módulos actuales ya resueltos -- la unidad de trabajo de la pantalla de asignación, no
     * la operación individual. {@code resourceLike} filtra por substring case-insensitive;
     * {@code null}/vacío no filtra. {@code onlyUnassigned}: solo pantallas sin ningún módulo --
     * la auditoría "qué falta asignar" tiene que resolverla la consulta, no compararse fila por
     * fila con cientos/miles de pantallas.
     */
    PagedResult<ScreenSummary> buscarPantallas(String idCompany, String resourceLike, boolean onlyUnassigned,
                                                PageQuery pageQuery);

    /**
     * Operaciones activas de la empresa que no cuelgan de ningún módulo (Spec 1.2) -- la bandeja
     * de entrada del administrador: es donde aterriza todo lo que el auto-registro da de alta
     * hasta que alguien decide bajo qué etiqueta del menú va.
     */
    List<Operation> listarOperacionesSinModulo(String idCompany);

    PagedResult<Operation> listarOperacionesPaginadas(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);

    /**
     * ¿Ya existe esta autoridad en esta empresa? {@code (resource, tag)} es la identidad de una
     * operación desde la Spec 1.2 -- respaldado por {@code uk_operations_company_resource_tag}
     * (changelog 025).
     *
     * <p>Antes era {@code existsByTag(tag)}, sin empresa ni módulo: cualquier empresa que ya
     * tuviera un {@code CREAR} bloqueaba el alta de un {@code CREAR} en otra. El filtro de tenant
     * no es alcance agregado, es lo que hace que la consulta responda la pregunta correcta.</p>
     */
    boolean existsByResourceAndTag(String idCompany, String resource, String tag);

    /**
     * Auto-registro de operaciones (POST /modules/self-register): busca por
     * {@code (idCompany, resource, tag)} -- mismo trío que protege
     * {@code uk_operations_company_resource_tag} -- sin filtrar por {@code state}, porque el
     * upsert necesita encontrar también una operación desactivada para reactivarla en vez de
     * violar la constraint intentando crear un duplicado.
     */
    Optional<Operation> buscarPorRecursoYTag(String idCompany, String resource, String tag);

}
