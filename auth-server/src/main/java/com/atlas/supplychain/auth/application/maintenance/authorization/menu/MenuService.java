package com.atlas.supplychain.auth.application.maintenance.authorization.menu;

import com.atlas.supplychain.auth.domain.maintenance.authorization.menu.MenuModuloView;

import java.util.List;

public interface MenuService {

    /**
     * Árbol de módulos con sus operaciones concedidas y vigentes para el usuario, ya ordenado
     * (Module por name asc, Operation por tag asc dentro de cada uno) y sin módulos vacíos.
     * Lista vacía si el usuario no tiene ningún permiso vigente.
     */
    List<MenuModuloView> obtenerMenu(String idUser, String idCompany);
}
