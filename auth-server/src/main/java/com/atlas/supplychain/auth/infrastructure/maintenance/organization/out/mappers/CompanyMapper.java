package com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.organization.model.Company;
import com.atlas.supplychain.auth.infrastructure.maintenance.organization.entities.CompanyEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.organization.CompanyDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    Company entityToDomain(CompanyEntity entity);

    CompanyEntity domainToEntity(Company domain);

    Company dtoToDomain(CompanyDTO dto);

    CompanyDTO domainToDto(Company domain);
}
