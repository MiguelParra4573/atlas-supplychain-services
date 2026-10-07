package com.atlas.supplychain.auth.application.maintenance.authorization.operation;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.screen.ModuleScreenOrder;
import com.atlas.supplychain.auth.domain.maintenance.authorization.screen.ScreenSummary;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;
import java.util.Map;

public interface OperationService {

    void crearOperacion(Operation operation);

    void actualizarOperacion(Operation operation) throws EntityNotFoundException;

    void eliminarOperacion(String idCompany, Long idOperation) throws EntityNotFoundException;

    Operation obtenerOperacionPorId(String idCompany, Long idOperation) throws EntityNotFoundException;

    List<Operation> listarOperacionesPorModulo(String idCompany, Long idModule);

    /** Operaciones activas de la empresa que todavía no cuelgan de ningún módulo (Spec 1.2). */
    List<Operation> listarOperacionesSinModulo(String idCompany);

    PagedResult<Operation> listarOperacionesPaginadas(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);

    /** Cuelga una operación de un módulo del menú. Idempotente. */
    void asignarAModulo(String idCompany, Long idOperation, Long idModule) throws EntityNotFoundException;

    /** La descuelga. Si era su único módulo, la operación queda sin menú pero conserva su autoridad. */
    void desasignarDeModulo(String idCompany, Long idOperation, Long idModule) throws EntityNotFoundException;

    /** Módulos bajo los que aparece hoy esta operación. */
    List<Module> listarModulosDeOperacion(String idCompany, Long idOperation) throws EntityNotFoundException;

    /** Cuelga las 5 operaciones activas de una pantalla (Spec 1.3) de un módulo, en un solo paso. */
    void asignarRecursoAModulo(String idCompany, String resource, Long idModule) throws EntityNotFoundException;

    /** Las descuelga a todas. */
    void desasignarRecursoDeModulo(String idCompany, String resource, Long idModule) throws EntityNotFoundException;

    /** Fija el orden de las pantallas de un módulo en una sola llamada (Spec 1.3). */
    void reordenarPantallasDeModulo(String idCompany, Long idModule, Map<String, Integer> displayOrderPorRecurso)
            throws EntityNotFoundException;

    /** Fija (o quita, con {@code null}) la carpeta de una pantalla ya asignada a este módulo
     * (Spec 1.4). No falla si la pantalla no está asignada -- no hace nada, mismo criterio que
     * {@link #reordenarPantallasDeModulo}. */
    void asignarCarpetaARecurso(String idCompany, String resource, Long idModule, Long idScreenGroup)
            throws EntityNotFoundException;

    /** Buscador de pantallas (Spec 1.3): una fila por {@code resource}, con sus módulos actuales. */
    PagedResult<ScreenSummary> buscarPantallas(String idCompany, String resourceLike, boolean onlyUnassigned,
                                                PageQuery pageQuery);

    /**
     * Pantallas asignadas a un módulo con su override de orden actual (Spec 1.3), para la pantalla
     * de reordenamiento -- ordenadas por override (las sin override, al final) y luego por
     * {@code resource}.
     */
    List<ModuleScreenOrder> pantallasDeModulo(String idCompany, Long idModule) throws EntityNotFoundException;
}
