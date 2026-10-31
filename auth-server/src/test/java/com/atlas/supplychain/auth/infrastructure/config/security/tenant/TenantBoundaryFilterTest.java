package com.atlas.supplychain.auth.infrastructure.config.security.tenant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.atlas.supplychain.auth.infrastructure.config.security.jwt.JwtService;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Spec 1.19 SS4: TenantBoundaryFilter delega el 403 al AccessDeniedHandler ya wireado en
 * SecurityConfig -- no lo escribe el mismo -- para no duplicar la logica de serializacion.
 */
class TenantBoundaryFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final AccessDeniedHandler accessDeniedHandler = mock(AccessDeniedHandler.class);

    private TenantBoundaryFilter filter(String expectedIdCompany) {
        return new TenantBoundaryFilter(jwtService, accessDeniedHandler, expectedIdCompany);
    }

    @Test
    void propertyAusenteEsNoOp() throws Exception {
        TenantBoundaryFilter filter = filter("");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(accessDeniedHandler, jwtService);
    }

    @Test
    void idCompanyDistintoDelegaAlAccessDeniedHandler() throws Exception {
        TenantBoundaryFilter filter = filter("99");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("idCompany", "01");

        when(request.getHeader("Authorization")).thenReturn("Bearer faketoken");
        when(jwtService.validarToken("faketoken")).thenReturn(Jwts.claims().add(claimsMap).build());

        filter.doFilter(request, response, filterChain);

        verify(accessDeniedHandler, times(1))
                .handle(eq(request), eq(response), any(TenantMismatchException.class));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void idCompanyCoincideDejaPasar() throws Exception {
        TenantBoundaryFilter filter = filter("99");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("idCompany", "99");

        when(request.getHeader("Authorization")).thenReturn("Bearer faketoken");
        when(jwtService.validarToken("faketoken")).thenReturn(Jwts.claims().add(claimsMap).build());

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(accessDeniedHandler);
    }

    @Test
    void tokenInvalidoDejaPasarSinBloquear() throws Exception {
        TenantBoundaryFilter filter = filter("99");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer tokeninvalido");
        when(jwtService.validarToken("tokeninvalido")).thenThrow(new RuntimeException("invalido"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(accessDeniedHandler);
    }
}
