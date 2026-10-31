package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.out.mappers;

import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.MenuModuloView;
import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.MenuOperacionView;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.MenuModuloDTO;
import com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization.MenuOperacionDTO;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuMapper {

    MenuModuloDTO domainToDto(MenuModuloView domain);

    MenuOperacionDTO domainToDto(MenuOperacionView domain);

    List<MenuModuloDTO> domainToDto(List<MenuModuloView> domain);
}
