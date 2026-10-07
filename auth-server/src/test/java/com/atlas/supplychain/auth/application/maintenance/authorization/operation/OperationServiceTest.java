package com.atlas.supplychain.auth.application.maintenance.authorization.operation;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.common.exceptions.ExistingDependenciesException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.GrantedPermissionRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleOperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.OperationRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atlas.supplychain.auth.domain.maintenance.authorization.screen.ModuleScreenOrder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    @Mock
    private OperationRepository repository;
    @Mock
    private GrantedPermissionRepository grantedPermissionRepository;
    @Mock
    private ModuleOperationRepository moduleOperationRepository;
    @Mock
    private ModuleRepository moduleRepository;

    private OperationServiceImpl service() {
        return new OperationServiceImpl(repository, grantedPermissionRepository, moduleOperationRepository,
                moduleRepository);
    }

    @Test
    void crearOperacionCreaCuandoNoExisteLaMismaAutoridadEnLaEmpresa() {
        Operation operation = Operation.builder().idCompany("c1").resource("USUARIOS").tag("CREAR").build();
        when(repository.existsByResourceAndTag("c1", "USUARIOS", "CREAR")).thenReturn(false);

        service().crearOperacion(operation);

        verify(repository).crearOperacion(operation);
    }

    @Test
    void crearOperacionLanzaDuplicateRecordExceptionSiYaExisteLaMismaAutoridad() {
        Operation operation = Operation.builder().idCompany("c1").resource("USUARIOS").tag("CREAR").build();
        when(repository.existsByResourceAndTag("c1", "USUARIOS", "CREAR")).thenReturn(true);

        assertThatThrownBy(() -> service().crearOperacion(operation)).isInstanceOf(DuplicateRecordException.class);

        verify(repository, never()).crearOperacion(operation);
    }

    /**
     * Spec 1.2: el mismo {@code tag} en otro recurso no es duplicado -- el vocabulario cerrado de
     * tags (CREAR/EDITAR/...) se repite por diseño en cada recurso. Antes la búsqueda era solo por
     * tag y sin empresa, así que el primer CREAR del sistema bloqueaba todos los demás.
     */
    @Test
    void crearOperacionNoConfundeElMismoTagEnOtroRecurso() {
        Operation operation = Operation.builder().idCompany("c1").resource("BODEGAS").tag("CREAR").build();
        when(repository.existsByResourceAndTag("c1", "BODEGAS", "CREAR")).thenReturn(false);

        service().crearOperacion(operation);

        verify(repository).crearOperacion(operation);
    }

    @Test
    void actualizarOperacionDelegaEnElRepositorio() throws EntityNotFoundException {
        Operation operation = Operation.builder().idCompany("c1").idOperation(1L).resource("USUARIOS").tag("CREAR").build();

        service().actualizarOperacion(operation);

        verify(repository).actualizarOperacion(operation);
    }

    @Test
    void actualizarOperacionPropagaEntityNotFoundExceptionDelRepositorio() throws EntityNotFoundException {
        Operation operation = Operation.builder().idCompany("c1").idOperation(99L).resource("USUARIOS").tag("CREAR").build();
        doThrow(new EntityNotFoundException("no existe")).when(repository).actualizarOperacion(operation);

        assertThatThrownBy(() -> service().actualizarOperacion(operation)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminarOperacionSueltaSusAsignacionesDeMenuYLaBorra() throws EntityNotFoundException {
        when(grantedPermissionRepository.existsByOperation("c1", 1L)).thenReturn(false);

        service().eliminarOperacion("c1", 1L);

        verify(moduleOperationRepository).eliminarAsignacionesDeOperacion("c1", 1L);
        verify(repository).eliminarOperacion("c1", 1L);
    }

    @Test
    void eliminarOperacionLanzaExistingDependenciesExceptionSiTienePermisosConcedidosAsociados() throws EntityNotFoundException {
        when(grantedPermissionRepository.existsByOperation("c1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service().eliminarOperacion("c1", 1L)).isInstanceOf(ExistingDependenciesException.class);

        verify(repository, never()).eliminarOperacion("c1", 1L);
        verify(moduleOperationRepository, never()).eliminarAsignacionesDeOperacion("c1", 1L);
    }

    @Test
    void obtenerOperacionPorIdDelegaEnElRepositorio() throws EntityNotFoundException {
        Operation operation = unaOperacion();
        when(repository.obtenerOperacionPorId("c1", 1L)).thenReturn(operation);

        assertThat(service().obtenerOperacionPorId("c1", 1L)).isEqualTo(operation);
    }

    @Test
    void listarOperacionesPorModuloDelegaEnElRepositorio() {
        Operation operation = unaOperacion();
        when(repository.listarOperacionesPorModulo("c1", 1L)).thenReturn(List.of(operation));

        assertThat(service().listarOperacionesPorModulo("c1", 1L)).containsExactly(operation);
    }

    @Test
    void listarOperacionesSinModuloDelegaEnElRepositorio() {
        Operation operation = unaOperacion();
        when(repository.listarOperacionesSinModulo("c1")).thenReturn(List.of(operation));

        assertThat(service().listarOperacionesSinModulo("c1")).containsExactly(operation);
    }

    @Test
    void asignarAModuloResuelveAmbosLadosContraLaEmpresaAntesDeAsignar() throws EntityNotFoundException {
        when(repository.obtenerOperacionPorId("c1", 1L)).thenReturn(unaOperacion());
        when(moduleRepository.obtenerModuloPorId("c1", 7L))
                .thenReturn(Module.builder().idCompany("c1").idModule(7L).name("Ventas").build());

        service().asignarAModulo("c1", 1L, 7L);

        verify(moduleOperationRepository).asignar("c1", 7L, 1L);
    }

    /** Sin esta comprobación se podría colgar una operación de otra empresa de un módulo propio. */
    @Test
    void asignarAModuloNoTocaLaTablaPuenteSiLaOperacionNoEsDeLaEmpresa() throws EntityNotFoundException {
        when(repository.obtenerOperacionPorId("c1", 1L)).thenThrow(new EntityNotFoundException("no existe"));

        assertThatThrownBy(() -> service().asignarAModulo("c1", 1L, 7L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(moduleOperationRepository, never()).asignar("c1", 7L, 1L);
    }

    @Test
    void desasignarDeModuloDelegaEnElRepositorioDeAsignaciones() throws EntityNotFoundException {
        when(repository.obtenerOperacionPorId("c1", 1L)).thenReturn(unaOperacion());
        when(moduleRepository.obtenerModuloPorId("c1", 7L))
                .thenReturn(Module.builder().idCompany("c1").idModule(7L).name("Ventas").build());

        service().desasignarDeModulo("c1", 1L, 7L);

        verify(moduleOperationRepository).desasignar("c1", 7L, 1L);
    }

    @Test
    void listarModulosDeOperacionDelegaEnElRepositorioDeAsignaciones() throws EntityNotFoundException {
        Module ventas = Module.builder().idCompany("c1").idModule(7L).name("Ventas").build();
        when(repository.obtenerOperacionPorId("c1", 1L)).thenReturn(unaOperacion());
        when(moduleOperationRepository.listarModulosDeOperacion("c1", 1L)).thenReturn(List.of(ventas));

        assertThat(service().listarModulosDeOperacion("c1", 1L)).containsExactly(ventas);
    }

    @Test
    void listarOperacionesPaginadasDelegaEnElRepositorio() {
        Operation operation = unaOperacion();
        PageQuery pageQuery = new PageQuery(0, 10, List.of());
        PagedResult<Operation> expected = new PagedResult<>(List.of(operation), 0, 10, 1);
        when(repository.listarOperacionesPaginadas("c1", List.of(), pageQuery)).thenReturn(expected);

        assertThat(service().listarOperacionesPaginadas("c1", List.of(), pageQuery)).isEqualTo(expected);
    }

    /**
     * Bug real (NPE en producción, `HashMap.merge` con valor nulo): una pantalla recién asignada
     * a un módulo, sin que nadie le haya puesto un orden todavía, es el caso normal -- no debe
     * tirar 500, tiene que aparecer con `displayOrder == null`.
     */
    @Test
    void pantallasDeModuloNoRevientaConPantallasSinOrdenTodavia() throws EntityNotFoundException {
        Operation sinOrden = Operation.builder().idCompany("c1").idOperation(1L).resource("USUARIOS").tag("CREAR").build();
        Operation conOrden = Operation.builder().idCompany("c1").idOperation(2L).resource("ROLES").tag("CREAR").build();
        when(moduleRepository.obtenerModuloPorId("c1", 7L))
                .thenReturn(Module.builder().idCompany("c1").idModule(7L).name("Seguridad").build());
        when(repository.listarOperacionesPorModulo("c1", 7L)).thenReturn(List.of(sinOrden, conOrden));
        when(moduleOperationRepository.displayOrdersDeModulo("c1", 7L)).thenReturn(Map.of(2L, 5));
        when(moduleOperationRepository.screenGroupsDeModulo("c1", 7L)).thenReturn(Map.of());

        List<ModuleScreenOrder> resultado = service().pantallasDeModulo("c1", 7L);

        assertThat(resultado).containsExactly(
                new ModuleScreenOrder("ROLES", 5, null),
                new ModuleScreenOrder("USUARIOS", null, null));
    }

    @Test
    void asignarCarpetaARecursoVerificaQueElModuloExistaYDelegaEnElRepositorio() throws EntityNotFoundException {
        when(moduleRepository.obtenerModuloPorId("c1", 7L))
                .thenReturn(Module.builder().idCompany("c1").idModule(7L).name("Nómina").build());

        service().asignarCarpetaARecurso("c1", "SOLICITUDES_VACACIONES", 7L, 3L);

        verify(moduleOperationRepository).asignarCarpetaARecurso("c1", 7L, "SOLICITUDES_VACACIONES", 3L);
    }

    private static Operation unaOperacion() {
        return Operation.builder().idCompany("c1").idOperation(1L).resource("USUARIOS").tag("CREAR").build();
    }
}
