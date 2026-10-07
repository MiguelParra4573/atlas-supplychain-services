package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.UserEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.UserDTO;
import org.mapstruct.*;


@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(source = "roleEntity", target = "role")
    User entityToDomain(UserEntity entity);

    @Mapping(source = "role", target = "roleEntity")
    UserEntity domainToEntity(User domain);

    // idCompany no se confía del cliente -- se recibe aparte (del principal), no del DTO
    @Mapping(source = "dto.idRole", target = "role.idRol")
    @Mapping(source = "idCompany", target = "idCompany")
    User dtoToDomain(UserDTO dto, String idCompany);

    @Mapping(source = "role.idRol", target = "idRole")
    UserDTO domainToDto(User domain);

    @Mapping(target = "roleEntity", source = "role")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(User domain, @MappingTarget UserEntity entity);

}
