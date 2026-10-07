package com.atlas.supplychain.auth.application.maintenance.authorization;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Colaborador compartido (Spec 1.18): recolecta los {@link GrantedPermission} vigentes de un
 * usuario. Extraído para que {@code GrantedPermissionServiceImpl.obtenerPermisosEfectivos}
 * (formato {@code Set<String>} plano) y {@code MenuServiceImpl.obtenerMenu} (árbol) no
 * reimplementen cada uno su propio criterio de "vigente" -- ambos consumen el mismo agregado.
 *
 * <p>Desde la Spec 1.2 son dos vistas distintas del mismo recorrido, y la diferencia es
 * deliberada: {@link #autoridades} no mira {@link Module} (una operación autoriza su endpoint
 * aunque no cuelgue de ningún módulo), mientras que {@link #obtener} solo devuelve lo que tiene
 * dónde pintarse. Devolver las autoridades a partir del mapa agrupado perdería en silencio las
 * operaciones sin módulo asignado, que es el estado normal de una operación recién
 * auto-registrada.</p>
 */
public interface PermisosVigentesDelUsuario {

    /**
     * @return permisos vigentes agrupados por módulo activo -- un permiso aparece bajo cada módulo
     * al que esté asignada su operación (N:M, Spec 1.2) --, o mapa vacío si el usuario no existe,
     * no pertenece a {@code idCompany} o no tiene ningún permiso vigente que pintar.
     */
    Map<Module, List<GrantedPermission>> obtener(String idUser, String idCompany);

    /**
     * @return autoridades {@code RECURSO:OPERACION} vigentes, o conjunto vacío si el usuario no
     * existe, no pertenece a {@code idCompany} o no tiene ningún permiso vigente.
     */
    Set<String> autoridades(String idUser, String idCompany);
}
