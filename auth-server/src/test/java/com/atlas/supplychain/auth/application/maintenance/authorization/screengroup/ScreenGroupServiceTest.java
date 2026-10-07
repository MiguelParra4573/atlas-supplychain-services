package com.atlas.supplychain.auth.application.maintenance.authorization.screengroup;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ScreenGroup;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ModuleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.ScreenGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreenGroupServiceTest {

    @Mock
    private ScreenGroupRepository repository;
    @Mock
    private ModuleRepository moduleRepository;

    private ScreenGroupServiceImpl service() {
        return new ScreenGroupServiceImpl(repository, moduleRepository);
    }

    @Test
    void crearVerificaQueElModuloExistaYFijaDisplayOrderEnCeroSiNoViene() throws EntityNotFoundException {
        ScreenGroup group = ScreenGroup.builder().idCompany("c1").idModule(7L).name("Solicitudes").build();
        when(moduleRepository.obtenerModuloPorId("c1", 7L))
                .thenReturn(Module.builder().idCompany("c1").idModule(7L).name("Nómina").build());

        service().crear(group);

        assertThat(group.getDisplayOrder()).isZero();
        verify(repository).crear(group);
    }

    @Test
    void crearPropagaEntityNotFoundExceptionSiElModuloNoExiste() throws EntityNotFoundException {
        ScreenGroup group = ScreenGroup.builder().idCompany("c1").idModule(99L).name("Solicitudes").build();
        when(moduleRepository.obtenerModuloPorId("c1", 99L)).thenThrow(new EntityNotFoundException("no existe"));

        assertThatThrownBy(() -> service().crear(group)).isInstanceOf(EntityNotFoundException.class);
        verify(repository, never()).crear(group);
    }

    @Test
    void actualizarDelegaEnElRepositorio() throws EntityNotFoundException {
        ScreenGroup group = ScreenGroup.builder().idCompany("c1").idScreenGroup(1L).idModule(7L).name("Aprobaciones").build();

        service().actualizar(group);

        verify(repository).actualizar(group);
    }

    @Test
    void eliminarDelegaEnElRepositorio() throws EntityNotFoundException {
        service().eliminar("c1", 1L);

        verify(repository).eliminar("c1", 1L);
    }

    @Test
    void listarPorModuloVerificaQueElModuloExistaYDelegaEnElRepositorio() throws EntityNotFoundException {
        ScreenGroup group = ScreenGroup.builder().idCompany("c1").idScreenGroup(1L).idModule(7L).name("Solicitudes").build();
        when(moduleRepository.obtenerModuloPorId("c1", 7L))
                .thenReturn(Module.builder().idCompany("c1").idModule(7L).name("Nómina").build());
        when(repository.listarPorModulo("c1", 7L)).thenReturn(List.of(group));

        assertThat(service().listarPorModulo("c1", 7L)).containsExactly(group);
    }
}
