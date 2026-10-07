package com.atlas.supplychain.auth.application.maintenance.organization.company;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.common.exceptions.ExistingDependenciesException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Company;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.AgencyRepository;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.CompanyRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.RoleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
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
class CompanyServiceTest {

    @Mock
    private CompanyRepository repository;
    @Mock
    private AgencyRepository agencyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;

    private CompanyServiceImpl service() {
        return new CompanyServiceImpl(repository, agencyRepository, userRepository, roleRepository);
    }

    private Company company() {
        return Company.builder()
                .idCompany("01")
                .alias("acme")
                .ruc("1234567890001")
                .businessName("Acme SA")
                .basePath("/acme")
                .cedulaRepresLegal("1234567890")
                .legalRepFirstName("Juan")
                .legalRepLastName("Perez")
                .state(true)
                .build();
    }

    @Test
    void crearCompanyCreaCuandoAliasYRucNoExisten() {
        Company company = company();
        when(repository.existsByAlias("acme")).thenReturn(false);
        when(repository.existsByRuc("1234567890001")).thenReturn(false);

        service().crearCompany(company);

        verify(repository).crearCompany(company);
    }

    @Test
    void crearCompanyLanzaDuplicateRecordExceptionSiElAliasYaExiste() {
        when(repository.existsByAlias("acme")).thenReturn(true);

        assertThatThrownBy(() -> service().crearCompany(company())).isInstanceOf(DuplicateRecordException.class);

        verify(repository, never()).crearCompany(any());
    }

    @Test
    void crearCompanyLanzaDuplicateRecordExceptionSiElRucYaExiste() {
        when(repository.existsByAlias("acme")).thenReturn(false);
        when(repository.existsByRuc("1234567890001")).thenReturn(true);

        assertThatThrownBy(() -> service().crearCompany(company())).isInstanceOf(DuplicateRecordException.class);

        verify(repository, never()).crearCompany(any());
    }

    @Test
    void actualizarCompanyVerificaExistenciaPrevia() throws EntityNotFoundException {
        Company company = company();
        when(repository.obtenerCompanyPorId("01")).thenReturn(company);

        service().actualizarCompany(company);

        verify(repository).actualizarCompany(company);
    }

    @Test
    void eliminarCompanyEliminaCuandoNoTieneDependencias() throws EntityNotFoundException {
        when(repository.obtenerCompanyPorId("01")).thenReturn(company());
        when(agencyRepository.existsByCompany("01")).thenReturn(false);
        when(userRepository.listarUsuariosPorCompany("01")).thenReturn(List.<User>of());
        when(roleRepository.listarRolesPorCompany("01")).thenReturn(List.<Role>of());

        service().eliminarCompany("01");

        verify(repository).eliminarCompany("01");
    }

    @Test
    void eliminarCompanyLanzaExistingDependenciesExceptionSiTieneAgenciasAsociadas() throws EntityNotFoundException {
        when(repository.obtenerCompanyPorId("01")).thenReturn(company());
        when(agencyRepository.existsByCompany("01")).thenReturn(true);

        assertThatThrownBy(() -> service().eliminarCompany("01")).isInstanceOf(ExistingDependenciesException.class);

        verify(repository, never()).eliminarCompany("01");
    }

    @Test
    void eliminarCompanyLanzaExistingDependenciesExceptionSiTieneUsuariosAsociados() throws EntityNotFoundException {
        when(repository.obtenerCompanyPorId("01")).thenReturn(company());
        when(agencyRepository.existsByCompany("01")).thenReturn(false);
        when(userRepository.listarUsuariosPorCompany("01")).thenReturn(List.of(new User()));

        assertThatThrownBy(() -> service().eliminarCompany("01")).isInstanceOf(ExistingDependenciesException.class);

        verify(repository, never()).eliminarCompany("01");
    }

    @Test
    void obtenerCompanyPorIdDelegaEnElRepositorio() throws EntityNotFoundException {
        Company company = company();
        when(repository.obtenerCompanyPorId("01")).thenReturn(company);

        assertThat(service().obtenerCompanyPorId("01")).isEqualTo(company);
    }

    @Test
    void listarCompaniesPorStateDelegaEnElRepositorio() {
        Company company = company();
        when(repository.listarCompaniesPorState(true)).thenReturn(List.of(company));

        assertThat(service().listarCompaniesPorState(true)).containsExactly(company);
    }

    @Test
    void listarCompaniesPaginadasDelegaEnElRepositorio() {
        Company company = company();
        PageQuery pageQuery = new PageQuery(0, 10, List.of());
        PagedResult<Company> expected = new PagedResult<>(List.of(company), 0, 10, 1);
        when(repository.listarCompaniesPaginadas(List.of(), pageQuery)).thenReturn(expected);

        assertThat(service().listarCompaniesPaginadas(List.of(), pageQuery)).isEqualTo(expected);
    }
}
