package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Role;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.RoleEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.RoleDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = { GrantedPermissionMapper.class })
public interface RoleMapper {

    @Mapping(source = "grantedPermissionEntities", target = "grantedPermissions")
    Role entityToDomain(RoleEntity entity);

    @Mapping(source = "grantedPermissions", target = "grantedPermissionEntities")
    RoleEntity domainToEntity(Role domain);

    // idCompany no se confía del cliente -- se recibe aparte (del principal), no del DTO
    @Mapping(target = "grantedPermissions", ignore = true)
    @Mapping(source = "dto.idRole", target = "idRol")
    @Mapping(source = "idCompany", target = "idCompany")
    Role dtoToDomain(RoleDTO dto, String idCompany);

    @Mapping(source = "idRol", target = "idRole")
    RoleDTO domainToDto(Role domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "grantedPermissionEntities", source = "grantedPermissions")
    void update(Role domain, @MappingTarget RoleEntity entity);
}
