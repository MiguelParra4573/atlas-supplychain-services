package com.atlas.supplychain.auth.application.maintenance.authorization.menucategory;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.MenuCategory;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.MenuCategoryRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuCategoryServiceTest {

    @Mock
    private MenuCategoryRepository repository;

    private MenuCategoryServiceImpl service() {
        return new MenuCategoryServiceImpl(repository);
    }

    @Test
    void crearFijaDisplayOrderEnCeroSiNoViene() throws DuplicateRecordException {
        MenuCategory category = MenuCategory.builder().idCompany("c1").code("MANTENIMIENTO").name("Mantenimiento").build();
        when(repository.existsByCode("c1", "MANTENIMIENTO")).thenReturn(false);

        service().crear(category);

        assertThat(category.getDisplayOrder()).isZero();
        verify(repository).crear(category);
    }

    @Test
    void crearLanzaDuplicateRecordExceptionSiElCodigoYaExisteEnLaEmpresa() {
        MenuCategory category = MenuCategory.builder().idCompany("c1").code("MANTENIMIENTO").name("Mantenimiento").build();
        when(repository.existsByCode("c1", "MANTENIMIENTO")).thenReturn(true);

        assertThatThrownBy(() -> service().crear(category)).isInstanceOf(DuplicateRecordException.class);
        verify(repository, never()).crear(category);
    }

    @Test
    void actualizarNoValidaUnicidadSiElCodigoNoCambio() throws EntityNotFoundException, DuplicateRecordException {
        MenuCategory existente = MenuCategory.builder().idCompany("c1").idMenuCategory(1L).code("MANTENIMIENTO").name("Mantenimiento").build();
        MenuCategory actualizada = MenuCategory.builder().idCompany("c1").idMenuCategory(1L).code("MANTENIMIENTO").name("Mantenimiento (renombrada)").build();
        when(repository.obtenerPorId("c1", 1L)).thenReturn(existente);

        service().actualizar(actualizada);

        verify(repository, never()).existsByCode(any(), any());
        verify(repository).actualizar(actualizada);
    }

    @Test
    void actualizarValidaUnicidadSiElCodigoCambio() throws EntityNotFoundException {
        MenuCategory existente = MenuCategory.builder().idCompany("c1").idMenuCategory(1L).code("MANTENIMIENTO").name("Mantenimiento").build();
        MenuCategory actualizada = MenuCategory.builder().idCompany("c1").idMenuCategory(1L).code("PROCESO").name("Procesos").build();
        when(repository.obtenerPorId("c1", 1L)).thenReturn(existente);
        when(repository.existsByCode("c1", "PROCESO")).thenReturn(true);

        assertThatThrownBy(() -> service().actualizar(actualizada)).isInstanceOf(DuplicateRecordException.class);
    }

    @Test
    void listarPorStateDelegaEnElRepositorio() {
        MenuCategory category = MenuCategory.builder().idCompany("c1").idMenuCategory(1L).code("MANTENIMIENTO").name("Mantenimiento").build();
        when(repository.listarPorState("c1", true)).thenReturn(List.of(category));

        assertThat(service().listarPorState("c1", true)).containsExactly(category);
    }

    @Test
    void listarPaginadoDelegaEnElRepositorio() {
        MenuCategory category = MenuCategory.builder().idCompany("c1").idMenuCategory(1L).code("MANTENIMIENTO").name("Mantenimiento").build();
        PageQuery pageQuery = new PageQuery(0, 10, List.of());
        PagedResult<MenuCategory> expected = new PagedResult<>(List.of(category), 0, 10, 1);
        when(repository.listarPaginado("c1", List.of(), pageQuery)).thenReturn(expected);

        assertThat(service().listarPaginado("c1", List.of(), pageQuery)).isEqualTo(expected);
    }
}
