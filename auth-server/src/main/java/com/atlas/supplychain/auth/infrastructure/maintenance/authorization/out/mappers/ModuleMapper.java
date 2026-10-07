package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.ModuleEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.ModuleDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = { OperationMapper.class })
public interface ModuleMapper {

    // ENTITY → DOMAIN
    // No mapear operationSet: Operation referencia de vuelta a Module (N:M desde la Spec 1.2) y
    // mapear los dos lados dispararía la cascada en círculo.
    // processActive no vive en la tabla: lo resuelve UserRepositoryImpl contra company_process al
    // cargar el agregado del usuario (Spec 1.0). Acá se asume activo -- quien necesite el valor
    // real lo pisa; el resto del sistema no depende de procesos.
    @Mapping(target = "operationSet", ignore = true)
    @Mapping(target = "processActive", constant = "true")
    Module entityToDomain(ModuleEntity entity);

    // DOMAIN → ENTITY
    @Mapping(target = "operationEntitySet", ignore = true)
    ModuleEntity domainToEntity(Module domain);

    // DTO → DOMAIN -- idCompany no se confía del cliente, se recibe aparte (del principal)
    @Mapping(target = "operationSet", ignore = true) // igual acá, evita el ciclo
    @Mapping(target = "processActive", constant = "true")
    @Mapping(source = "idCompany", target = "idCompany")
    Module dtoToDomain(ModuleDTO dto, String idCompany);

    // DOMAIN → DTO
    ModuleDTO domainToDto(Module domain);

    // UPDATE
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "operationEntitySet", ignore = true)
    void update(Module domain, @MappingTarget ModuleEntity entity);
}

