package com.atlas.supplychain.auth.application.maintenance.organization.company;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Company;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;

public interface CompanyService {

    void crearCompany(Company company);

    void actualizarCompany(Company company) throws EntityNotFoundException;

    void eliminarCompany(String idCompany) throws EntityNotFoundException;

    Company obtenerCompanyPorId(String idCompany) throws EntityNotFoundException;

    List<Company> listarCompaniesPorState(boolean state);

    PagedResult<Company> listarCompaniesPaginadas(List<FilterCriterion> filters, PageQuery pageQuery);
}
