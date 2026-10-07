package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.ProcessStage;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ProcessStageEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ProcessStageDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProcessStageMapper {

    ProcessStage entityToDomain(ProcessStageEntity entity);

    ProcessStageEntity domainToEntity(ProcessStage domain);

    ProcessStageDTO domainToDto(ProcessStage domain);

    ProcessStage dtoToDomain(ProcessStageDTO dto);
}
