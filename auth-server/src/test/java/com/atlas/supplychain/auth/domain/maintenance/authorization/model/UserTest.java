package com.atlas.supplychain.auth.domain.maintenance.authorization.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void permisosEfectivosDevuelveLasAutoridadesDeLosPermisosVigentesDeDosRecursos() {
        Module usuarios = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Module roles = Module.builder().idModule(2L).name("ROLES").state(true).build();

        Operation crear = operacion("USUARIOS", "CREAR", true, usuarios);
        Operation editar = operacion("USUARIOS", "EDITAR", true, usuarios);
        Operation listar = operacion("ROLES", "LISTAR", true, roles);

        User user = usuarioCon(crear, editar, listar);

        assertThat(user.permisosEfectivos()).containsExactlyInAnyOrder("USUARIOS:CREAR", "USUARIOS:EDITAR", "ROLES:LISTAR");
    }

    @Test
    void permisosEfectivosIgnoraPermisosCuyaOperacionEstaInactiva() {
        Module modulo = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Operation activa = operacion("USUARIOS", "CREAR", true, modulo);
        Operation inactiva = operacion("USUARIOS", "EDITAR", false, modulo);

        assertThat(usuarioCon(activa, inactiva).permisosEfectivos()).containsExactly("USUARIOS:CREAR");
    }

    /**
     * Spec 1.2: desactivar un módulo corta el acceso de verdad, no solo lo saca del menú. Si solo
     * dejara de pintarse, el endpoint seguiría respondiendo a quien lo llamara por fuera del menú
     * (Postman, un enlace guardado, otra pantalla) y la pantalla de módulos estaría mintiendo.
     */
    @Test
    void permisosEfectivosNoIncluyeOperacionesCuyosModulosEstanTodosInactivos() {
        Module moduloInactivo = Module.builder().idModule(9L).name("REPORTES").state(false).build();
        Operation operacion = operacion("REPORTES", "CONSULTAR", true, moduloInactivo);

        assertThat(usuarioCon(operacion).permisosEfectivos()).isEmpty();
    }

    /**
     * Basta un módulo activo: la misma pantalla puede vivir bajo dos agrupaciones y apagar una no
     * tiene por qué apagar la otra.
     */
    @Test
    void permisosEfectivosIncluyeLaOperacionSiAlMenosUnoDeSusModulosSigueActivo() {
        Module ventas = Module.builder().idModule(1L).name("Ventas").state(true).build();
        Module administracion = Module.builder().idModule(2L).name("Administración").state(false).build();
        Operation clientes = operacion("CLIENTES", "LISTAR", true, ventas, administracion);

        assertThat(usuarioCon(clientes).permisosEfectivos()).containsExactly("CLIENTES:LISTAR");
    }

    /**
     * Estado normal de una operación recién auto-registrada: nadie la asignó a un módulo todavía.
     * Sigue autorizando su endpoint.
     */
    @Test
    void permisosEfectivosIncluyeOperacionesSinNingunModuloAsignado() {
        Operation huerfana = Operation.builder()
                .resource("INVENTARIO").tag("CREAR").state(true).modules(Set.of()).build();

        assertThat(usuarioCon(huerfana).permisosEfectivos()).containsExactly("INVENTARIO:CREAR");
    }

    @Test
    void permisosEfectivosDuplicadosHaciaLaMismaOperacionProducenUnaSolaEntrada() {
        Module modulo = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Operation operacion = operacion("USUARIOS", "CREAR", true, modulo);

        Role role = Role.builder().grantedPermissions(Set.of(
                GrantedPermission.builder().idGrantedPermission(1L).operation(operacion).build(),
                GrantedPermission.builder().idGrantedPermission(2L).operation(operacion).build()
        )).build();
        User user = User.builder().idUser("u1").role(role).build();

        assertThat(user.permisosEfectivos()).containsExactly("USUARIOS:CREAR");
    }

    @Test
    void permisosEfectivosDevuelveConjuntoVacioSiElUsuarioNoTieneRol() {
        User user = User.builder().idUser("u1").role(null).build();

        assertThat(user.permisosEfectivos()).isEmpty();
    }

    @Test
    void permisosVigentesPorModuloAgrupaLosPermisosVigentesPorModulo() {
        Module usuarios = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Module roles = Module.builder().idModule(2L).name("ROLES").state(true).build();

        Operation crear = operacion("USUARIOS", "CREAR", true, usuarios);
        Operation editar = operacion("USUARIOS", "EDITAR", true, usuarios);
        Operation listar = operacion("ROLES", "LISTAR", true, roles);

        Map<Module, List<GrantedPermission>> porModulo = usuarioCon(crear, editar, listar).permisosVigentesPorModulo();

        assertThat(porModulo).hasSize(2);
        assertThat(porModulo.get(usuarios)).extracting(GrantedPermission::getOperation)
                .containsExactlyInAnyOrder(crear, editar);
        assertThat(porModulo.get(roles)).extracting(GrantedPermission::getOperation)
                .containsExactly(listar);
    }

    /** N:M (Spec 1.2): la misma pantalla puede vivir bajo dos agrupaciones de la misma empresa. */
    @Test
    void unaOperacionEnDosModulosApareceBajoLosDosPeroConUnaSolaAutoridad() {
        Module ventas = Module.builder().idModule(1L).name("Ventas").state(true).build();
        Module administracion = Module.builder().idModule(2L).name("Administración").state(true).build();
        Operation clientes = operacion("CLIENTES", "LISTAR", true, ventas, administracion);

        User user = usuarioCon(clientes);

        assertThat(user.permisosVigentesPorModulo()).hasSize(2);
        assertThat(user.permisosVigentesPorModulo().get(ventas)).hasSize(1);
        assertThat(user.permisosVigentesPorModulo().get(administracion)).hasSize(1);
        assertThat(user.permisosEfectivos()).containsExactly("CLIENTES:LISTAR");
    }

    @Test
    void permisosVigentesPorModuloIgnoraModulosInactivosYOperacionesSinModulo() {
        Module activo = Module.builder().idModule(1L).name("USUARIOS").state(true).build();
        Module inactivo = Module.builder().idModule(2L).name("REPORTES").state(false).build();

        Operation visible = operacion("USUARIOS", "CREAR", true, activo);
        Operation enModuloInactivo = operacion("REPORTES", "CONSULTAR", true, inactivo);
        Operation sinModulo = Operation.builder()
                .resource("INVENTARIO").tag("CREAR").state(true).modules(Set.of()).build();

        Map<Module, List<GrantedPermission>> porModulo =
                usuarioCon(visible, enModuloInactivo, sinModulo).permisosVigentesPorModulo();

        assertThat(porModulo).containsOnlyKeys(activo);
    }

    @Test
    void permisosVigentesPorModuloDevuelveMapaVacioSiElUsuarioNoTieneRol() {
        User user = User.builder().idUser("u1").role(null).build();

        assertThat(user.permisosVigentesPorModulo()).isEmpty();
    }

    private static Operation operacion(String resource, String tag, boolean state, Module... modules) {
        return Operation.builder()
                .resource(resource)
                .tag(tag)
                .state(state)
                .modules(Set.of(modules))
                .build();
    }

    private static User usuarioCon(Operation... operaciones) {
        Set<GrantedPermission> permisos = new java.util.HashSet<>();
        long id = 1;
        for (Operation operacion : operaciones) {
            permisos.add(GrantedPermission.builder().idGrantedPermission(id++).operation(operacion).build());
        }
        return User.builder().idUser("u1").role(Role.builder().idRol(1L).grantedPermissions(permisos).build()).build();
    }
}
