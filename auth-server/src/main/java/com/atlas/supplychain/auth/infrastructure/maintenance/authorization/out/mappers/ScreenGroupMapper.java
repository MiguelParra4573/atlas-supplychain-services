package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ScreenGroup;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ScreenGroupEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ScreenGroupDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ScreenGroupMapper {

    ScreenGroup entityToDomain(ScreenGroupEntity entity);

    ScreenGroupEntity domainToEntity(ScreenGroup domain);

    @Mapping(source = "idCompany", target = "idCompany")
    @Mapping(source = "idModule", target = "idModule")
    ScreenGroup dtoToDomain(ScreenGroupDTO dto, String idCompany, Long idModule);

    ScreenGroupDTO domainToDto(ScreenGroup domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(ScreenGroup domain, @MappingTarget ScreenGroupEntity entity);
}
