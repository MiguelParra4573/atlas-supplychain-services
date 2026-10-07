package com.atlas.supplychain.auth.application.maintenance.authorization;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermisosVigentesDelUsuarioImplTest {

    @Mock
    private UserRepository userRepository;

    private PermisosVigentesDelUsuarioImpl collaborator() {
        return new PermisosVigentesDelUsuarioImpl(userRepository);
    }

    @Test
    void obtenerAgrupaLosPermisosVigentesPorModuloFiltrandoOperacionesYModulosInactivos() {
        Module moduloActivo = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Module moduloInactivo = Module.builder().idModule(2L).name("REPORTES").state(false).build();

        Operation operacionActiva = Operation.builder().idOperation(1L).resource("USUARIOS").tag("CREAR")
                .state(true).modules(Set.of(moduloActivo)).build();
        Operation operacionInactiva = Operation.builder().idOperation(2L).resource("USUARIOS").tag("EDITAR")
                .state(false).modules(Set.of(moduloActivo)).build();
        Operation operacionDeModuloInactivo = Operation.builder().idOperation(3L).resource("REPORTES").tag("CONSULTAR")
                .state(true).modules(Set.of(moduloInactivo)).build();

        Role role = Role.builder().idRol(1L).idCompany("c1").grantedPermissions(Set.of(
                GrantedPermission.builder().idGrantedPermission(1L).operation(operacionActiva).build(),
                GrantedPermission.builder().idGrantedPermission(2L).operation(operacionInactiva).build(),
                GrantedPermission.builder().idGrantedPermission(3L).operation(operacionDeModuloInactivo).build()
        )).build();

        User user = User.builder().idUser("u1").idCompany("c1").role(role).build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        var resultado = collaborator().obtener("u1", "c1");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(moduloActivo)).extracting(GrantedPermission::getOperation)
                .containsExactly(operacionActiva);
    }

    /**
     * Spec 1.2: {@code autoridades} y {@code obtener} son dos vistas del mismo recorrido y no
     * filtran igual a propósito -- una operación sin ningún módulo autoriza su endpoint pero no
     * tiene dónde pintarse en el menú. Derivar las autoridades a partir del mapa agrupado las
     * perdería, que es justo por lo que existe el método aparte.
     *
     * <p>El caso contrario, operación en módulo desactivado, no aparece acá porque ya no autoriza:
     * lo cubre {@code noAutorizaOperacionesCuyosModulosEstanTodosInactivos}.</p>
     */
    @Test
    void autoridadesIncluyeOperacionesSinModuloQueElMenuNoMuestra() {
        Operation sinModulo = Operation.builder().idOperation(2L).resource("INVENTARIO").tag("CREAR")
                .state(true).modules(Set.of()).build();

        Role role = Role.builder().idRol(1L).idCompany("c1").grantedPermissions(Set.of(
                GrantedPermission.builder().idGrantedPermission(2L).operation(sinModulo).build()
        )).build();
        User user = User.builder().idUser("u1").idCompany("c1").role(role).build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        assertThat(collaborator().autoridades("u1", "c1")).containsExactly("INVENTARIO:CREAR");
        assertThat(collaborator().obtener("u1", "c1")).isEmpty();
    }

    /** Spec 1.2: esconder el módulo corta el acceso, no solo el pintado. */
    @Test
    void noAutorizaOperacionesCuyosModulosEstanTodosInactivos() {
        Module moduloInactivo = Module.builder().idModule(2L).name("REPORTES").state(false).build();
        Operation enModuloInactivo = Operation.builder().idOperation(1L).resource("REPORTES").tag("CONSULTAR")
                .state(true).modules(Set.of(moduloInactivo)).build();

        Role role = Role.builder().idRol(1L).idCompany("c1").grantedPermissions(Set.of(
                GrantedPermission.builder().idGrantedPermission(1L).operation(enModuloInactivo).build()
        )).build();
        User user = User.builder().idUser("u1").idCompany("c1").role(role).build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        assertThat(collaborator().autoridades("u1", "c1")).isEmpty();
        assertThat(collaborator().obtener("u1", "c1")).isEmpty();
    }

    @Test
    void autoridadesDevuelveConjuntoVacioSiElUsuarioNoExiste() {
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.empty());

        assertThat(collaborator().autoridades("u1", "c1")).isEmpty();
    }

    @Test
    void obtenerDevuelveMapaVacioSiElUsuarioNoExiste() {
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.empty());

        assertThat(collaborator().obtener("u1", "c1")).isEmpty();
    }

    @Test
    void obtenerDevuelveMapaVacioSiElIdCompanyNoCoincideConElDelUsuario() {
        Role role = Role.builder().idRol(1L).idCompany("c1").grantedPermissions(Set.of()).build();
        User user = User.builder().idUser("u1").idCompany("c1").role(role).build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        assertThat(collaborator().obtener("u1", "c2")).isEmpty();
    }

    @Test
    void obtenerConsultaElRepositorioUnaSolaVez() {
        Role role = Role.builder().idRol(1L).idCompany("c1").grantedPermissions(Set.of()).build();
        User user = User.builder().idUser("u1").idCompany("c1").role(role).build();
        when(userRepository.findByIdUsuarioWithAuthorities("u1")).thenReturn(Optional.of(user));

        collaborator().obtener("u1", "c1");

        verify(userRepository, times(1)).findByIdUsuarioWithAuthorities("u1");
    }
}
