package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.security.PasswordHistory;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.security.PasswordHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PasswordHistoryMapper {

    PasswordHistory entityToDomain(PasswordHistoryEntity entity);

    PasswordHistoryEntity domainToEntity(PasswordHistory domain);

}
