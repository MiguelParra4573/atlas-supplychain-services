package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.BusinessProcess;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.CompanyProcess;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.BusinessProcessEntity;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.CompanyProcessEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.BusinessProcessDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BusinessProcessMapper {

    BusinessProcess entityToDomain(BusinessProcessEntity entity);

    BusinessProcessEntity domainToEntity(BusinessProcess domain);

    CompanyProcess entityToDomain(CompanyProcessEntity entity);

    // "active" no vive en el dominio (ver Javadoc de BusinessProcess): solo aparece en la
    // respuesta de "procesos de mi empresa" y lo pone BusinessProcessController a mano con
    // el CompanyProcess de la empresa que pide.
    @Mapping(target = "active", ignore = true)
    BusinessProcessDTO domainToDto(BusinessProcess domain);

    BusinessProcess dtoToDomain(BusinessProcessDTO dto);
}
