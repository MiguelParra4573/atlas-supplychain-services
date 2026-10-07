package com.atlas.supplychain.auth.application.maintenance.organization.agency;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Agency;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Company;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.AgencyRepository;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.CompanyRepository;
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
class AgencyServiceTest {

    @Mock
    private AgencyRepository repository;
    @Mock
    private CompanyRepository companyRepository;

    private AgencyServiceImpl service() {
        return new AgencyServiceImpl(repository, companyRepository);
    }

    private Agency agency() {
        return Agency.builder().idAgency("a1").idCompany("01").name("Agencia Centro").state(true).build();
    }

    @Test
    void crearAgencyCreaCuandoLaCompanyExiste() throws EntityNotFoundException {
        Agency agency = agency();
        when(companyRepository.obtenerCompanyPorId("01")).thenReturn(Company.builder().idCompany("01").build());

        service().crearAgency(agency);

        assertThat(agency.getIdAgency()).isNotBlank();
        verify(repository).crearAgency(agency);
    }

    @Test
    void crearAgencyLanzaEntityNotFoundExceptionSiLaCompanyNoExiste() throws EntityNotFoundException {
        when(companyRepository.obtenerCompanyPorId("99"))
                .thenThrow(new EntityNotFoundException("Empresa no encontrada ID 99"));

        Agency agency = Agency.builder().idCompany("99").name("Agencia Huérfana").state(true).build();

        assertThatThrownBy(() -> service().crearAgency(agency)).isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).crearAgency(any());
    }

    @Test
    void actualizarAgencyVerificaExistenciaYPertenenciaAlTenant() throws EntityNotFoundException {
        Agency agency = agency();
        when(repository.obtenerAgencyPorId("01", "a1")).thenReturn(agency);

        service().actualizarAgency(agency);

        verify(repository).actualizarAgency(agency);
    }

    @Test
    void actualizarAgencyLanzaEntityNotFoundExceptionSiEsDeOtraEmpresa() throws EntityNotFoundException {
        Agency agency = agency();
        when(repository.obtenerAgencyPorId("01", "a1"))
                .thenThrow(new EntityNotFoundException("Agencia no encontrada ID a1"));

        assertThatThrownBy(() -> service().actualizarAgency(agency)).isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).actualizarAgency(any());
    }

    @Test
    void eliminarAgencyEliminaCuandoExiste() throws EntityNotFoundException {
        when(repository.obtenerAgencyPorId("01", "a1")).thenReturn(agency());

        service().eliminarAgency("01", "a1");

        verify(repository).eliminarAgency("01", "a1");
    }

    @Test
    void obtenerAgencyPorIdDelegaEnElRepositorio() throws EntityNotFoundException {
        Agency agency = agency();
        when(repository.obtenerAgencyPorId("01", "a1")).thenReturn(agency);

        assertThat(service().obtenerAgencyPorId("01", "a1")).isEqualTo(agency);
    }

    @Test
    void listarAgenciesPorCompanyDelegaEnElRepositorio() {
        Agency agency = agency();
        when(repository.listarAgenciesPorCompany("01")).thenReturn(List.of(agency));

        assertThat(service().listarAgenciesPorCompany("01")).containsExactly(agency);
    }

    @Test
    void listarAgenciesPaginadasDelegaEnElRepositorio() {
        Agency agency = agency();
        PageQuery pageQuery = new PageQuery(0, 10, List.of());
        PagedResult<Agency> expected = new PagedResult<>(List.of(agency), 0, 10, 1);
        when(repository.listarAgenciesPaginadas("01", List.of(), pageQuery)).thenReturn(expected);

        assertThat(service().listarAgenciesPaginadas("01", List.of(), pageQuery)).isEqualTo(expected);
    }
}
