package com.atlas.supplychain.auth.application.maintenance.authorization.menu;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.maintenance.authorization.PermisosVigentesDelUsuarioImpl;
import com.atlas.supplychain.auth.application.maintenance.authorization.grantedpermission.GrantedPermissionServiceImpl;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.MenuModuloView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.MenuOperacionView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.BusinessProcessRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.GrantedPermissionRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.MenuCategoryRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleOperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.OperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ProcessStageRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.RoleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ScreenGroupRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Spec 1.18 + Spec 1.2: impide que {@code GrantedPermissionService.obtenerPermisosEfectivos}
 * (formato {@code Set<String>} plano) y {@code MenuService.obtenerMenu} (árbol) diverjan en su
 * criterio de "permiso vigente" -- ambos consumen el mismo colaborador
 * ({@code PermisosVigentesDelUsuario}).
 *
 * <p>Hasta la Spec 1.2 la relación era de igualdad: cada autoridad tenía su par exacto
 * {@code (module.name, operation.tag)} en el árbol. Ahora es de <b>inclusión</b>, por un único
 * motivo: una operación todavía sin agrupar autoriza su endpoint pero no tiene módulo donde
 * pintarse. Un módulo desactivado, en cambio, no produce asimetría -- saca la operación de las dos
 * vistas a la vez, porque esconder el módulo corta el acceso.</p>
 *
 * <p>El invariante que este test protege: todo lo que el árbol muestra tiene que estar autorizado.
 * Un menú que ofrezca una pantalla sin permiso sería el bug grave.</p>
 */
@ExtendWith(MockitoExtension.class)
class MenuYPermisosEfectivosParidadTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GrantedPermissionRepository grantedPermissionRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private AuditTrail auditTrail;
    @Mock
    private CurrentActor currentActor;
    @Mock
    private ModuleOperationRepository moduleOperationRepository;
    @Mock
    private ProcessStageRepository processStageRepository;
    @Mock
    private BusinessProcessRepository businessProcessRepository;
    @Mock
    private MenuCategoryRepository menuCategoryRepository;
    @Mock
    private ScreenGroupRepository screenGroupRepository;

    @Test
    void elArbolDelMenuNuncaOfreceAlgoQueLasAutoridadesNoCubran() {
        Module usuarios = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Module roles = Module.builder().idModule(2L).name("ROLES").state(true).build();
        Module inactivo = Module.builder().idModule(3L).name("REPORTES").state(false).build();

        Operation crear = operacion(1L, "USUARIOS", "CREAR", "/usuarios", "POST", true, usuarios);
        Operation editar = operacion(2L, "USUARIOS", "EDITAR", "/usuarios", "PATCH", false, usuarios);
        Operation listarRoles = operacion(3L, "ROLES", "LISTAR", "/roles", "GET", true, roles);
        Operation consultarReportes = operacion(4L, "REPORTES", "CONSULTAR", "/reportes", "GET", true, inactivo);
        Operation sinModulo = Operation.builder().idOperation(5L).resource("INVENTARIO").tag("CREAR")
                .path("/inventario").httpMethod("POST").state(true).modules(Set.of()).build();

        Map.Entry<Set<String>, List<MenuModuloView>> resultado =
                autoridadesYMenu(Set.of(crear, editar, listarRoles, consultarReportes, sinModulo));
        Set<String> autoridades = resultado.getKey();
        List<MenuModuloView> menu = resultado.getValue();

        // Quedan afuera de las dos vistas tanto la operación inactiva como la que solo colgaba de
        // un módulo desactivado. La que no tiene módulo autoriza aunque no se pinte.
        assertThat(autoridades).containsExactlyInAnyOrder(
                "USUARIOS:CREAR", "ROLES:LISTAR", "INVENTARIO:CREAR");

        // El menú solo muestra lo que tiene módulo activo.
        assertThat(menu).extracting(MenuModuloView::name).containsExactly("ROLES", "USUARIOS");

        Set<String> autoridadesDelArbol = menu.stream()
                .flatMap(modulo -> modulo.operations().stream().map(MenuOperacionView::tag)
                        .map(tag -> recursoDe(modulo.name()) + ":" + tag))
                .collect(Collectors.toSet());

        assertThat(autoridadesDelArbol).isSubsetOf(autoridades);
        assertThat(autoridadesDelArbol).containsExactlyInAnyOrder("USUARIOS:CREAR", "ROLES:LISTAR");
    }

    /**
     * En este fixture el módulo se llama igual que el recurso (es el estado que deja la migración
     * del changelog 025), así que el nombre alcanza para reconstruir la autoridad esperada. Fuera
     * del test no vale la equivalencia: justamente la spec permite que la empresa los divorcie.
     */
    private static String recursoDe(String nombreDeModulo) {
        return nombreDeModulo;
    }

    private static Operation operacion(long id, String resource, String tag, String path, String httpMethod,
                                        boolean state, Module module) {
        return Operation.builder()
                .idOperation(id).resource(resource).tag(tag).path(path).httpMethod(httpMethod)
                .state(state).modules(Set.of(module))
                .build();
    }

    private Map.Entry<Set<String>, List<MenuModuloView>> autoridadesYMenu(Set<Operation> operaciones) {
        Set<GrantedPermission> permisos = operaciones.stream()
                .map(op -> GrantedPermission.builder().idGrantedPermission(op.getIdOperation()).operation(op).build())
                .collect(Collectors.toSet());

        Role role = Role.builder().idRol(1L).idCompany("c1").grantedPermissions(permisos).build();
        User user = User.builder().idUser("u1").idCompany("c1").role(role).build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        lenient().when(moduleOperationRepository.displayOrdersDeModulo(any(), any())).thenReturn(Map.of());
        lenient().when(moduleOperationRepository.screenGroupsDeModulo(any(), any())).thenReturn(Map.of());
        lenient().when(processStageRepository.listar()).thenReturn(List.of());
        lenient().when(businessProcessRepository.listar()).thenReturn(List.of());
        lenient().when(menuCategoryRepository.listarPorState(any(), any())).thenReturn(List.of());
        lenient().when(screenGroupRepository.listarPorModulo(any(), any())).thenReturn(List.of());

        PermisosVigentesDelUsuarioImpl colaborador = new PermisosVigentesDelUsuarioImpl(userRepository);
        GrantedPermissionServiceImpl grantedPermissionService = new GrantedPermissionServiceImpl(
                grantedPermissionRepository, roleRepository, operationRepository, colaborador, auditTrail, currentActor);
        MenuServiceImpl menuService = new MenuServiceImpl(colaborador, moduleOperationRepository,
                processStageRepository, businessProcessRepository, menuCategoryRepository, screenGroupRepository);

        return Map.entry(
                grantedPermissionService.obtenerPermisosEfectivos("u1", "c1"),
                menuService.obtenerMenu("u1", "c1"));
    }
}
