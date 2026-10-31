package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.UserEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.UserDTO;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(source = "roleEntity", target = "role")
    User entityToDomain(UserEntity entity);

    @Mapping(source = "role", target = "roleEntity")
    UserEntity domainToEntity(User domain);

    // idCompany no se confía del cliente -- se recibe aparte (del principal), no del DTO.
    // passwordHash, passwordChangedAt, failedAttempts, lockUntil y passwordExpiresAt tampoco
    // se confían del cliente: los administra el servicio de autenticación, nunca este DTO.
    @Mapping(source = "dto.idRole", target = "role.idRol")
    @Mapping(source = "idCompany", target = "idCompany")
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "failedAttempts", ignore = true)
    @Mapping(target = "lockUntil", ignore = true)
    @Mapping(target = "passwordExpiresAt", ignore = true)
    User dtoToDomain(UserDTO dto, String idCompany);

    // idCompny: nombre de campo heredado del DTO (ver UserDTO), no una propiedad de User.
    // password nunca sale en una respuesta: el DTO lo lleva solo para altas/cambios de clave.
    @Mapping(source = "role.idRol", target = "idRole")
    @Mapping(target = "idCompny", ignore = true)
    @Mapping(target = "password", ignore = true)
    UserDTO domainToDto(User domain);

    @Mapping(target = "roleEntity", source = "role")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(User domain, @MappingTarget UserEntity entity);
}
