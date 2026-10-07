package com.atlas.supplychain.auth.application.maintenance.authorization.menu;

import com.atlas.supplychain.auth.application.maintenance.authorization.PermisosVigentesDelUsuario;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.MenuModuloView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.MenuOperacionView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.BusinessProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.MenuCategory;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ProcessStage;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ScreenGroup;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.BusinessProcessRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.MenuCategoryRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleOperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ProcessStageRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ScreenGroupRepository;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orden del menú (Spec 1.3): proceso -&gt; {@code Module.displayOrder} -&gt; orden de pantalla. Un
 * módulo sin proceso, o una pantalla sin etapa, van al final de su nivel -- nunca rompen el orden
 * de lo que sí lo declara.
 *
 * <p>Agrupación del rail (Spec 1.4): eje aparte del orden -- {@code idMenuCategory} (dueño la
 * empresa) dice bajo qué botón cae cada módulo, {@code idScreenGroup} dice qué pantallas de un
 * módulo van en una carpeta plegable. Ninguno de los dos cambia el orden que arma este servicio,
 * solo viajan como dato plano para que el shell decida cómo agrupar/plegar.</p>
 */
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final PermisosVigentesDelUsuario permisosVigentesDelUsuario;
    private final ModuleOperationRepository moduleOperationRepository;
    private final ProcessStageRepository processStageRepository;
    private final BusinessProcessRepository businessProcessRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final ScreenGroupRepository screenGroupRepository;

    @Override
    public List<MenuModuloView> obtenerMenu(String idUser, String idCompany) {
        Map<Module, List<GrantedPermission>> permisosPorModulo =
                permisosVigentesDelUsuario.obtener(idUser, idCompany);

        List<BusinessProcess> procesos = businessProcessRepository.listar();
        Map<Long, Integer> ordenDeProceso = ordenDeProcesos(procesos);
        Map<Long, String> nombreDeProceso = nombreDeProcesos(procesos);
        Map<Long, Integer> secuenciaPorEtapa = processStageRepository.listar().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProcessStage::getIdProcessStage, ProcessStage::getSequence));
        Map<Long, String> nombreDeCategoria = menuCategoryRepository.listarPorState(idCompany, true).stream()
                .collect(java.util.stream.Collectors.toMap(MenuCategory::getIdMenuCategory, MenuCategory::getName));

        return permisosPorModulo.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<Module, List<GrantedPermission>>>comparingInt(
                                entry -> ordenDeProceso.getOrDefault(entry.getKey().getIdBusinessProcess(), Integer.MAX_VALUE))
                        .thenComparingInt(entry -> ordenDeModulo(entry.getKey()))
                        .thenComparing(entry -> entry.getKey().getName()))
                .map(entry -> aModuloView(idCompany, entry.getKey(), entry.getValue(), secuenciaPorEtapa,
                        nombreDeProceso, nombreDeCategoria))
                .toList();
    }

    /**
     * Orden **declarado** por el proveedor, no alfabético: el id de alta es el orden en que se dio
     * de alta cada proceso en el catálogo. Un módulo sin proceso queda fuera del mapa y ordena al
     * final vía {@code getOrDefault(..., MAX_VALUE)} en el llamador.
     */
    private static Map<Long, Integer> ordenDeProcesos(List<BusinessProcess> procesos) {
        List<Long> idsOrdenados = procesos.stream()
                .map(BusinessProcess::getIdBusinessProcess)
                .sorted()
                .toList();

        Map<Long, Integer> orden = new HashMap<>();
        for (int i = 0; i < idsOrdenados.size(); i++) {
            orden.put(idsOrdenados.get(i), i);
        }
        return orden;
    }

    private static Map<Long, String> nombreDeProcesos(List<BusinessProcess> procesos) {
        Map<Long, String> nombres = new HashMap<>();
        procesos.forEach(proceso -> nombres.put(proceso.getIdBusinessProcess(), proceso.getName()));
        return nombres;
    }

    private static int ordenDeModulo(Module module) {
        return module.getDisplayOrder() == null ? 0 : module.getDisplayOrder();
    }

    private MenuModuloView aModuloView(String idCompany, Module module, List<GrantedPermission> permisos,
                                        Map<Long, Integer> secuenciaPorEtapa, Map<Long, String> nombreDeProceso,
                                        Map<Long, String> nombreDeCategoria) {
        Map<Long, Integer> overridesDelModulo =
                moduleOperationRepository.displayOrdersDeModulo(idCompany, module.getIdModule());

        // Carpetas (Spec 1.4): `idOperation -> idScreenGroup` de este módulo + el nombre de cada
        // carpeta -- 2 consultas en bloque por módulo (mismo nivel que `overridesDelModulo`, no
        // una por operación).
        Map<Long, Long> carpetaPorOperacion = moduleOperationRepository.screenGroupsDeModulo(idCompany, module.getIdModule());
        Map<Long, String> nombreDeCarpeta = screenGroupRepository.listarPorModulo(idCompany, module.getIdModule()).stream()
                .collect(java.util.stream.Collectors.toMap(ScreenGroup::getIdScreenGroup, ScreenGroup::getName));

        List<MenuOperacionView> operaciones = permisos.stream()
                .map(GrantedPermission::getOperation)
                .sorted(Comparator
                        .<Operation>comparingInt(operation -> ordenDePantalla(operation, overridesDelModulo, secuenciaPorEtapa))
                        .thenComparing(Operation::getTag))
                .map(operation -> aOperacionView(operation, carpetaPorOperacion, nombreDeCarpeta))
                .toList();

        String businessProcessName = module.getIdBusinessProcess() == null
                ? null : nombreDeProceso.get(module.getIdBusinessProcess());
        String menuCategoryName = module.getIdMenuCategory() == null
                ? null : nombreDeCategoria.get(module.getIdMenuCategory());

        return new MenuModuloView(module.getIdModule(), module.getName(), module.getIcon(),
                module.getIdBusinessProcess(), businessProcessName, module.getIdMenuCategory(), menuCategoryName,
                operaciones);
    }

    /** El override de la empresa manda; si no hay, la secuencia de la etapa; si no tiene etapa, al
     * final -- empatado por {@code tag} entre las 5 operaciones de la misma pantalla. */
    private static int ordenDePantalla(Operation operation, Map<Long, Integer> overrides,
                                        Map<Long, Integer> secuenciaPorEtapa) {
        Integer override = overrides.get(operation.getIdOperation());
        if (override != null) {
            return override;
        }
        if (operation.getIdProcessStage() != null) {
            Integer secuencia = secuenciaPorEtapa.get(operation.getIdProcessStage());
            if (secuencia != null) {
                return secuencia;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static MenuOperacionView aOperacionView(Operation operation, Map<Long, Long> carpetaPorOperacion,
                                                      Map<Long, String> nombreDeCarpeta) {
        Long idScreenGroup = carpetaPorOperacion.get(operation.getIdOperation());
        String screenGroupName = idScreenGroup == null ? null : nombreDeCarpeta.get(idScreenGroup);

        return new MenuOperacionView(operation.getIdOperation(), operation.getResource(), operation.getTag(),
                operation.getName(), operation.getIcon(), idScreenGroup, screenGroupName,
                operation.getPath(), operation.getHttpMethod());
    }
}
