package com.atlas.supplychain.auth.application.maintenance.authorization.businessprocess;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.supplychain.auth.application.audit.contracts.AuditTrail;
import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.BusinessProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.BusinessProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessProcessServiceTest {

    @Mock private BusinessProcessRepository repository;
    @Mock private AuditTrail auditTrail;
    @Mock private CurrentActor currentActor;

    private BusinessProcessServiceImpl service() {
        return new BusinessProcessServiceImpl(repository, auditTrail, currentActor);
    }

    /** El proceso tal como está guardado hoy en la base: id 1, codigo VENTAS. */
    private BusinessProcess guardado() {
        return BusinessProcess.builder()
                .idBusinessProcess(1L)
                .code("VENTAS")
                .name("Ventas")
                .state(true)
                .build();
    }

    // Escenario 2: el codigo nuevo ya lo usa otro proceso. Resuelto como modelo.
    @Test
    void actualizarLanzaDuplicateRecordExceptionSiElCodigoNuevoYaLoUsaOtroProceso()
            throws EntityNotFoundException {
        BusinessProcessServiceImpl service = service();
        BusinessProcess cambios =
                BusinessProcess.builder()
                        .idBusinessProcess(1L)
                        .code("TALLER")
                        .name("Ventas")
                        .state(true)
                        .build();
        when(repository.obtenerPorId(1L)).thenReturn(guardado());
        when(repository.existsByCode("TALLER")).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(cambios))
                .isInstanceOf(DuplicateRecordException.class)
                .hasMessageContaining("TALLER");

        verify(repository, never()).actualizar(any());
    }

    // Escenario 1: el codigo no cambia. El cortocircuito del && evita la consulta de duplicados,
    // que de otro modo encontraria el codigo del propio registro y rechazaria la actualizacion.
    @Test
    void actualizarGuardaSinConsultarDuplicadosSiElCodigoNoCambia() throws EntityNotFoundException {
        BusinessProcessServiceImpl service = service();
        BusinessProcess cambios =
                BusinessProcess.builder()
                        .idBusinessProcess(1L)
                        .code("VENTAS")
                        .name("Ventas y posventa")
                        .state(true)
                        .build();
        when(repository.obtenerPorId(1L)).thenReturn(guardado());

        service.actualizar(cambios);

        verify(repository).actualizar(cambios);
        verify(repository, never()).existsByCode(any());
    }

    // Escenario 3: el codigo cambia a uno libre. Se evaluan las dos mitades del && y aun asi no
    // hay duplicado, asi que guarda.
    @Test
    void actualizarGuardaSiElCodigoNuevoNoLoUsaNadie() throws EntityNotFoundException {
        BusinessProcessServiceImpl service = service();
        BusinessProcess cambios =
                BusinessProcess.builder()
                        .idBusinessProcess(1L)
                        .code("COMPRAS")
                        .name("Ventas")
                        .state(true)
                        .build();
        when(repository.obtenerPorId(1L)).thenReturn(guardado());
        when(repository.existsByCode("COMPRAS")).thenReturn(false);

        service.actualizar(cambios);

        verify(repository).actualizar(cambios);
    }
}
