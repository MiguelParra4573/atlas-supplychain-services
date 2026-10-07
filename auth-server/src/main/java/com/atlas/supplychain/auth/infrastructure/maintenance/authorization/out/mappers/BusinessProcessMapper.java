package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.BusinessProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.CompanyProcess;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.BusinessProcessEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.CompanyProcessEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.BusinessProcessDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BusinessProcessMapper {

    BusinessProcess entityToDomain(BusinessProcessEntity entity);

    BusinessProcessEntity domainToEntity(BusinessProcess domain);

    CompanyProcess entityToDomain(CompanyProcessEntity entity);

    BusinessProcessDTO domainToDto(BusinessProcess domain);

    BusinessProcess dtoToDomain(BusinessProcessDTO dto);
}
