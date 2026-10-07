package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.OperationEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.OperationDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = { ModuleMapper.class })
public interface OperationMapper {

    // OJO: este método también lo usa GrantedPermissionMapper (vía `uses`, desambiguado con
    // @Named porque hay 2 métodos OperationEntity->Operation acá) para construir el agregado
    // User -> Role -> GrantedPermission -> Operation que consume `User.esVigente` -- ESE path SÍ
    // necesita `modules` poblado (corta acceso si el módulo está desactivado). No ignorar
    // `modules` acá -- ver `entityToDomainParaListado` para los repositorios de listado, que no
    // necesitan la asignación completa en cada fila.
    @Named("operationEntityToDomainConModulos")
    Operation entityToDomain(OperationEntity entity);

    /**
     * Variante para listados masivos (search paginado, `listarPorRecurso`, etc.): ignora
     * {@code modules} a propósito. Es {@code @ManyToMany(LAZY)} sin {@code @BatchSize}/
     * {@code @EntityGraph} -- mapearlo en {@link #entityToDomain} dispara una query extra **por
     * fila** (N+1 confirmado). Quien necesite la asignación completa la pide explícito
     * ({@code GET /operations/{id}/modules}, o la proyección de pantallas del buscador nuevo).
     */
    @Mapping(target = "modules", ignore = true)
    Operation entityToDomainParaListado(OperationEntity entity);

    // `modules` es navegación de solo lectura sobre module_operation (Spec 1.2): escribirla desde
    // acá no haría nada en la base y solo dejaría entidades desprendidas dando vueltas.
    @Mapping(target = "modules", ignore = true)
    OperationEntity domainToEntity(Operation domain);

    // idCompany no se confía del cliente -- se recibe aparte (del principal), no del DTO.
    // `modules` es solo de salida: la asignación tiene sus propios endpoints.
    @Mapping(source = "idCompany", target = "idCompany")
    @Mapping(target = "modules", ignore = true)
    Operation dtoToDomain(OperationDTO dto, String idCompany);

    OperationDTO domainToDto(Operation domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "modules", ignore = true)
    void update(Operation domain, @MappingTarget OperationEntity entity);
}
