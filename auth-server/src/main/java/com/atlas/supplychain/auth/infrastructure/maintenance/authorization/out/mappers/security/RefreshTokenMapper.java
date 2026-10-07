package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.RefreshToken;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security.RefreshTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {

    RefreshToken entityToDomain(RefreshTokenEntity entity);

    RefreshTokenEntity domainToEntity(RefreshToken domain);

}
