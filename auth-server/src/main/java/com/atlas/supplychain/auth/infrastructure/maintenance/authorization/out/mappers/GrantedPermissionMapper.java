package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.infrastructure.maintenance.authorization.entities.GrantedPermissionEntity;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.GrantedPermissionDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = { OperationMapper.class })
public interface GrantedPermissionMapper {

    // ENTITY → DOMAIN
    // `qualifiedByName` obligatorio: OperationMapper tiene 2 métodos OperationEntity->Operation
    // (la variante con modules y la de listados sin modules) -- este path (User.esVigente) SÍ
    // necesita `modules` poblado, ver el comentario en OperationMapper#entityToDomain.
    @Mapping(target = "role", ignore = true) // 👈 no mapear el Role para evitar ciclo
    @Mapping(source = "operationEntity", target = "operation", qualifiedByName = "operationEntityToDomainConModulos")
    GrantedPermission entityToDomain(GrantedPermissionEntity entity);

    // DOMAIN → ENTITY
    @Mapping(target = "roleEntity", ignore = true) // 👈 igual aquí
    @Mapping(source = "operation", target = "operationEntity")
    GrantedPermissionEntity domainToEntity(GrantedPermission domain);

    // DTO → DOMAIN -- idCompany no se confía del cliente, se recibe aparte (del principal)
    @Mapping(target = "role", ignore = true) // si quieres luego rellenar el role por id, lo haces a mano
    @Mapping(source = "dto.idOperation", target = "operation.idOperation")
    @Mapping(source = "idCompany", target = "idCompany")
    GrantedPermission dtoToDomain(GrantedPermissionDTO dto, String idCompany);

    // DOMAIN → DTO -- nombres solo para lectura (pantallas de administracion), el domain llega
    // completo aca porque el repositorio arma el Role/Operation a mano (ver
    // GrantedPermissionRepositoryImpl#entityToDomainConRole), no via este mismo mapper en cascada
    @Mapping(source = "role.idRol", target = "idRole")
    @Mapping(source = "role.name", target = "roleName")
    @Mapping(source = "operation.idOperation", target = "idOperation")
    @Mapping(source = "operation.tag", target = "operationTag")
    // `resource` y no el nombre del módulo (Spec 1.2): la matriz de permisos agrupa por la
    // identidad estable de la operación, no por dónde la empresa decidió ponerla en el menú -- que
    // ahora puede ser más de un lugar a la vez, o ninguno.
    @Mapping(source = "operation.resource", target = "resource")
    GrantedPermissionDTO domainToDto(GrantedPermission domain);

    // UPDATE
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "roleEntity", ignore = true)
    @Mapping(target = "operationEntity", source = "operation")
    void update(GrantedPermission domain, @MappingTarget GrantedPermissionEntity entity);
}
