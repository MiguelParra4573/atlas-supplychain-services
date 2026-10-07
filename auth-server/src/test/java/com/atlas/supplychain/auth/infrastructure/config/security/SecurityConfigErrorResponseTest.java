package com.atlas.supplychain.auth.infrastructure.config.security;

import tools.jackson.databind.ObjectMapper;
import com.atlas.supplychain.auth.infrastructure.common.exceptions.ErrorResponse;
import com.atlas.supplychain.auth.infrastructure.common.exceptions.ErrorResponseWriter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityConfig.restAuthenticationEntryPoint / restAccessDeniedHandler se emiten en la
 * cadena de filtros, antes del DispatcherServlet -- no pasan por @RestControllerAdvice.
 * No hay hoy ningún endpoint con @PreAuthorize que dispare un 403 real por HTTP, así que
 * se prueba el AccessDeniedHandler directo, igual que hace Spring internamente.
 */
class SecurityConfigErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorResponseWriter errorResponseWriter = new ErrorResponseWriter(objectMapper);
    private final SecurityConfig securityConfig = new SecurityConfig(null, null, null, errorResponseWriter, new CorsProperties());

    @Test
    void entryPointNoFiltraElMensajeDeLaAuthenticationException() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityConfig.restAuthenticationEntryPoint()
                .commence(null, response, new BadCredentialsException("Full authentication is required to access this resource"));

        assertThat(response.getStatus()).isEqualTo(401);
        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.message()).isEqualTo("No autorizado");
        assertThat(response.getContentAsString()).doesNotContain("Full authentication is required");
        assertThat(body.correlationId()).isNotBlank();
    }

    @Test
    void entryPointDevuelve423ConMensajeCentralizadoParaLockedException() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityConfig.restAuthenticationEntryPoint()
                .commence(null, response, new LockedException("detalle interno de bloqueo"));

        assertThat(response.getStatus()).isEqualTo(423);
        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.message()).isEqualTo(ErrorResponseWriter.LOCKED_MESSAGE);
        assertThat(response.getContentAsString()).doesNotContain("detalle interno de bloqueo");
    }

    @Test
    void accessDeniedHandlerNoFiltraElMensajeDeLaAccessDeniedException() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityConfig.restAccessDeniedHandler()
                .handle(null, response, new AccessDeniedException("Access is denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.message()).isEqualTo("Acceso denegado. No tienes permisos para realizar esta acción.");
        assertThat(response.getContentAsString()).doesNotContain("Access is denied");
        assertThat(body.correlationId()).isNotBlank();
    }
}
