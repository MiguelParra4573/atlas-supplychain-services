package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.MenuCategory;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.MenuCategoryEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.MenuCategoryDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface MenuCategoryMapper {

    MenuCategory entityToDomain(MenuCategoryEntity entity);

    MenuCategoryEntity domainToEntity(MenuCategory domain);

    @Mapping(source = "idCompany", target = "idCompany")
    MenuCategory dtoToDomain(MenuCategoryDTO dto, String idCompany);

    MenuCategoryDTO domainToDto(MenuCategory domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(MenuCategory domain, @MappingTarget MenuCategoryEntity entity);
}
