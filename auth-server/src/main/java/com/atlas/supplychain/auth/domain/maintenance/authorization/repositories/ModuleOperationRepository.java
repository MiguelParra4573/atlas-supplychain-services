package com.atlas.supplychain.auth.domain.maintenance.authorization.repositories;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;

import java.util.List;
import java.util.Map;

/**
 * Asignaciones {@code Module} <-> {@code Operation} (Spec 1.2, N:M). Vive aparte de
 * {@code ModuleRepository}/{@code OperationRepository} porque la asignación no pertenece a ninguno
 * de los dos agregados: es la decisión de la empresa sobre cómo agrupar su menú, y ninguno de los
 * dos lados manda sobre el otro.
 */
public interface ModuleOperationRepository {

    /** Idempotente: asignar dos veces la misma operación al mismo módulo no falla ni duplica. */
    void asignar(String idCompany, Long idModule, Long idOperation);

    void desasignar(String idCompany, Long idModule, Long idOperation);

    /** Módulos de la empresa bajo los que aparece esta operación. Vacío si no está asignada. */
    List<Module> listarModulosDeOperacion(String idCompany, Long idOperation);

    /** Baja de todas las asignaciones de un módulo -- las operaciones no se tocan. */
    void eliminarAsignacionesDeModulo(String idCompany, Long idModule);

    /** Baja de todas las asignaciones de una operación, para poder borrarla sin dejar huérfanas. */
    void eliminarAsignacionesDeOperacion(String idCompany, Long idOperation);

    /**
     * Overrides de orden de la empresa para las pantallas de este módulo (Spec 1.3):
     * {@code idOperation -> displayOrder}. Solo las filas que sí tienen override -- una ausente
     * usa la secuencia de su {@code ProcessStage}, o el final si no tiene ninguna de las dos.
     */
    Map<Long, Integer> displayOrdersDeModulo(String idCompany, Long idModule);

    /**
     * Fija el orden de las pantallas ({@code resource}) de este módulo en una sola llamada (Spec
     * 1.3) -- las 5 operaciones de un mismo {@code resource} reciben el mismo {@code displayOrder}.
     * Solo toca operaciones ya asignadas al módulo; un {@code resource} sin asignación ahí se
     * ignora en silencio, no es error de esta llamada.
     */
    void reordenarPantallas(String idCompany, Long idModule, Map<String, Integer> displayOrderPorRecurso);

    /**
     * Fija la carpeta ({@link com.atlas.supplychain.auth.domain.maintenance.authorization.model.ScreenGroup})
     * de las operaciones ya asignadas de un {@code resource}, dentro de este módulo (Spec 1.4).
     * {@code idScreenGroup == null} la saca de cualquier carpeta. Solo toca filas que ya existen --
     * un {@code resource} sin asignar a este módulo se ignora en silencio, mismo criterio que
     * {@link #reordenarPantallas}.
     */
    void asignarCarpetaARecurso(String idCompany, Long idModule, String resource, Long idScreenGroup);

    /**
     * `idOperation -> idScreenGroup` de las operaciones de este módulo que sí tienen carpeta (Spec
     * 1.4) -- resolución en bloque para {@code MenuServiceImpl}, mismo criterio anti-N+1 que
     * {@link #displayOrdersDeModulo}.
     */
    Map<Long, Long> screenGroupsDeModulo(String idCompany, Long idModule);
}
