package com.atlas.supplychain.auth.infrastructure.maintenance.organization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.organization.model.Agency;
import com.atlas.supplychain.auth.infrastructure.maintenance.organization.entities.AgencyEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.organization.AgencyDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AgencyMapper {

    Agency entityToDomain(AgencyEntity entity);

    AgencyEntity domainToEntity(Agency domain);

    @Mapping(source = "idCompany", target = "idCompany")
    Agency dtoToDomain(AgencyDTO dto, String idCompany);

    AgencyDTO domainToDto(Agency domain);
}
