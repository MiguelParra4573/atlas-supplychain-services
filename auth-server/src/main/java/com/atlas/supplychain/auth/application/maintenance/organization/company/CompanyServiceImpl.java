package com.atlas.supplychain.auth.application.maintenance.organization.company;

import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.common.exceptions.ExistingDependenciesException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Company;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.AgencyRepository;
import com.atlas.supplychain.auth.domain.maintenance.organization.repositories.CompanyRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.RoleRepository;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * {@code Company} es la raíz del árbol de tenant (Spec 1.17) -- a diferencia de
 * {@code Agency}, sus lecturas no llevan filtro de {@code idCompany}: no hay
 * bajo qué empresa filtrar la tabla que define las empresas.
 */
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository repository;
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public void crearCompany(Company company) {
        if (repository.existsByAlias(company.getAlias())) {
            throw new DuplicateRecordException("Ya existe una empresa con alias '%s'".formatted(company.getAlias()));
        }
        if (repository.existsByRuc(company.getRuc())) {
            throw new DuplicateRecordException("Ya existe una empresa con RUC '%s'".formatted(company.getRuc()));
        }
        // idCompany se genera en CompanyRepositoryImpl#crearCompany (secuencia
        // auth.company_id_seq formateada a 2 dígitos) -- no se asigna acá.
        repository.crearCompany(company);
    }

    @Override
    public void actualizarCompany(Company company) throws EntityNotFoundException {
        // idCompany es inmutable una vez creada (es la clave que ya replican otras 7
        // tablas) -- obtenerCompanyPorId confirma que la empresa referenciada existe;
        // actualizarCompany nunca reasigna el id, solo los campos restantes.
        repository.obtenerCompanyPorId(company.getIdCompany());
        repository.actualizarCompany(company);
    }

    @Override
    public void eliminarCompany(String idCompany) throws EntityNotFoundException {
        repository.obtenerCompanyPorId(idCompany);

        if (agencyRepository.existsByCompany(idCompany)) {
            throw new ExistingDependenciesException(
                    "No se puede eliminar la empresa %s: tiene agencias asociadas".formatted(idCompany));
        }
        if (!userRepository.listarUsuariosPorCompany(idCompany).isEmpty()) {
            throw new ExistingDependenciesException(
                    "No se puede eliminar la empresa %s: tiene usuarios asociados".formatted(idCompany));
        }
        if (!roleRepository.listarRolesPorCompany(idCompany).isEmpty()) {
            throw new ExistingDependenciesException(
                    "No se puede eliminar la empresa %s: tiene roles asociados".formatted(idCompany));
        }

        repository.eliminarCompany(idCompany);
    }

    @Override
    public Company obtenerCompanyPorId(String idCompany) throws EntityNotFoundException {
        return repository.obtenerCompanyPorId(idCompany);
    }

    @Override
    public List<Company> listarCompaniesPorState(boolean state) {
        return repository.listarCompaniesPorState(state);
    }

    @Override
    public PagedResult<Company> listarCompaniesPaginadas(List<FilterCriterion> filters, PageQuery pageQuery) {
        return repository.listarCompaniesPaginadas(filters, pageQuery);
    }
}
