package com.atlas.supplychain.auth.application.maintenance.authorization;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.GrantedPermission;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.User;
import com.atlas.supplychain.auth.domain.maintenance.authorization.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class PermisosVigentesDelUsuarioImpl implements PermisosVigentesDelUsuario {

    private final UserRepository userRepository;

    @Override
    public Map<Module, List<GrantedPermission>> obtener(String idUser, String idCompany) {
        return usuarioDeLaEmpresa(idUser, idCompany)
                .map(User::permisosVigentesPorModulo)
                .orElseGet(Map::of);
    }

    @Override
    public Set<String> autoridades(String idUser, String idCompany) {
        return usuarioDeLaEmpresa(idUser, idCompany)
                .map(User::permisosEfectivos)
                .orElseGet(Set::of);
    }

    // findByIdUsuarioWithAuthorities ya trae user -> role -> grantedPermissions -> operation ->
    // modules con fetchJoin: única consulta, reutilizada por cualquier consumidor de este
    // colaborador (GrantedPermissionServiceImpl y MenuServiceImpl, Spec 1.18).
    private Optional<User> usuarioDeLaEmpresa(String idUser, String idCompany) {
        return userRepository.findByIdUsuarioWithAuthorities(idUser)
                .filter(user -> idCompany.equals(user.getIdCompany()));
    }
}
