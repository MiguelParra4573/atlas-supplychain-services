package com.atlas.supplychain.auth.infrastructure.config.security.tenant;

import com.atlas.supplychain.auth.infrastructure.config.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Candado de integridad de Spec 1.19 SS4: no reemplaza el aislamiento real (bases físicamente
 * distintas), es defensa en profundidad contra un JWT emitido para otra empresa siendo usado
 * contra el stack exclusivo de un cliente. Property ausente (perfil sin
 * tenant.expected-id-company, caso de la base compartida) = filtro no aplica, comportamiento
 * actual sin cambios.
 */
@Component
public class TenantBoundaryFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccessDeniedHandler accessDeniedHandler;

    /**
     * Constructor explicito (no @RequiredArgsConstructor de Lombok): Lombok no copia
     * anotaciones de campo al parametro del constructor generado, asi que @Lazy en el campo
     * queda inerte -- tiene que ir en el parametro para romper el ciclo SecurityConfig <->
     * TenantBoundaryFilter <-> AccessDeniedHandler (el handler es un @Bean de SecurityConfig).
     */
    public TenantBoundaryFilter(JwtService jwtService, @Lazy AccessDeniedHandler accessDeniedHandler) {
        this.jwtService = jwtService;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Value("${tenant.expected-id-company:}")
    private String expectedIdCompany;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        if (expectedIdCompany == null || expectedIdCompany.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            claims = jwtService.validarToken(authHeader.substring(7));
        } catch (Exception ex) {
            // Token invalido: la autenticacion ya quedo sin establecer, anyRequest().authenticated()
            // lo resuelve como 401 mas adelante en la cadena -- no es responsabilidad de este filtro.
            filterChain.doFilter(request, response);
            return;
        }

        String idCompany = claims.get("idCompany", String.class);
        if (idCompany == null || !idCompany.equals(expectedIdCompany)) {
            accessDeniedHandler.handle(request, response, new TenantMismatchException(
                    "Acceso denegado: la empresa del token no corresponde a este stack."));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
