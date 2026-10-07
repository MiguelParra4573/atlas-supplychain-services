package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.application.audit.contracts.CurrentActor;
import com.atlas.supplychain.auth.application.audit.dto.Actor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentActor implements CurrentActor {

    @Override
    public Actor actorActual() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails cud)) {
            return null;
        }

        var roles = cud.getRoleNames();
        String rolPrincipal = roles.isEmpty() ? null : roles.get(0);
        return new Actor(cud.getUsername(), cud.getIdCompany(), cud.getUsername(), rolPrincipal);
    }
}
