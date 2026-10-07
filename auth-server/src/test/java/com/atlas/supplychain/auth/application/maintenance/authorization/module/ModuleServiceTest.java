package com.atlas.supplychain.auth.application.maintenance.authorization.module;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleOperationRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock
    private ModuleRepository repository;
    @Mock
    private ModuleOperationRepository moduleOperationRepository;

    private ModuleServiceImpl service() {
        return new ModuleServiceImpl(repository, moduleOperationRepository);
    }

    @Test
    void crearModuloDelegaEnElRepositorio() {
        Module module = Module.builder().idCompany("c1").name("USUARIOS").build();

        service().crearModulo(module);

        verify(repository).crearModulo(module);
    }

    @Test
    void actualizarModuloDelegaEnElRepositorio() throws EntityNotFoundException {
        Module module = Module.builder().idCompany("c1").idModule(1L).name("USUARIOS").build();

        service().actualizarModulo(module);

        verify(repository).actualizarModulo(module);
    }

    @Test
    void actualizarModuloPropagaEntityNotFoundExceptionDelRepositorio() throws EntityNotFoundException {
        Module module = Module.builder().idCompany("c1").idModule(99L).name("USUARIOS").build();
        doThrow(new EntityNotFoundException("no existe")).when(repository).actualizarModulo(module);

        assertThatThrownBy(() -> service().actualizarModulo(module)).isInstanceOf(EntityNotFoundException.class);
    }

    /**
     * Spec 1.2: tener operaciones asignadas ya no bloquea la baja. Antes era una dependencia real
     * (la operación colgaba del módulo con una FK); ahora es solo agrupación, así que se sueltan
     * las asignaciones y las operaciones quedan sin módulo, sin perder su autoridad.
     */
    @Test
    void eliminarModuloSueltaLasAsignacionesAntesDeBorrarElModulo() throws EntityNotFoundException {
        when(repository.obtenerModuloPorId("c1", 1L))
                .thenReturn(Module.builder().idCompany("c1").idModule(1L).name("USUARIOS").build());

        service().eliminarModulo("c1", 1L);

        InOrder orden = inOrder(moduleOperationRepository, repository);
        orden.verify(moduleOperationRepository).eliminarAsignacionesDeModulo("c1", 1L);
        orden.verify(repository).eliminarModulo("c1", 1L);
    }

    @Test
    void eliminarModuloPropagaEntityNotFoundExceptionSiElModuloNoExiste() throws EntityNotFoundException {
        when(repository.obtenerModuloPorId("c1", 99L)).thenThrow(new EntityNotFoundException("no existe"));

        assertThatThrownBy(() -> service().eliminarModulo("c1", 99L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void obtenerModuloPorIdDelegaEnElRepositorio() throws EntityNotFoundException {
        Module module = Module.builder().idCompany("c1").idModule(1L).name("USUARIOS").build();
        when(repository.obtenerModuloPorId("c1", 1L)).thenReturn(module);

        assertThat(service().obtenerModuloPorId("c1", 1L)).isEqualTo(module);
    }

    @Test
    void listarModulosPorStateDelegaEnElRepositorio() {
        Module module = Module.builder().idCompany("c1").idModule(1L).name("USUARIOS").state(true).build();
        when(repository.listarModulosPorState("c1", true)).thenReturn(List.of(module));

        assertThat(service().listarModulosPorState("c1", true)).containsExactly(module);
    }

    @Test
    void listarModulosPaginadosDelegaEnElRepositorio() {
        Module module = Module.builder().idCompany("c1").idModule(1L).name("USUARIOS").build();
        PageQuery pageQuery = new PageQuery(0, 10, List.of());
        PagedResult<Module> expected = new PagedResult<>(List.of(module), 0, 10, 1);
        when(repository.listarModulosPaginados("c1", List.of(), pageQuery)).thenReturn(expected);

        assertThat(service().listarModulosPaginados("c1", List.of(), pageQuery)).isEqualTo(expected);
    }
}
