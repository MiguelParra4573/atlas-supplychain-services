package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.PasswordResetToken;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security.PasswordResetTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PasswordResetTokenMapper {

    PasswordResetToken entityToDomain(PasswordResetTokenEntity entity);

    PasswordResetTokenEntity domainToEntity(PasswordResetToken domain);

}
