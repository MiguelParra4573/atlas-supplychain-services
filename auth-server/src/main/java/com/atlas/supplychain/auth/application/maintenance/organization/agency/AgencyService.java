package com.atlas.supplychain.auth.application.maintenance.organization.agency;

import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.maintenance.organization.model.Agency;
import com.atlas.supplychain.auth.domain.shared.PageQuery;
import com.atlas.supplychain.auth.domain.shared.PagedResult;
import com.atlas.supplychain.auth.domain.shared.query.FilterCriterion;

import java.util.List;

public interface AgencyService {

    void crearAgency(Agency agency) throws EntityNotFoundException;

    void actualizarAgency(Agency agency) throws EntityNotFoundException;

    void eliminarAgency(String idCompany, String idAgency) throws EntityNotFoundException;

    Agency obtenerAgencyPorId(String idCompany, String idAgency) throws EntityNotFoundException;

    List<Agency> listarAgenciesPorCompany(String idCompany);

    PagedResult<Agency> listarAgenciesPaginadas(String idCompany, List<FilterCriterion> filters, PageQuery pageQuery);
}
