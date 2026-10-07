package com.atlas.supplychain.auth.application.maintenance.authorization.menu;

import com.atlas.supplychain.auth.application.maintenance.authorization.PermisosVigentesDelUsuario;
import com.atlas.supplychain.auth.application.maintenance.authorization.PermisosVigentesDelUsuarioImpl;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.MenuModuloView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.MenuOperacionView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.BusinessProcessRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.MenuCategoryRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleOperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ProcessStageRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ScreenGroupRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private PermisosVigentesDelUsuario permisosVigentesDelUsuario;

    @Mock
    private UserRepository userRepository;

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

    /** Sin etapas/procesos/overrides/categorías/carpetas de por medio: el orden por defecto de
     * estos tests sigue siendo alfabético, el mismo que antes de la Spec 1.3 (ver
     * {@code ordenDePantalla}). */
    @BeforeEach
    void sinEtapasNiOverridesPorDefecto() {
        lenient().when(moduleOperationRepository.displayOrdersDeModulo(any(), any())).thenReturn(Map.of());
        lenient().when(moduleOperationRepository.screenGroupsDeModulo(any(), any())).thenReturn(Map.of());
        lenient().when(processStageRepository.listar()).thenReturn(List.of());
        lenient().when(businessProcessRepository.listar()).thenReturn(List.of());
        lenient().when(menuCategoryRepository.listarPorState(any(), any())).thenReturn(List.of());
        lenient().when(screenGroupRepository.listarPorModulo(any(), any())).thenReturn(List.of());
    }

    private MenuServiceImpl service() {
        return new MenuServiceImpl(permisosVigentesDelUsuario, moduleOperationRepository,
                processStageRepository, businessProcessRepository, menuCategoryRepository, screenGroupRepository);
    }

    private MenuServiceImpl serviceConColaboradorReal() {
        return new MenuServiceImpl(new PermisosVigentesDelUsuarioImpl(userRepository), moduleOperationRepository,
                processStageRepository, businessProcessRepository, menuCategoryRepository, screenGroupRepository);
    }

    @Test
    void obtenerMenuDevuelveLosModulosConOperacionesConcedidasEnOrdenAlfabetico() {
        Module usuarios = Module.builder().idModule(1L).name("USUARIOS").icon("people").state(true).build();
        Module roles = Module.builder().idModule(2L).name("ROLES").state(true).build();

        Operation crear = Operation.builder().idOperation(1L).resource("USUARIOS").tag("CREAR").path("/usuarios").httpMethod("POST").state(true).modules(Set.of(usuarios)).build();
        Operation listar = Operation.builder().idOperation(2L).resource("USUARIOS").tag("LISTAR").path("/usuarios/search").httpMethod("POST").state(true).modules(Set.of(usuarios)).build();
        Operation editar = Operation.builder().idOperation(3L).resource("USUARIOS").tag("EDITAR").path("/usuarios").httpMethod("PATCH").state(true).modules(Set.of(usuarios)).build();
        Operation listarRoles = Operation.builder().idOperation(4L).resource("ROLES").tag("LISTAR").path("/roles").httpMethod("GET").state(true).modules(Set.of(roles)).build();

        when(permisosVigentesDelUsuario.obtener("u1", "c1")).thenReturn(Map.of(
                usuarios, List.of(
                        GrantedPermission.builder().operation(crear).build(),
                        GrantedPermission.builder().operation(listar).build(),
                        GrantedPermission.builder().operation(editar).build()),
                roles, List.of(GrantedPermission.builder().operation(listarRoles).build())
        ));

        List<MenuModuloView> menu = service().obtenerMenu("u1", "c1");

        assertThat(menu).hasSize(2);
        assertThat(menu).extracting(MenuModuloView::name).containsExactly("ROLES", "USUARIOS");

        MenuModuloView moduloRoles = menu.get(0);
        assertThat(moduloRoles.operations()).extracting(MenuOperacionView::tag).containsExactly("LISTAR");

        MenuModuloView moduloUsuarios = menu.get(1);
        assertThat(moduloUsuarios.idModule()).isEqualTo(1L);
        assertThat(moduloUsuarios.icon()).isEqualTo("people");
        assertThat(moduloUsuarios.operations()).extracting(MenuOperacionView::tag)
                .containsExactly("CREAR", "EDITAR", "LISTAR");
    }

    @Test
    void obtenerMenuDevuelveListaVaciaSiElUsuarioNoTieneNingunPermiso() {
        when(permisosVigentesDelUsuario.obtener("u1", "c1")).thenReturn(Map.of());

        assertThat(service().obtenerMenu("u1", "c1")).isEmpty();
    }

    /**
     * El colaborador ya filtra Operation/Module inactivos (Spec 1.18): esta prueba
     * verifica que MenuServiceImpl no reintroduce entradas que el colaborador descartó,
     * usando el colaborador real (PermisosVigentesDelUsuarioImpl) contra un fixture donde
     * hay una Operation con state=false y un Module con state=false.
     */
    @Test
    void obtenerMenuNoIncluyeOperacionesInactivasNiModulosInactivosUsandoElColaboradorReal() {
        Module moduloActivo = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Module moduloInactivo = Module.builder().idModule(2L).name("REPORTES").state(false).build();

        Operation operacionActiva = Operation.builder().idOperation(1L).resource("USUARIOS").tag("CREAR").path("/usuarios").httpMethod("POST").state(true).modules(Set.of(moduloActivo)).build();
        Operation operacionInactiva = Operation.builder().idOperation(2L).resource("USUARIOS").tag("EDITAR").path("/usuarios").httpMethod("PATCH").state(false).modules(Set.of(moduloActivo)).build();
        Operation operacionDeModuloInactivo = Operation.builder().idOperation(3L).resource("REPORTES").tag("CONSULTAR").path("/reportes").httpMethod("GET").state(true).modules(Set.of(moduloInactivo)).build();

        Role role = Role.builder().idRol(1L).idCompany("c1").grantedPermissions(Set.of(
                GrantedPermission.builder().idGrantedPermission(1L).operation(operacionActiva).build(),
                GrantedPermission.builder().idGrantedPermission(2L).operation(operacionInactiva).build(),
                GrantedPermission.builder().idGrantedPermission(3L).operation(operacionDeModuloInactivo).build()
        )).build();
        User user = User.builder().idUser("u1").idCompany("c1").role(role).build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        MenuServiceImpl service = serviceConColaboradorReal();

        List<MenuModuloView> menu = service.obtenerMenu("u1", "c1");

        assertThat(menu).hasSize(1);
        assertThat(menu.getFirst().name()).isEqualTo("USUARIOS");
        assertThat(menu.getFirst().operations()).extracting(MenuOperacionView::tag).containsExactly("CREAR");
    }

    /**
     * Un Module cuyas únicas Operation concedidas están todas con state=false no debe
     * quedar como entrada vacía en el árbol.
     */
    @Test
    void obtenerMenuNoDejaUnModuloVacioSiTodasSusOperacionesConcedidasEstanInactivas() {
        Module modulo = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Operation operacionInactiva = Operation.builder().idOperation(1L).resource("USUARIOS").tag("EDITAR").state(false).modules(Set.of(modulo)).build();

        Role role = Role.builder().idRol(1L).idCompany("c1").grantedPermissions(Set.of(
                GrantedPermission.builder().idGrantedPermission(1L).operation(operacionInactiva).build()
        )).build();
        User user = User.builder().idUser("u1").idCompany("c1").role(role).build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        MenuServiceImpl service = serviceConColaboradorReal();

        assertThat(service.obtenerMenu("u1", "c1")).isEmpty();
    }

    @Test
    void obtenerMenuNoDisparaUnaSegundaConsultaAdemasDeFindByIdUsuarioWithAuthorities() {
        Module modulo = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Operation operacion = Operation.builder().idOperation(1L).resource("USUARIOS").tag("CREAR").state(true).modules(Set.of(modulo)).build();
        Role role = Role.builder().idRol(1L).idCompany("c1").grantedPermissions(Set.of(
                GrantedPermission.builder().idGrantedPermission(1L).operation(operacion).build()
        )).build();
        User user = User.builder().idUser("u1").idCompany("c1").role(role).build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        MenuServiceImpl service = serviceConColaboradorReal();
        service.obtenerMenu("u1", "c1");

        verify(userRepository, times(1)).findByIdUsuarioWithAuthorities("u1");
    }
}
