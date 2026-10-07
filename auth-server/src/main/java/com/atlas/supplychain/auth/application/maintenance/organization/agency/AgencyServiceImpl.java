package com.atlas.supplychain.auth.application.maintenance.organization.agency;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Agency;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.AgencyRepository;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.CompanyRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Mismo patrón que {@code RoleServiceImpl} (Spec 1.2.2): {@code Agency} lleva {@code idCompany}
 * obligatorio del tenant, a diferencia de la raíz {@code Company} (sin filtro).
 */
@RequiredArgsConstructor
public class AgencyServiceImpl implements AgencyService {

    private final AgencyRepository repository;
    private final CompanyRepository companyRepository;

    @Override
    public void crearAgency(Agency agency) throws EntityNotFoundException {
        requerirCompanyExistente(agency.getIdCompany());
        // idAgency (varchar(36)) no lo especifica la spec como código corto -- se genera acá,
        // igual que el resto de PK varchar(36) del proyecto (idUser, idRole no aplica por ser
        // Long), en vez de en el RepositoryImpl: a diferencia del idCompany de Company (Spec
        // 1.17 exige un roundtrip a la secuencia de BD), un UUID no necesita tocar la BD antes
        // de persistir.
        agency.setIdAgency(UUID.randomUUID().toString());
        repository.crearAgency(agency);
    }

    @Override
    public void actualizarAgency(Agency agency) throws EntityNotFoundException {
        // Confirma existencia Y pertenencia al tenant: obtenerAgencyPorId filtra por
        // idCompany, así que una agencia de otra empresa da EntityNotFoundException acá,
        // no una edición cruzada de tenant.
        repository.obtenerAgencyPorId(agency.getIdCompany(), agency.getIdAgency());
        repository.actualizarAgency(agency);
    }

    @Override
    public void eliminarAgency(String idCompany, String idAgency) throws EntityNotFoundException {
        repository.obtenerAgencyPorId(idCompany, idAgency);
        repository.eliminarAgency(idCompany, idAgency);
    }

    @Override
    public Agency obtenerAgencyPorId(String idCompany, String idAgency) throws EntityNotFoundException {
        return repository.obtenerAgencyPorId(idCompany, idAgency);
    }

    @Override
    public List<Agency> listarAgenciesPorCompany(String idCompany) {
        return repository.listarAgenciesPorCompany(idCompany);
    }

    @Override
    public PagedResult<Agency> listarAgenciesPaginadas(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery) {
        return repository.listarAgenciesPaginadas(idCompany, filters, pageQuery);
    }

    private void requerirCompanyExistente(String idCompany) throws EntityNotFoundException {
        companyRepository.obtenerCompanyPorId(idCompany);
    }

}
